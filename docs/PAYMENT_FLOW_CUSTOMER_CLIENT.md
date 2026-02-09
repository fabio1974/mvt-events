# Fluxo de Pagamentos — CUSTOMER vs CLIENT

> Documento que descreve **quando** e **como** os pagamentos são criados no Pagar.me para cada tipo de usuário.

---

## Visão Geral

| Quem cria a delivery | Método | Momento do pagamento | Split |
|---|---|---|---|
| **CLIENT** (estabelecimento) | Cartão de Crédito | No **aceite** do courier | 87% courier · 5% organizer · 8% plataforma |
| **CLIENT** (estabelecimento) | PIX | **Pagamento consolidado** (criado pelo ADMIN no FE) | 87% courier · 5% organizer · 8% plataforma |
| **CUSTOMER** (app mobile) | PIX | No **aceite** do courier | 87% courier · 13% plataforma |
| **CUSTOMER** (app mobile) | Cartão de Crédito | Ao entrar em **trânsito** (confirmPickup) | 87% courier · 13% plataforma |
| **CUSTOMER** RIDE | PIX | No **aceite** do courier | 87% courier · 13% plataforma |
| **CUSTOMER** RIDE | Cartão de Crédito | Ao entrar em **trânsito** (confirmPickup) | 87% courier · 13% plataforma |

---

## CLIENT (Estabelecimento)

O CLIENT é um estabelecimento comercial que possui contrato ativo com uma **Organização**.  
Quando o courier aceita a delivery, o sistema encontra a organização em comum entre courier e client e define o **organizer** (dono da organização).

### Cartão de Crédito — no aceite

```
PENDING ──► courier aceita ──► ACCEPTED
                 │
                 ├─ Busca organização comum (courier ↔ client)
                 ├─ Define organizer (owner da organização)
                 ├─ Verifica preferência: CREDIT_CARD?
                 ├─ Busca cartão padrão do CLIENT
                 ├─ Cria order no Pagar.me (createOrderWithCreditCardSplit)
                 │    └─ Split: 87% courier | 5% organizer | 8% plataforma
                 └─ Salva Payment no banco (status: PENDING)
```

- **Momento:** `assignToCourier` (método `createAutomaticCreditCardPayment`)
- **Método Pagar.me:** `createOrderWithCreditCardSplit`
- **Se falhar:** O aceite **NÃO é revertido** — a delivery continua ACCEPTED (log de warning)

### PIX — pagamento consolidado (via ADMIN)

O PIX do CLIENT não é criado automaticamente no aceite da delivery.  
O fluxo funciona assim:

1. O CLIENT cria deliveries normalmente
2. O **ADMIN** acessa o frontend e gera um **pagamento consolidado** (agrupando múltiplas deliveries)
3. O pagamento consolidado é enviado ao Pagar.me via PIX com split
4. O CLIENT recebe o QR Code PIX para pagar

```
Deliveries do CLIENT ──► ADMIN gera pagamento consolidado no FE
                              │
                              ├─ Agrupa deliveries pendentes de pagamento
                              ├─ Cria order PIX no Pagar.me (createOrderWithSplit)
                              │    └─ Split: 87% courier | 5% organizer | 8% plataforma
                              └─ Cliente recebe QR Code PIX
```

- **Momento:** Iniciado pelo ADMIN no frontend (não automático)
- **Método Pagar.me:** `createOrderWithSplit`

---

## CUSTOMER (App Mobile)

O CUSTOMER é um usuário do app mobile que cria entregas **sem vínculo com organização**.  
Não existe organizer — o split é dividido apenas entre **courier** e **plataforma**.

### PIX — no aceite (DELIVERY e RIDE)

```
PENDING ──► courier aceita ──► ACCEPTED
                 │
                 ├─ Sem organização (organizer = null)
                 ├─ Verifica preferência: PIX?
                 ├─ Cria order PIX no Pagar.me (createOrderWithSplit)
                 │    └─ Split: 87% courier | 13% plataforma
                 ├─ Salva Payment no banco (status: PENDING)
                 └─ Cliente recebe QR Code PIX para pagar
```

- **Aplica-se a:** DELIVERY e RIDE
- **Momento:** `assignToCourier` (método `createPixPaymentForCustomer`)
- **Método Pagar.me:** `createOrderWithSplit`
- **Se falhar:** O aceite **É REVERTIDO** — delivery volta para PENDING (courier desvinculado)

### Cartão de Crédito — ao entrar em trânsito (DELIVERY e RIDE)

```
ACCEPTED ──► courier confirma coleta ──► IN_TRANSIT
                      │
                      ├─ Verifica preferência: CREDIT_CARD?
                      ├─ Busca cartão padrão do CUSTOMER
                      ├─ Cria order no Pagar.me (createOrderWithCreditCardSplit)
                      │    └─ Split: 87% courier | 13% plataforma
                      ├─ Salva Payment no banco (status: PENDING)
                      └─ Cobrança imediata no cartão (auth_and_capture)
```

- **Aplica-se a:** DELIVERY e RIDE
- **Momento:** `confirmPickup` (método `createCreditCardPaymentForCustomer`)
- **Método Pagar.me:** `createOrderWithCreditCardSplit`
- **Se falhar:** O trânsito **É REVERTIDO** — delivery volta para ACCEPTED

---

## Tabela de Split

### Com Organizer (CLIENT)

| Destinatário | Percentual | Observação |
|---|---|---|
| Courier | 87% | Recebe o valor principal |
| Organizer | 5% | Dono da organização |
| Plataforma | 8% | Remainder automático (conta master Pagar.me) |

### Sem Organizer (CUSTOMER)

| Destinatário | Percentual | Observação |
|---|---|---|
| Courier | 87% | Recebe o valor principal |
| Plataforma | 13% | Absorve os 5% do organizer ausente |

---

## Por que o cartão do CUSTOMER é cobrado no trânsito?

O pagamento por **PIX** é criado no aceite porque gera um QR Code que o cliente precisa pagar — quanto antes gerar, mais tempo o cliente tem para efetuar o pagamento.

Já o **cartão de crédito** é uma cobrança instantânea e automática. Cobrar no momento do trânsito garante que:

1. O courier já confirmou que está com o item em mãos (coleta feita)
2. Reduz o risco de cobrar o cliente e o courier não buscar o item
3. Caso a delivery seja cancelada entre ACCEPTED e IN_TRANSIT, não há cobrança indevida

---

## Comportamento em caso de falha

| Cenário | O que acontece |
|---|---|
| CLIENT + Cartão falha no aceite | Delivery continua ACCEPTED (warning no log) |
| CUSTOMER + PIX falha no aceite | Delivery **volta para PENDING** (courier desvinculado) |
| CUSTOMER + Cartão falha no trânsito | Delivery **volta para ACCEPTED** (trânsito revertido) |

> **Nota:** Todos os pagamentos são criados com `status: PENDING`. A confirmação final vem via **webhook** do Pagar.me (`order.paid`), que atualiza `paymentCompleted` e `paymentCaptured` na delivery.

---

## Fluxo Completo — Ciclo de Vida

```
┌─────────────────────────────────────────────────────────────────┐
│                        DELIVERY LIFECYCLE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PENDING ───────────► ACCEPTED ───────────► IN_TRANSIT ──► ...  │
│       │                    │                     │               │
│       │  CLIENT:           │                     │               │
│       │  💳 Cartão (aceite)│                     │               │
│       │                    │                     │               │
│       │  CUSTOMER:         │                     │               │
│       │  💳 PIX (aceite)   │    CUSTOMER:        │               │
│       │                    │    💳 Cartão         │               │
│       │                    │    (trânsito)        │               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```
