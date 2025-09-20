# Sistema de Gestão Financeira - MVT Events

## Visão Geral

O sistema de gestão financeira implementado fornece controle completo sobre pagamentos, taxas da plataforma e transferências automáticas para organizadores de eventos esportivos.

## Arquitetura Financeira

### Estratégia Multi-Tenant

- **Event-as-Tenant**: Cada evento funciona como um tenant isolado
- **Row Level Security (RLS)**: Isolamento de dados a nível de linha no PostgreSQL
- **Contexto de Evento**: Segurança baseada no contexto do evento atual

### Entidades Principais

#### 1. EventFinancials

**Tabela**: `event_financials`

Consolida informações financeiras por evento:

- **Receita Total**: Soma de todos os pagamentos recebidos
- **Taxas da Plataforma**: Percentual cobrado pela plataforma
- **Receita Líquida**: Valor destinado ao organizador
- **Valor Pendente**: Quantia aguardando transferência
- **Valor Transferido**: Total já transferido ao organizador
- **Frequência de Transferência**: Imediata, diária, semanal, mensal ou sob demanda

#### 2. Transfer

**Tabela**: `transfers`

Gerencia transferências de valores para organizadores:

- **Métodos Suportados**: PIX, TED, Transferência Bancária
- **Status de Rastreamento**: Pendente, Processando, Concluído, Falhou
- **Integração com Gateway**: Suporte para múltiplos provedores de pagamento
- **Retry Logic**: Tentativas automáticas para transferências falhadas

#### 3. PaymentEvent

**Tabela**: `payment_events`

Auditoria completa de eventos financeiros:

- **Tipos de Evento**: Pagamento recebido, estorno, taxa calculada, transferência iniciada
- **Rastreabilidade**: Log completo de todas as operações financeiras
- **Metadados**: Informações adicionais em formato JSON

#### 4. Payment

**Tabela**: `payments`

Gestão de pagamentos individuais:

- **Status Completo**: Pendente, processando, concluído, falhou, estornado
- **Métodos de Pagamento**: Cartão, PIX, transferência bancária
- **Integração Gateway**: Suporte para múltiplos provedores

## Funcionalidades Principais

### 1. Processamento Automático de Pagamentos

```java
// Exemplo de uso
FinancialService.processPayment(payment);
```

- Calcula automaticamente a taxa da plataforma
- Atualiza o valor líquido do organizador
- Registra eventos de auditoria
- Agenda próxima transferência

### 2. Transferências Programadas

```java
// Frequências suportadas
TransferFrequency.IMMEDIATE  // Imediata
TransferFrequency.DAILY      // Diária
TransferFrequency.WEEKLY     // Semanal
TransferFrequency.MONTHLY    // Mensal
TransferFrequency.ON_DEMAND  // Sob demanda
```

### 3. Sistema de Retry Automático

- **Transferências Falhadas**: Tentativas automáticas a cada 4 horas
- **Limite de Tentativas**: Máximo 3 tentativas por transferência
- **Logging Detalhado**: Razão da falha e histórico de tentativas

### 4. Gateway de Pagamento Mock

Implementação simulada com:

- **Validação PIX**: Verificação de formato de chave PIX
- **Cálculo de Taxas**: Baseado no método de transferência
- **Tempo de Processamento**: Estimativas realistas por método

## Endpoints da API

### Financeiro - `/api/financial`

#### Resumo Financeiro do Evento

```http
GET /api/financial/events/{eventId}/summary
Authorization: Bearer {token}
```

#### Criar Transferência Manual

```http
POST /api/financial/events/{eventId}/transfers
Content-Type: application/json
Authorization: Bearer {token}

{
    "amount": 100.00,
    "transferMethod": "PIX",
    "destinationKey": "email@example.com"
}
```

#### Calcular Taxa de Transferência

```http
GET /api/financial/transfer-fee?amount=100.00&method=PIX
Authorization: Bearer {token}
```

#### Processar Transferências (Admin)

```http
POST /api/financial/transfers/process
Authorization: Bearer {token}
```

## Configurações

### Taxas Padrão

- **Taxa da Plataforma**: 5% (configurável por evento)
- **Valor Mínimo para Transferência**: R$ 10,00
- **Taxa PIX**: 1% do valor
- **Taxa TED**: R$ 5,00 fixo
- **Taxa Transferência Bancária**: R$ 2,50 fixo

### Agendamentos Automáticos

- **Transferências Automáticas**: A cada hora
- **Transferências Pendentes**: A cada 30 minutos
- **Retry de Falhas**: A cada 4 horas

## Segurança

### Row Level Security (RLS)

```sql
-- Política de isolamento por evento
CREATE POLICY event_financials_isolation ON event_financials
    FOR ALL TO event_organizer
    USING (event_id = current_setting('app.current_event_id', true)::uuid);
```

### Autorização JWT

- **ORGANIZER**: Acesso aos dados do próprio evento
- **ADMIN**: Acesso completo a todos os dados
- **Context Aware**: Segurança baseada no contexto do evento

## Monitoramento

### Logs Detalhados

- Todas as operações financeiras são logadas
- Transferências automáticas incluem métricas de desempenho
- Falhas incluem razão detalhada e stack trace

### Métricas Disponíveis

- Total de transferências pendentes
- Volume total transferido
- Taxa de sucesso das transferências
- Tempo médio de processamento

## Exemplo de Fluxo Completo

1. **Pagamento Recebido**: Sistema processa automaticamente
2. **Taxa Calculada**: 5% retido pela plataforma
3. **Valor Líquido**: Adicionado ao saldo pendente do organizador
4. **Agendamento**: Próxima transferência programada conforme frequência
5. **Transferência Automática**: Sistema executa no horário programado
6. **Confirmação**: Gateway confirma sucesso/falha
7. **Auditoria**: Todos os eventos registrados para compliance

## Extensibilidade

### Novos Gateways de Pagamento

Implementar interface `PaymentGatewayService` para adicionar novos provedores.

### Novas Funcionalidades

- Sistema de split payment para múltiplos beneficiários
- Escrow para eventos com política de reembolso
- Integração com sistemas de contabilidade
- Dashboard financeiro em tempo real

Este sistema fornece uma base sólida e escalável para gestão financeira de plataformas de eventos esportivos, com foco em automação, segurança e auditabilidade.
