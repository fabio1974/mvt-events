# 📊 Apresentação: Sistema de Grupos e Algoritmo Inteligente de Notificações

**Zapi10 - Plataforma de Entregas por Aplicativo**  
**Data:** Novembro 2024  
**Versão:** 1.0  
**Público:** Gerentes e Gestores

---

## 📑 Índice

1. [Como Funciona o Sistema de Grupos](#1-como-funciona-o-sistema-de-grupos)
2. [Algoritmo Inteligente de Notificação em 3 Níveis](#2-algoritmo-inteligente-de-notificação-em-3-níveis)
3. [Divisão de Valores e Comissões](#3-divisão-de-valores-e-comissões)
4. [Exemplos Práticos e Cenários Reais](#4-exemplos-práticos-e-cenários-reais)

---

# 1. Como Funciona o Sistema de Grupos

## 1.1. Visão Geral

O **Zapi10** está sendo desenvolvido para conectar **clientes** que precisam fazer entregas com **motoboys** disponíveis, através de **grupos organizados** gerenciados por empresas de logística.

### 🔑 Atores do Sistema

- **Cliente**: Pessoa ou empresa que solicita entregas pelo aplicativo
- **Motoboy**: Profissional que realiza as entregas
- **Grupo/Organização**: Empresa de logística que gerencia motoboys
- **Gerente**: Responsável pela administração do grupo

---

## 1.2. Os Dois Tipos de Vínculos

### 📋 Tipo 1: Vínculos de Clientes com Grupos

Um **cliente** pode estar conectado a **vários grupos de logística** ao mesmo tempo. Isso oferece flexibilidade e garante que sempre terá motoboys disponíveis.

#### Como funciona:

| Conceito | Explicação |
|----------|------------|
| **Múltiplos Grupos** | Um cliente pode ter contrato com 3, 4, 5 ou mais grupos |
| **Grupo Principal** | Apenas **1 grupo** é marcado como principal/titular |
| **Grupos Secundários** | Outros grupos são considerados secundários |
| **Situação do Contrato** | Pode estar Ativo, Suspenso ou Cancelado |
| **Período** | Contratos têm data de início e fim (opcional) |

#### Exemplo na Prática:

**Cliente:** Restaurante Boa Pizza
- **Grupo Principal:** LogiFast Entregas ⭐ (contrato desde janeiro)
- **Grupo Secundário 1:** Expresso Rápido (contrato de junho a dezembro)
- **Grupo Secundário 2:** RapidLog (contrato suspenso temporariamente)

**O que isso significa?**
- Quando o Restaurante Boa Pizza pede uma entrega, **LogiFast** é avisada primeiro
- Se ninguém da LogiFast aceitar, **Expresso Rápido** é avisado depois
- RapidLog não recebe avisos enquanto estiver suspenso

#### Por que isso é importante?

✅ **Para o Cliente:**
- Garantia de sempre ter motoboys disponíveis
- Flexibilidade para trabalhar com múltiplos fornecedores
- Continuidade do serviço mesmo se um grupo estiver sem motoboys

✅ **Para o Grupo:**
- Fidelização do cliente através do contrato principal
- Oportunidade de atender clientes de outros grupos
- Controle sobre ativação/suspensão de contratos

---

### 👷 Tipo 2: Vínculos de Motoboys com Grupos

Um **motoboy** pode trabalhar para **vários grupos** ao mesmo tempo. Isso é comum no mercado de entregas.

#### Como funciona:

| Conceito | Explicação |
|----------|------------|
| **Múltiplos Grupos** | Um motoboy pode estar cadastrado em 2, 3 ou mais grupos |
| **Ativo/Inativo** | Em cada grupo, o motoboy pode estar ativo ou inativo |
| **Histórico** | O sistema mantém registro de quando o motoboy entrou em cada grupo |
| **Flexibilidade** | Motoboy decide em quais grupos quer trabalhar simultaneamente |

#### Exemplo na Prática:

**Motoboy:** João Silva
- **LogiFast Entregas:** Ativo ✅ (trabalha desde janeiro)
- **Expresso Rápido:** Ativo ✅ (trabalha desde março)
- **RapidLog:** Inativo ❌ (trabalhou de novembro/2023 a fevereiro/2024)

**O que isso significa?**
- João pode receber entregas de clientes da LogiFast **e** da Expresso Rápido
- Ele não recebe mais entregas da RapidLog
- Se um cliente estiver conectado aos dois grupos onde João trabalha, ele pode receber a notificação duas vezes

#### Por que isso é importante?

✅ **Para o Motoboy:**
- Mais oportunidades de trabalho
- Diversificação de fonte de renda
- Flexibilidade para escolher com quais grupos trabalhar

✅ **Para o Grupo:**
- Acesso a profissionais qualificados
- Não precisa ter motoboys exclusivos
- Pode ativar/desativar motoboys conforme demanda

---

## 1.3. Como as Conexões Funcionam na Prática

### 🔗 Cenário Completo: Do Cliente ao Motoboy

Vamos ver um exemplo real de como tudo se conecta:

```
┌──────────────────────────────────────────────┐
│     CLIENTE: Restaurante Boa Pizza           │
└──────────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │LogiFast │  │Expresso │  │ RapidLog│
   │  (⭐)   │  │  Rápido │  │(Suspenso)│
   └─────────┘  └─────────┘  └─────────┘
        │            │             │
    ┌───┼───┐        │             │
    ▼   ▼   ▼        ▼             ▼
   João Maria Pedro  Ana          Carlos
  (Ativo)(Ativo)(Inativo)      (Ativo)
```

**Análise:**
1. **LogiFast** é o grupo principal do cliente
2. LogiFast tem 3 motoboys cadastrados, mas só 2 estão ativos
3. **Expresso Rápido** é grupo secundário com motoboys próprios
4. **RapidLog** está suspenso - não participa das entregas

**Quando o Restaurante solicitar uma entrega:**
1. João e Maria (LogiFast) serão avisados primeiro
2. Pedro não receberá aviso (está inativo)
3. Ana (Expresso Rápido) só será avisada se João e Maria não aceitarem
4. Carlos (RapidLog) não será avisado (grupo suspenso)

---

## 1.4. Perguntas Frequentes dos Gerentes

### ❓ Um cliente pode trocar o grupo principal?

**Sim!** A qualquer momento, o gerente pode alterar qual grupo é o principal do cliente. Isso permite:
- Renegociação de contratos
- Premiação por desempenho (grupo que atende melhor vira principal)
- Ajustes estratégicos conforme a região

### ❓ O que acontece se o grupo principal não tiver motoboys disponíveis?

O sistema é inteligente e escalona automaticamente para os grupos secundários (explicado no próximo capítulo). **Nenhuma entrega fica sem atendimento.**

### ❓ Posso suspender um cliente temporariamente?

**Sim!** Você pode suspender o contrato sem excluí-lo. Útil para:
- Inadimplência temporária
- Manutenção de cadastro
- Períodos de férias acordados

### ❓ Um motoboy pode trabalhar para grupos concorrentes?

**Sim!** O Zapi10 não impõe exclusividade. Isso beneficia:
- **Motoboys:** Mais oportunidades de ganho
- **Grupos:** Acesso a profissionais sem vínculo exclusivo
- **Clientes:** Mais motoboys disponíveis

---

# 2. Algoritmo Inteligente de Notificação em 3 Níveis

## 2.1. O Problema que o Sistema Resolve

**Cenário:** Um cliente solicita uma entrega pelo aplicativo Zapi10.

**Desafio:** Encontrar rapidamente um motoboy que:
1. Esteja **disponível** (não ocupado em outra entrega)
2. Esteja **próximo** do local de coleta
3. Tenha **vínculo comercial** com o cliente (quando possível)

**Solução:** Sistema inteligente em **3 níveis** que prioriza relacionamentos comerciais e proximidade.

---

## 2.2. Como Funciona o Sistema de Níveis

### 📊 Visão Geral

O aplicativo **não avisa todos os motoboys de uma vez**. Ele segue uma estratégia inteligente:

```
🥇 NÍVEL 1: Motoboys do Grupo Principal (imediato)
         ↓ [Aguarda 2 minutos]
         
🥈 NÍVEL 2: Motoboys de Outros Grupos do Cliente (+2 min)
         ↓ [Aguarda mais 2 minutos]
         
🥉 NÍVEL 3: Todos os Motoboys Próximos (+4 min)
```

### 🎯 Objetivos do Sistema

✅ **Priorizar parceiros comerciais** - Quem tem contrato atende primeiro  
✅ **Garantir rapidez** - 2 minutos entre cada tentativa  
✅ **Não perder entregas** - Se ninguém aceitar, expande para todos  
✅ **Respeitar proximidade** - Só avisa quem está perto (5-10km)

---

## 2.3. Nível 1 - Grupo Principal (Prioridade Máxima)

### 📌 Como Funciona

Quando um cliente solicita uma entrega, o sistema:

1. Identifica qual é o **grupo principal** daquele cliente
2. Busca **todos os motoboys ativos** daquele grupo
3. Filtra apenas os que estão **disponíveis** (não em entrega)
4. Filtra apenas os que estão **próximos** (até 5km do local de coleta)
5. **Envia notificação** para todos esses motoboys simultaneamente

### 📏 Regras de Distância

- **Primeiro:** Busca motoboys em até **5 km**
- **Se nenhum encontrado:** Expande para **10 km**
- **Se ainda nenhum:** Passa para o Nível 2

### ⏱️ Tempo de Espera

O sistema aguarda **2 minutos** para alguém aceitar antes de passar para o Nível 2.

### 💡 Exemplo Prático

```
Cliente: Restaurante Boa Pizza
Grupo Principal: LogiFast Entregas
Local de Coleta: Av. Paulista, 1000

Motoboys da LogiFast:
├── João Silva - 2.3 km - Disponível ✅ → NOTIFICADO
├── Maria Santos - 4.1 km - Disponível ✅ → NOTIFICADO
├── Pedro Costa - 3.5 km - Em entrega ❌ → NÃO notificado
└── Ana Lima - 12 km - Disponível ❌ → Muito longe

RESULTADO: 2 motoboys notificados no Nível 1
```

### 🎁 Vantagens para o Grupo Principal

- **Primeira chance** de aceitar todas as entregas
- **Fidelização** do cliente através do atendimento prioritário
- **Previsibilidade** de demanda
- **Valorização** do contrato principal

---

## 2.4. Nível 2 - Grupos Secundários (Segunda Chance)

### 📌 Como Funciona

Se **nenhum motoboy do Nível 1 aceitou** após 2 minutos:

1. Sistema busca **todos os outros grupos** onde o cliente tem contrato ativo
2. Pega os motoboys ativos de **todos esses grupos**
3. Filtra disponíveis e próximos (mesma regra: 5km → 10km)
4. **Envia notificação** para todos simultaneamente

### 🔄 Por que esperar 2 minutos?

- Dar **tempo justo** para o grupo principal responder
- Evitar **sobrecarga** de notificações desnecessárias
- Manter **hierarquia comercial** dos contratos
- **Economizar custos** de notificações

### 💡 Exemplo Prático

```
Cliente: Restaurante Boa Pizza (após 2 min sem aceite)
Grupos Secundários: Expresso Rápido, MegaLog

Motoboys disponíveis próximos:
├── Carlos (Expresso Rápido) - 3.8 km ✅ → NOTIFICADO
├── Bruno (Expresso Rápido) - 7.2 km ✅ → NOTIFICADO
├── Rafael (MegaLog) - 6.5 km ✅ → NOTIFICADO
└── Lucas (Expresso Rápido) - 15 km ❌ → Muito longe

RESULTADO: 3 motoboys notificados no Nível 2
```

### 🎁 Vantagens dos Grupos Secundários

- **Oportunidade de atender** mesmo não sendo principal
- **Demonstrar eficiência** pode levar a virar grupo principal
- **Aumentar receita** com entregas de clientes compartilhados
- **Aproveitar ociosidade** dos motoboys

---

## 2.5. Nível 3 - Todos Próximos (Rede Aberta)

### 📌 Como Funciona

Se **ainda ninguém aceitou** após 4 minutos totais:

1. Sistema **abandona** a restrição de grupos
2. Busca **QUALQUER motoboy disponível** próximo
3. **Não importa** se o grupo tem contrato com o cliente
4. Objetivo: **Garantir que a entrega aconteça**

### 🌐 Quando isso acontece?

- Horários de pico (poucos motoboys disponíveis)
- Regiões com baixa cobertura
- Clientes com poucos contratos
- Grupos com poucos motoboys

### 💡 Exemplo Prático

```
Cliente: Restaurante Boa Pizza (após 4 min sem aceite)
Situação: Horário de pico, todos ocupados

Motoboys próximos (QUALQUER grupo):
├── Roberto (TurboLog) - 6.5 km ✅ → NOTIFICADO*
├── Fernanda (RapidEntregas) - 8.3 km ✅ → NOTIFICADA*
└── Paulo (MegaLog) - 9.1 km ✅ → NOTIFICADO*

* Esses grupos NÃO têm contrato com o cliente

RESULTADO: 3 motoboys notificados no Nível 3
```

### 🎁 Vantagens do Nível 3

✅ **Para o Cliente:**
- Garantia de que a entrega será atendida
- Não fica "na mão" por falta de motoboys

✅ **Para Motoboys Sem Vínculo:**
- Oportunidade de ganhar mesmo sem contrato prévio
- Demonstrar serviço pode gerar contrato futuro

✅ **Para Grupos Sem Contrato:**
- Conquistar novos clientes organicamente
- Mostrar capacidade de atendimento

---

## 2.6. Resumo Visual dos 3 Níveis

### 📊 Tabela Comparativa

| Aspecto | Nível 1 | Nível 2 | Nível 3 |
|---------|---------|---------|---------|
| **Quando** | Imediato | Após 2 min | Após 4 min |
| **Quem** | Grupo principal | Grupos secundários | Todos próximos |
| **Critério** | Contrato principal | Contratos ativos | Sem restrição |
| **Distância** | 5 km → 10 km | 5 km → 10 km | 5 km → 10 km |
| **Objetivo** | Priorizar parceiro | Usar rede do cliente | Garantir entrega |
| **Vantagem** | Fidelização | Oportunidade | Cobertura total |

### ⏰ Linha do Tempo

```
00:00 min → Cliente solicita entrega
00:00 min → 🥇 NÍVEL 1 ativado (Grupo Principal)
            ↓
02:00 min → Ninguém aceitou?
02:00 min → 🥈 NÍVEL 2 ativado (Grupos Secundários)
            ↓
04:00 min → Ainda sem aceite?
04:00 min → 🥉 NÍVEL 3 ativado (Todos Próximos)
            ↓
04:30 min → Paulo aceita! (Grupo sem contrato prévio)
```

---

# 3. Divisão de Valores e Comissões

## 3.1. Modelo de Negócio do Zapi10

### 💰 Divisão de Valores

Quando uma entrega é concluída, o valor é dividido da seguinte forma:

```
┌─────────────────────────────────────┐
│   VALOR TOTAL DA ENTREGA: R$ 20,00 │
└─────────────────────────────────────┘
              │
              ├─────────────────────────┐
              ▼                         ▼
    ┌──────────────────┐      ┌──────────────────┐
    │  MOTOBOY: 85%    │      │   GRUPO: 15%     │
    │   R$ 17,00       │      │    R$ 3,00       │
    └──────────────────┘      └──────────────────┘
```

### 📊 Tabela de Exemplos

| Valor da Entrega | Motoboy (85%) | Grupo (15%) |
|------------------|---------------|-------------|
| R$ 10,00 | R$ 8,50 | R$ 1,50 |
| R$ 15,00 | R$ 12,75 | R$ 2,25 |
| R$ 20,00 | R$ 17,00 | R$ 3,00 |
| R$ 25,00 | R$ 21,25 | R$ 3,75 |
| R$ 30,00 | R$ 25,50 | R$ 4,50 |
| R$ 50,00 | R$ 42,50 | R$ 7,50 |
| R$ 100,00 | R$ 85,00 | R$ 15,00 |

---

## 3.2. Impacto do Sistema de Níveis nas Comissões

### 💡 Cenário 1: Aceite no Nível 1 (Grupo Principal)

```
Cliente: Restaurante Boa Pizza
Grupo Principal: LogiFast
Entrega: R$ 25,00
Aceite: João (motoboy da LogiFast)

Divisão:
├── João (motoboy): R$ 21,25 (85%)
└── LogiFast (grupo): R$ 3,75 (15%) ✅ Grupo principal lucra
```

### 💡 Cenário 2: Aceite no Nível 2 (Grupo Secundário)

```
Cliente: Restaurante Boa Pizza
Grupo que aceitou: Expresso Rápido (secundário)
Entrega: R$ 25,00
Aceite: Carlos (motoboy da Expresso Rápido)

Divisão:
├── Carlos (motoboy): R$ 21,25 (85%)
└── Expresso Rápido (grupo): R$ 3,75 (15%) ✅ Grupo secundário lucra
```

### 💡 Cenário 3: Aceite no Nível 3 (Sem Contrato)

```
Cliente: Restaurante Boa Pizza
Grupo que aceitou: TurboLog (SEM contrato com o cliente)
Entrega: R$ 25,00
Aceite: Roberto (motoboy da TurboLog)

Divisão:
├── Roberto (motoboy): R$ 21,25 (85%)
└── TurboLog (grupo): R$ 3,75 (15%) ✅ Grupo sem contrato lucra
```

---

## 3.3. Análise Estratégica para Gerentes

### 🎯 Por que ser Grupo Principal é importante?

**Vantagens Financeiras:**
- ✅ **Primeira chance** em todas as entregas do cliente
- ✅ **Maior volume** de entregas aceitas
- ✅ **Previsibilidade** de receita mensal
- ✅ **ROI do relacionamento** comercial

**Exemplo Numérico:**
```
Cliente faz 100 entregas/mês de R$ 20 cada

Grupo Principal (aceita 70% no Nível 1):
├── 70 entregas × R$ 3,00 = R$ 210,00/mês
└── Taxa de sucesso: 70%

Grupo Secundário (aceita 20% no Nível 2):
├── 20 entregas × R$ 3,00 = R$ 60,00/mês
└── Taxa de sucesso: 20%

Sem Contrato (aceita 10% no Nível 3):
├── 10 entregas × R$ 3,00 = R$ 30,00/mês
└── Taxa de sucesso: 10%
```

### 📈 Como Aumentar Receita do Grupo?

**1. Ser Grupo Principal de Mais Clientes**
- Negociar virar principal de clientes secundários
- Oferecer SLA melhor que concorrentes

**2. Manter Motoboys Sempre Disponíveis**
- Mais motoboys online = mais aceites no Nível 1
- Incentivos para motoboys ficarem disponíveis em horários de pico

**3. Proximidade Geográfica**
- Recrutar motoboys em regiões estratégicas
- Posicionamento inteligente da frota

**4. Velocidade de Resposta**
- Treinar motoboys para aceitarem rápido
- 2 minutos é pouco tempo - quem responde primeiro ganha

---

# 4. Exemplos Práticos e Cenários Reais

## 4.1. Caso Completo - Dia Típico de Entregas

```
┌─────────────────────────────────────────┐
│  CLIENTE CRIA NOVA ENTREGA (PENDING)    │
└─────────────────────────────────────────┘
                    ↓
        ┌───────────────────────┐
        │   INICIAR NÍVEL 1     │
        │  Organização Titular  │
        └───────────────────────┘
                    ↓
        ┌───────────────────────┐
        │ Buscar contrato       │
        │ is_primary = true     │
        └───────────────────────┘
                    ↓
            ┌───────┴────────┐
            │   Encontrou?   │
            └───────┬────────┘
        NÃO ←───────┤        ├───────→ SIM
            ↓               ↓
    ┌───────────┐   ┌──────────────────┐
    │ Ir para   │   │ Buscar motoboys  │
    │ Nível 2   │   │ ativos da org    │
    └───────────┘   └──────────────────┘
                            ↓
                    ┌──────────────────┐
                    │ Filtrar próximos │
                    │    (5km/10km)    │
                    └──────────────────┘
                            ↓
                    ┌──────────────────┐
                    │ Enviar notifs    │
                    └──────────────────┘
                            ↓
                    ┌──────────────────┐
                    │ Aguardar 2 min   │
                    └──────────────────┘
                            ↓
                    ┌──────────────────┐
                    │ Status = PENDING?│
                    └──────┬───────────┘
                    SIM ←──┤      ├──→ NÃO
                        ↓         ↓
            ┌───────────────┐ ┌──────────┐
            │   NÍVEL 2     │ │   FIM    │
            │Outras Orgs    │ │(Aceita)  │
            └───────────────┘ └──────────┘
                    ↓
        ┌──────────────────────┐
        │ Buscar contratos     │
        │ secundários (N orgs) │
        └──────────────────────┘
                    ↓
        ┌──────────────────────┐
        │ Agregar motoboys     │
        │ de todas as orgs     │
        └──────────────────────┘
                    ↓
        ┌──────────────────────┐
        │ Filtrar próximos     │
        │    (5km/10km)        │
        └──────────────────────┘
                    ↓
        ┌──────────────────────┐
        │ Enviar notifs        │
        └──────────────────────┘
                    ↓
        ┌──────────────────────┐
        │ Aguardar 2 min       │
        └──────────────────────┘
                    ↓
        ┌──────────────────────┐
        │ Status = PENDING?    │
        └──────┬───────────────┘
        SIM ←──┤      ├──→ NÃO
            ↓         ↓
┌───────────────┐ ┌──────────┐
│   NÍVEL 3     │ │   FIM    │
│Todos Próximos │ │(Aceita)  │
└───────────────┘ └──────────┘
        ↓
┌──────────────────────┐
│ Buscar TODOS         │
│ motoboys disponíveis │
│ (sem filtro de org)  │
└──────────────────────┘
        ↓
┌──────────────────────┐
│ Filtrar próximos     │
│    (5km/10km)        │
└──────────────────────┘
        ↓
┌──────────────────────┐
│ Enviar notifs        │
└──────────────────────┘
        ↓
┌──────────────────────┐
│        FIM           │
└──────────────────────┘
```

---

## 3.2. Diagrama de Entidades e Relacionamentos

```
┌─────────────────────────────────────────────────────────────┐
│                      MODELO DE DADOS                         │
└─────────────────────────────────────────────────────────────┘

        ┌──────────────┐
        │     USER     │
        │──────────────│
        │ id (UUID)    │
        │ full_name    │
        │ email        │
        │ role         │ ← ADMIN, CLIENT, COURIER, ORGANIZER
        └──────────────┘
           │         │
           │         │
┌──────────┘         └──────────┐
│ (COURIER)                     │ (CLIENT)
│                               │
│ 1:N                           │ 1:N
│                               │
▼                               ▼
┌────────────────────┐    ┌────────────────────┐
│EmploymentContract  │    │  ClientContract    │
│────────────────────│    │────────────────────│
│ id                 │    │ id                 │
│ courier_id (FK)    │    │ client_id (FK)     │
│ organization_id(FK)│    │ organization_id(FK)│
│ is_active          │    │ is_primary         │
│ linked_at          │    │ status             │
└────────────────────┘    │ start_date         │
        │                 │ end_date           │
        │                 └────────────────────┘
        │                         │
        │ N:1                     │ N:1
        │                         │
        │    ┌────────────────────┘
        │    │
        ▼    ▼
┌──────────────────┐
│  ORGANIZATION    │
│──────────────────│
│ id (BIGSERIAL)   │
│ name             │
│ cnpj             │
│ owner_id (FK)    │ ← User (role: ORGANIZER)
│ status           │
└──────────────────┘
        │ 1:1
        │
        ▼
┌──────────────────┐
│  CourierProfile  │
│──────────────────│
│ user_id (FK)     │
│ status           │ ← AVAILABLE, BUSY, OFFLINE
│ current_lat      │
│ current_lon      │
│ rating           │
└──────────────────┘
```

---

## 3.3. Linha do Tempo do Algoritmo

```
TEMPO     AÇÃO                           STATUS          MOTOBOYS NOTIFICADOS
──────────────────────────────────────────────────────────────────────────────
00:00     Cliente cria entrega          PENDING         -
00:00     NÍVEL 1 inicia                PENDING         Maria, Pedro (LogiFast)
          
          [Aguardando aceite...]
          
02:00     NÍVEL 2 inicia                PENDING         Ana, Bruno (ExpressLog)
                                                         + Lucas (RapidDelivery)
          
          [Aguardando aceite...]
          
04:00     NÍVEL 3 inicia                PENDING         Roberto (MegaLog)
                                                         + Fernanda (TurboEntregas)
                                                         + Paulo (SuperLog)
          
04:15     Motoboy aceita entrega        ACCEPTED        Paulo (SuperLog)
──────────────────────────────────────────────────────────────────────────────
```

---

## 3.4. Matriz de Decisão

| Nível | Critério de Organização | Raio Inicial | Raio Estendido | Tempo de Espera |
|-------|-------------------------|--------------|----------------|-----------------|
| **1** | Apenas organização titular (is_primary=true) | 5km | 10km | 0 min |
| **2** | Organizações secundárias (is_primary=false, status=ACTIVE) | 5km | 10km | 2 min |
| **3** | TODAS as organizações (sem restrição) | 5km | 10km | 4 min |

---

## 3.5. Exemplo Completo - Caso Real


### 📍 Cenário Inicial

```
Cliente: Restaurante Bella Italia
Endereço: Av. Paulista, 1000 - São Paulo/SP
Valor da Entrega: R$ 25,00

Contratos do Cliente:
├── LogiFast (PRINCIPAL - contrato ativo)
├── Expresso Rápido (Secundário - contrato ativo)
└── RapidLog (Secundário - contrato suspenso)
```

### 🚴 Motoboys Disponíveis na Região

```
LogiFast (Grupo Principal)
├── João Silva - Disponível - 2.3 km da Paulista
├── Maria Santos - Disponível - 4.1 km da Paulista
└── Pedro Costa - Em entrega - 3.5 km da Paulista

Expresso Rápido (Grupo Secundário)
├── Ana Lima - Disponível - 3.8 km da Paulista
├── Carlos Souza - Disponível - 7.2 km da Paulista
└── Rafael Oliveira - Disponível - 12 km da Paulista (fora do raio)

RapidLog (Grupo Suspenso)
└── Bruno Alves - Disponível - 1.5 km (NÃO será notificado)

Outros Grupos (sem contrato com o cliente)
├── TurboLog: Fernando Costa - Disponível - 6.5 km
├── MegaEntregas: Paula Santos - Disponível - 8.3 km
└── SuperLog: Ricardo Lima - Disponível - 11.5 km (raio estendido)
```

---

### ⏱️ T = 00:00 - NÍVEL 1 ATIVADO

**O que acontece:**
- ✅ Restaurante solicita entrega pelo app
- ✅ Sistema identifica LogiFast como grupo principal
- ✅ Busca motoboys ativos da LogiFast em até 5km
- ✅ Encontra João (2.3km) e Maria (4.1km)
- ❌ Pedro está ocupado (não recebe)

**Notificações enviadas:** 2 motoboys
- 📱 João Silva (LogiFast)
- 📱 Maria Santos (LogiFast)

**Mensagem no celular deles:**
```
🚚 Nova Entrega Disponível!
Entrega de R$ 25,00 - Restaurante Bella Italia
Distância: 2.3 km
👆 Toque para aceitar
```

**⏳ Sistema aguarda 2 minutos para alguém aceitar...**

---

### ⏱️ T = 02:00 - NÍVEL 2 ATIVADO

*Ninguém aceitou no Nível 1*

**O que acontece:**
- ✅ Sistema busca grupos secundários ativos
- ✅ Encontra Expresso Rápido (RapidLog está suspenso)
- ✅ Busca motoboys da Expresso Rápido em até 5km
- ✅ Encontra Ana (3.8km)
- ✅ Expande para 10km e encontra também Carlos (7.2km)
- ❌ Rafael (12km) está muito longe

**Notificações enviadas:** 2 motoboys
- 📱 Ana Lima (Expresso Rápido)
- 📱 Carlos Souza (Expresso Rápido)

**⏳ Sistema aguarda mais 2 minutos...**

---

### ⏱️ T = 04:00 - NÍVEL 3 ATIVADO

*Ainda nenhum aceite*

**O que acontece:**
- ✅ Sistema **remove restrição** de grupos
- ✅ Busca **QUALQUER** motoboy disponível em até 10km
- ✅ Encontra motoboys de grupos **sem contrato** com o cliente

**Notificações enviadas:** 2 motoboys
- 📱 Fernando Costa (TurboLog) - 6.5km
- 📱 Paula Santos (MegaEntregas) - 8.3km

---

### ⏱️ T = 04:15 - ACEITE!

**Paula Santos aceita a entrega! 🎉**

**Divisão do valor:**
```
Valor Total: R$ 25,00
├── Paula (motoboy): R$ 21,25 (85%)
└── MegaEntregas (grupo): R$ 3,75 (15%)
```

**Resultado:**
- ✅ Cliente teve entrega atendida
- ✅ Paula ganhou R$ 21,25
- ✅ MegaEntregas ganhou R$ 3,75 (mesmo sem contrato prévio!)
- ⏱️ Tempo total até aceite: 4 minutos e 15 segundos

---

## 4.2. Cenário de Sucesso - Grupo Principal Atende

### 📍 Situação

```
Cliente: Lanchonete Sabor Bom
Grupo Principal: Expresso Rápido
Valor: R$ 18,00
Horário: 12h30 (horário de pico)
```

### ⏱️ Execução

**T = 00:00 - NÍVEL 1**
- 4 motoboys da Expresso Rápido próximos são notificados
- João aceita em **35 segundos** ⚡

**Resultado:**
```
✅ Aceite no Nível 1 (melhor cenário)
✅ Tempo de resposta: 35 segundos
✅ Cliente satisfeito com rapidez
✅ Grupo principal valorizado

Divisão:
├── João (motoboy): R$ 15,30 (85%)
└── Expresso Rápido (grupo): R$ 2,70 (15%)
```

---

## 4.3. Cenário Crítico - Horário de Pico

### 📍 Situação

```
Cliente: Farmácia 24h
Momento: Sexta-feira 20h (pico de entregas)
Problema: Todos os motoboys dos grupos principal e secundários ocupados
```

### ⏱️ Execução

**T = 00:00 - NÍVEL 1**
- Grupo principal: 0 motoboys disponíveis ❌

**T = 02:00 - NÍVEL 2**
- Grupos secundários: 0 motoboys disponíveis ❌

**T = 04:00 - NÍVEL 3**
- Sistema abre para TODOS os motoboys
- Encontra Roberto a 9km (grupo sem contrato)
- Roberto aceita! ✅

**Resultado:**
```
✅ Entrega NÃO ficou sem atendimento
✅ Nível 3 salvou a situação
✅ Cliente não percebeu o problema
✅ Grupo sem contrato ganhou uma oportunidade

Importância do Nível 3:
- Garante cobertura 24/7
- Evita perda de vendas
- Mantém satisfação do cliente
```

---

## 4.4. Análise de Performance Mensal

### 📊 Relatório de um Cliente Real

**Cliente:** Restaurante Bella Italia (100 entregas/mês)

| Nível | Quantidade | % do Total | Receita do Grupo Principal |
|-------|-----------|-----------|---------------------------|
| Nível 1 (LogiFast) | 72 entregas | 72% | R$ 216,00 (72 × R$ 3,00) |
| Nível 2 (Secundários) | 21 entregas | 21% | R$ 0,00 (não é principal) |
| Nível 3 (Sem contrato) | 7 entregas | 7% | R$ 0,00 (não tem contrato) |
| **TOTAL** | **100** | **100%** | **R$ 216,00/mês** |

**Análise para o Gerente da LogiFast:**
- ✅ **Taxa de sucesso no Nível 1: 72%** (excelente!)
- ✅ **Receita mensal: R$ 216,00** só deste cliente
- ✅ **ROI do contrato:** Compensa manter relacionamento
- ⚠️ **28% das entregas** foram perdidas para outros grupos

**Como melhorar:**
1. Aumentar número de motoboys disponíveis (+10% = R$ 30/mês a mais)
2. Incentivar resposta rápida (treinar equipe)
3. Posicionar motoboys próximos ao cliente em horários de pico

---

## 4.5. Simulação de Impacto Financeiro

### 💰 Cenário: Grupo quer virar Principal de um Cliente

**Cliente Alvo:** Empresa XYZ (150 entregas/mês, ticket médio R$ 22,00)

**Situação Atual:**
- Grupo ABC é o principal (72% de aceites no Nível 1)
- Seu grupo é secundário (aceita 18% no Nível 2)

**Receita Atual (como secundário):**
```
18% de 150 entregas = 27 entregas/mês
27 × (R$ 22,00 × 15%) = 27 × R$ 3,30 = R$ 89,10/mês
```

**Receita Potencial (se virar principal):**
```
72% de 150 entregas = 108 entregas/mês
108 × (R$ 22,00 × 15%) = 108 × R$ 3,30 = R$ 356,40/mês
```

**Ganho ao virar principal:**
```
R$ 356,40 - R$ 89,10 = R$ 267,30/mês
R$ 267,30 × 12 meses = R$ 3.207,60/ano
```

**Estratégias para conquistar:**
1. Oferecer SLA menor (responder em menos de 1 minuto)
2. Garantir disponibilidade 24/7
3. Propor comissão menor temporariamente
4. Demonstrar qualidade do serviço durante período de teste

---

## 4.6. Casos Especiais

### 🔴 Caso 1: Cliente Novo (Sem Grupo Principal)

**Situação:** Cliente acabou de se cadastrar, ainda não definiu grupo principal

**Comportamento do Sistema:**
```
Nível 1: PULADO (não tem grupo principal)
   ↓
Nível 2: PULADO (não tem contratos)
   ↓
Nível 3: ATIVADO IMEDIATAMENTE (T=0)
   └── Todos os motoboys próximos são notificados na hora
```

**Vantagem:** Cliente novo não espera 4 minutos

---

### 🟡 Caso 2: Grupo Principal Sem Motoboys Ativos

**Situação:** Grupo principal está sem nenhum motoboy disponível

**Comportamento do Sistema:**
```
Nível 1: Retorna "falso" (ninguém disponível)
   ↓
Aguarda 2 minutos? NÃO!
   ↓
Nível 2: Ativado imediatamente
   └── Pula tempo de espera quando Nível 1 não encontra ninguém
```

**Vantagem:** Não perde 2 minutos esperando o impossível

---

### 🟢 Caso 3: Todos os Níveis Falharam

**Situação:** Nenhum motoboy disponível em 10km de raio

**Comportamento do Sistema:**
```
Nível 1: 0 notificações
Nível 2: 0 notificações
Nível 3: 0 notificações
   ↓
Entrega permanece PENDENTE
   ↓
Cliente recebe mensagem:
"Aguardando motoboys ficarem disponíveis..."
   ↓
Quando algum motoboy ficar disponível:
Sistema notifica automaticamente
```

**Opções para o Cliente:**
- Aguardar motoboys ficarem disponíveis
- Aumentar valor da entrega (incentivo)
- Cancelar a solicitação

---

## 4.7. Indicadores de Sucesso (KPIs)

### 📊 Métricas para Acompanhar

| Indicador | Fórmula | Meta Ideal |
|-----------|---------|-----------|
| **Taxa de Aceite Nível 1** | (Aceites Nível 1 / Total) × 100 | > 70% |
| **Taxa de Aceite Nível 2** | (Aceites Nível 2 / Total) × 100 | 20-25% |
| **Taxa de Aceite Nível 3** | (Aceites Nível 3 / Total) × 100 | < 10% |
| **Tempo Médio de Aceite** | Soma(Tempos) / Total Entregas | < 1 min |
| **Taxa de Não-Aceite** | (Não Aceitas / Total) × 100 | < 3% |

### 📈 Relatório Mensal Exemplo

```
Mês: Novembro/2024
Total de Entregas: 1.250

Distribuição:
├── Nível 1: 900 (72%) ✅ Acima da meta
├── Nível 2: 280 (22%) ✅ Dentro da meta
├── Nível 3: 60 (5%) ✅ Abaixo da meta (bom!)
└── Não Aceitas: 10 (1%) ✅ Excelente!

Tempo Médio de Aceite: 58 segundos ✅

Receita do Grupo Principal:
900 entregas × R$ 3,00 = R$ 2.700,00
```

---

# 5. Perguntas Frequentes dos Gerentes

## ❓ Sobre o Sistema de Grupos

**P: Posso ter contrato com um cliente como grupo secundário e depois virar principal?**  
**R:** Sim! É uma estratégia comum. Você demonstra qualidade no Nível 2 e negocia virar principal.

**P: Se eu suspender um cliente, ele fica sabendo?**  
**R:** Não diretamente. Ele apenas não receberá mais motoboys do seu grupo. Outros grupos atenderão normalmente.

**P: Posso reativar um contrato suspenso?**  
**R:** Sim, a qualquer momento. O histórico é mantido.

---

## ❓ Sobre o Algoritmo de Notificações

**P: Por que esperar 2 minutos entre os níveis? Não é muito tempo?**  
**R:** 2 minutos é o tempo ideal encontrado para:
- Dar chance justa ao grupo principal
- Não sobrecarregar motoboys com notificações
- Economizar custos de envio
- Manter hierarquia comercial

**P: O que acontece se um motoboy aceitar no Nível 1 e outro aceitar no Nível 3 ao mesmo tempo?**  
**R:** O sistema aceita apenas o **primeiro que clicar**. O segundo recebe mensagem "Entrega já foi aceita".

**P: Posso ajustar o raio de busca (5km/10km)?**  
**R:** Atualmente não, mas está em desenvolvimento a possibilidade de cada grupo definir seu raio preferencial.

---

## ❓ Sobre Comissões

**P: A divisão 85/15 é fixa?**  
**R:** Atualmente sim. No futuro, pode haver negociação por volume ou performance.

**P: Quando o grupo recebe o pagamento?**  
**R:** Os 15% são creditados automaticamente após conclusão e confirmação da entrega.

**P: Se um motoboy de outro grupo aceitar uma entrega do meu cliente no Nível 3, eu perco a comissão?**  
**R:** Sim. A comissão vai para o grupo do motoboy que aceitou. Por isso é importante manter motoboys disponíveis.

---

# 6. Conclusão

## ✨ Principais Vantagens do Zapi10

### Para Clientes
- ✅ Garantia de atendimento (3 níveis de cobertura)
- ✅ Rapidez (média de 1 minuto para aceite)
- ✅ Flexibilidade (múltiplos grupos)
- ✅ Confiabilidade (nunca fica sem motoboy)

### Para Motoboys
- ✅ Mais oportunidades (trabalhar para vários grupos)
- ✅ Autonomia (escolher quais entregas aceitar)
- ✅ Transparência (sabe o valor antes de aceitar)
- ✅ Renda justa (85% do valor)

### Para Grupos
- ✅ Fidelização (contrato principal tem prioridade)
- ✅ Escalabilidade (não precisa de motoboys exclusivos)
- ✅ Previsibilidade (acesso à demanda dos clientes)
- ✅ Oportunidades (pode atender clientes de outros grupos)

---

## 🎯 Próximos Passos

O **Zapi10** está em desenvolvimento ativo com as seguintes melhorias planejadas:

1. **Painel de Gestão Avançado**
   - Relatórios em tempo real
   - Análise de performance por motoboy
   - Previsão de demanda

2. **Inteligência Artificial**
   - Prever qual motoboy tem maior chance de aceitar
   - Otimizar rotas automaticamente
   - Sugerir melhor horário para cada região

3. **Gamificação**
   - Ranking de motoboys
   - Badges por desempenho
   - Bônus por metas atingidas

4. **Personalização**
   - Cada grupo pode definir seu raio de busca
   - Tempo entre níveis configurável
   - Comissões negociáveis por volume

---

## 📞 Suporte e Contato

Para mais informações sobre o **Zapi10**, entre em contato com a equipe de desenvolvimento.

---

**Fim da Apresentação**

*Documento criado para gerentes e gestores*  
*Zapi10 - Plataforma de Entregas*  
*Novembro 2024 - Versão 1.0*
