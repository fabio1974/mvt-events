# 🚨 MUDANÇA NO FLUXO DE STATUS DAS ENTREGAS

## O que mudou?

Removemos o status **PICKED_UP** e unificamos com **IN_TRANSIT**.

### Antes (6 status):
```
PENDING → ACCEPTED → PICKED_UP → IN_TRANSIT → COMPLETED
                                ↓
                           CANCELLED
```

### Agora (5 status):
```
PENDING → ACCEPTED → IN_TRANSIT → COMPLETED
                        ↓
                   CANCELLED
```

## Por quê?

- **PICKED_UP e IN_TRANSIT eram redundantes**: quando o motoboy coleta, ele já está em trânsito automaticamente
- **Para viagem de passageiro**: o botão "coletar" = início da viagem
- **Simplifica UX**: um botão a menos, fluxo mais direto

## O que o mobile precisa ajustar?

### ✅ CONTINUA FUNCIONANDO (compatibilidade mantida):
- Endpoint `PATCH /api/deliveries/{id}/pickup` - **usar este!**
- Endpoint `PATCH /api/deliveries/{id}/transit` - **deprecated mas funciona** (redireciona para /pickup)

### ⚠️ COMPORTAMENTO MUDOU:
Quando chamar `PATCH /api/deliveries/{id}/pickup`:
- **Antes**: status mudava para `PICKED_UP`
- **Agora**: status muda direto para `IN_TRANSIT`

### 📱 AJUSTES NECESSÁRIOS NO MOBILE:

1. **Remover botão "Iniciar Transporte"** (se existir separado)
   - Agora o botão "Coletar" já faz tudo

2. **Renomear botão** (opcional mas recomendado):
   - De: "Confirmar Coleta"
   - Para: "Coletar e Iniciar Viagem" ou apenas "Iniciar Viagem"

3. **Atualizar validações de status**:
   ```typescript
   // Remover referências a PICKED_UP
   if (status === 'PICKED_UP') { ... } // ❌ REMOVER
   
   // Ajustar fluxo
   if (status === 'ACCEPTED') {
     showButton('Iniciar Viagem'); // vai para IN_TRANSIT
   }
   if (status === 'IN_TRANSIT') {
     showButton('Finalizar Entrega'); // vai para COMPLETED
   }
   ```

4. **Lista de status válidos**:
   ```typescript
   type DeliveryStatus = 
     | 'PENDING'
     | 'ACCEPTED'
     | 'IN_TRANSIT'  // ← engloba coleta + transporte
     | 'COMPLETED'
     | 'CANCELLED';
   ```

### 🔄 ENDPOINTS ATUALIZADOS:

#### ✅ Endpoint principal (usar este):
```http
PATCH /api/deliveries/{id}/pickup
```
- **Input**: Apenas autenticação JWT
- **Output**: Delivery com status `IN_TRANSIT`
- **Ação**: Confirma coleta E inicia transporte (tudo junto)

#### ⚠️ Endpoint deprecated (mantido por compatibilidade):
```http
PATCH /api/deliveries/{id}/transit
```
- Ainda funciona mas **redireciona** internamente para `/pickup`
- Recomendo **remover** do mobile em algum momento

### 📊 FLUXO COMPLETO ATUALIZADO:

```
1. Cliente cria entrega
   POST /api/deliveries
   → Status: PENDING

2. Motoboy aceita
   PATCH /api/deliveries/{id}/accept
   → Status: ACCEPTED
   → Botão no mobile: "Iniciar Viagem"

3. Motoboy coleta e inicia
   PATCH /api/deliveries/{id}/pickup
   → Status: IN_TRANSIT
   → Botão no mobile: "Finalizar Entrega"

4. Motoboy finaliza
   PATCH /api/deliveries/{id}/complete
   → Status: COMPLETED
```

### 🗓️ PRAZO:

- ✅ Backend já atualizado (hoje, 02/02/2026)
- ✅ Compatibilidade mantida (não quebra app antigo)
- ⏰ Ajuste no mobile: próxima sprint

### 🔍 DETALHES TÉCNICOS:

#### Backend Changes:
- ✅ Enum `DeliveryStatus` removeu `PICKED_UP`
- ✅ Método `confirmPickup()` agora seta `status = IN_TRANSIT` direto
- ✅ Método `startTransit()` marcado como `@Deprecated`
- ✅ Migration V47 atualiza registros antigos
- ✅ Constraint CHECK do PostgreSQL atualizada

#### Timestamps mantidos:
- `picked_up_at` - Quando coletou (continua sendo setado)
- `in_transit_at` - Quando iniciou transporte (continua sendo setado)
- Ambos são setados **simultaneamente** agora

#### Métricas não afetadas:
- Tempo entre aceitar e coletar
- Tempo de coleta até entrega
- SLA de coleta
- Todos os relatórios continuam funcionando

### ❓ DÚVIDAS?

Qualquer coisa, só chamar! 🚀

---

**Última atualização:** 02/02/2026  
**Versão Backend:** v1.47.0  
**Status:** ✅ Em produção
