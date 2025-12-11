# 🚀 Endpoint de Consolidação de Pagamentos - Implementação Assíncrona

## 📋 Resumo

Implementação de um endpoint REST que dispara o processamento de consolidação de pagamentos em uma thread separada do backend. O frontend pode disparar o processo e monitorar seu progresso via polling.

## 🎯 Objetivo

Permitir que o frontend dispare o processamento de pagamentos consolidados sem bloquear a aplicação. O processamento ocorre em background e o status pode ser consultado a qualquer momento.

## 📦 Arquivos Criados/Modificados

### 1. **ConsolidatedPaymentController.java** ✅ (NOVO)
**Localização:** `src/main/java/com/mvt/mvt_events/controller/ConsolidatedPaymentController.java`

**Endpoints:**
- `POST /api/consolidated-payments/process-all` - Dispara processamento assíncrono
- `GET /api/consolidated-payments/status/{taskId}` - Consulta status de uma tarefa

**Recursos:**
- Validação de autorização (requer ADMIN)
- Retorna status HTTP 202 (Accepted) para requisições assíncronas
- Suporte a polling via GET /status/{taskId}

### 2. **ConsolidatedPaymentTaskTracker.java** ✅ (NOVO)
**Localização:** `src/main/java/com/mvt/mvt_events/service/ConsolidatedPaymentTaskTracker.java`

**Responsabilidade:** Rastrear estado de tarefas de consolidação em memória

**Métodos Públicos:**
- `createTask()` - Cria nova tarefa com estado QUEUED
- `markAsProcessing(taskId)` - Marca como iniciada
- `updateProgress(taskId, percentage, message)` - Atualiza progresso
- `markAsCompleted(taskId, statistics)` - Marca como concluída
- `markAsFailed(taskId, errorMessage, errors)` - Marca como falha
- `getTaskStatus(taskId)` - Recupera status completo
- `taskExists(taskId)` - Verifica existência
- `removeTask(taskId)` - Remove tarefa (limpeza)
- `cleanupOldTasks()` - Remove tarefas antigas (24h+)
- `getAllTasks()` - Lista todas (debug)

**Ciclo de Vida da Tarefa:**
```
QUEUED (criação) → PROCESSING (início) → COMPLETED (sucesso) ou FAILED (erro)
```

### 3. **ConsolidatedPaymentProcessResponse.java** ✅ (NOVO)
**Localização:** `src/main/java/com/mvt/mvt_events/dto/ConsolidatedPaymentProcessResponse.java`

**Campos:**
- `taskId` - ID único da tarefa
- `status` - Estado atual (QUEUED, PROCESSING, COMPLETED, FAILED)
- `message` - Mensagem informativa
- `startedAt` - Timestamp de início
- `completedAt` - Timestamp de conclusão
- `statistics` - Mapa de estatísticas (opcional)
- `errors` - Lista de erros (opcional)
- `progressPercentage` - Progresso 0-100

### 4. **ConsolidatedPaymentService.java** ✅ (MODIFICADO)
**Localização:** `src/main/java/com/mvt/mvt_events/service/ConsolidatedPaymentService.java`

**Alterações:**
- Injetado `ConsolidatedPaymentTaskTracker`
- Novo overload: `processAllClientsConsolidatedPayments(String taskId)`
- Mantém compatibilidade com método original sem taskId
- Integração com task tracker:
  - Atualiza progresso durante processamento
  - Marca como COMPLETED ou FAILED
  - Passa estatísticas de volta

## 🔄 Fluxo de Processamento

### 1. Frontend dispara requisição
```bash
POST /api/consolidated-payments/process-all
Authorization: Bearer {token}
```

### 2. Backend retorna 202 ACCEPTED
```json
{
  "taskId": "5fa2c5d0-1234-4567-89ab-cdef01234567",
  "status": "QUEUED",
  "message": "Processamento de pagamentos consolidados enfileirado",
  "progressPercentage": 0
}
```

### 3. Frontend faz polling para monitorar progresso
```bash
GET /api/consolidated-payments/status/{taskId}
Authorization: Bearer {token}
```

### 4. Backend retorna status atualizado
```json
{
  "taskId": "5fa2c5d0-1234-4567-89ab-cdef01234567",
  "status": "PROCESSING",
  "message": "Processando cliente 5 de 12",
  "startedAt": "2025-12-11T10:30:00",
  "progressPercentage": 45
}
```

### 5. Processamento completa
```json
{
  "taskId": "5fa2c5d0-1234-4567-89ab-cdef01234567",
  "status": "COMPLETED",
  "message": "Processamento concluído com sucesso",
  "startedAt": "2025-12-11T10:30:00",
  "completedAt": "2025-12-11T11:15:30",
  "statistics": {
    "processedClients": 12,
    "createdPayments": 15,
    "includedDeliveries": 48
  },
  "progressPercentage": 100
}
```

## 💻 Exemplo Frontend (React/TypeScript)

```typescript
// 1. Disparar processamento
const handleProcessPayments = async () => {
  try {
    const response = await fetch('/api/consolidated-payments/process-all', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.status === 202) {
      const data = await response.json();
      const { taskId } = data;
      
      // Armazenar taskId para polling
      setCurrentTaskId(taskId);
      setShowProgress(true);
      startPolling(taskId);
    }
  } catch (error) {
    console.error('Erro ao disparar processamento:', error);
  }
};

// 2. Monitorar progresso via polling
const startPolling = (taskId: string) => {
  const interval = setInterval(async () => {
    try {
      const response = await fetch(
        `/api/consolidated-payments/status/${taskId}`,
        { headers: { 'Authorization': `Bearer ${token}` } }
      );
      
      const status = await response.json();
      
      // Atualizar UI com status
      setTaskStatus(status);
      
      // Se completou ou falhou, parar polling
      if (status.status === 'COMPLETED' || status.status === 'FAILED') {
        clearInterval(interval);
        setShowProgress(false);
        
        if (status.status === 'COMPLETED') {
          showSuccessMessage(`
            Processamento concluído!
            - ${status.statistics.createdPayments} pagamentos criados
            - ${status.statistics.includedDeliveries} entregas incluídas
          `);
        } else {
          showErrorMessage(`Processamento falhou: ${status.message}`);
        }
      }
    } catch (error) {
      console.error('Erro ao consultar status:', error);
      clearInterval(interval);
    }
  }, 5000); // Polling a cada 5 segundos
};

// 3. Renderizar interface
return (
  <>
    <button 
      onClick={handleProcessPayments}
      disabled={showProgress}
    >
      {showProgress ? 'Processando...' : 'Processar Pagamentos'}
    </button>

    {showProgress && (
      <ProgressBar 
        value={taskStatus?.progressPercentage || 0}
        message={taskStatus?.message}
        status={taskStatus?.status}
      />
    )}
  </>
);
```

## 🔒 Segurança

- ✅ Autenticação JWT obrigatória
- ✅ Autorização: apenas ADMIN pode disparar
- ✅ TaskId é UUID aleatório (não previsível)
- ✅ Tarefas limpas após 24h de inatividade

## 📊 Estrutura de Dados

### Task Tracker (Em Memória - ConcurrentHashMap)
```
Map<String taskId, ConsolidatedPaymentProcessResponse>
```

**Ciclo de Vida:**
1. Criação com estado QUEUED
2. Transição para PROCESSING ao iniciar
3. Atualização de progresso (0-100%)
4. Transição final para COMPLETED ou FAILED
5. Limpeza automática após 24h

## ⚙️ Configuração

Nenhuma configuração especial necessária. O componente é totalmente automático:
- Task tracker é singleton (gerenciado pelo Spring)
- Usa `CompletableFuture` para processamento assíncrono
- Usa `ConcurrentHashMap` para thread-safety

## 🧪 Testes (Exemplos CURL)

### 1. Disparar processamento
```bash
curl -X POST http://localhost:8080/api/consolidated-payments/process-all \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json"

# Response:
# {
#   "taskId": "...",
#   "status": "QUEUED",
#   "message": "Processamento de pagamentos consolidados enfileirado",
#   "progressPercentage": 0
# }
```

### 2. Monitorar status
```bash
curl -X GET http://localhost:8080/api/consolidated-payments/status/{taskId} \
  -H "Authorization: Bearer {token}"

# Response:
# {
#   "taskId": "...",
#   "status": "PROCESSING",
#   "message": "Processando cliente 5 de 12",
#   "progressPercentage": 45,
#   ...
# }
```

## 🚀 Próximos Passos

1. **Frontend**: Criar UI com botão "Processar Pagamentos"
2. **Frontend**: Implementar polling para monitorar progresso
3. **Frontend**: Mostrar ProgressBar e estatísticas
4. **Backend**: Considerar persistência de histórico (logs)
5. **Backend**: Webhooks para notificar frontend em tempo real (WebSocket)

## 📝 Notas Importantes

- ⚠️ **Task tracker em memória**: Tarefas são perdidas em restart da aplicação
- ⚠️ **Escalabilidade**: Para múltiplas instâncias, use Redis para rastrear
- ✅ **Thread-safe**: Usa `ConcurrentHashMap` para sincronização
- ✅ **Non-blocking**: RetresultIndex HTTP 202 imediatamente

## 📦 Dependências

Todas já presentes no projeto:
- Spring Boot (Web, Data JPA, Transaction)
- Lombok (anotações)
- Jackson (JSON serialization)
- Jakarta Persistence (JPA)

Nenhuma dependência nova foi adicionada.

## ✅ Checklist de Implementação

- [x] ConsolidatedPaymentController criado
- [x] ConsolidatedPaymentTaskTracker criado
- [x] ConsolidatedPaymentProcessResponse DTO criado
- [x] ConsolidatedPaymentService modificado para suportar taskId
- [x] Dois endpoints implementados (POST, GET)
- [x] Segurança (autenticação + autorização)
- [x] Thread-safety (ConcurrentHashMap)
- [x] Documentação Swagger
- [x] Compilação sem erros
- [x] Documentação completa neste arquivo

## 🎓 Como Usar

1. **Frontend**:
   - Adicionar botão "Processar Pagamentos Consolidados"
   - Disparar POST /api/consolidated-payments/process-all
   - Receber taskId na resposta
   - Iniciar polling com GET /api/consolidated-payments/status/{taskId}
   - Atualizar ProgressBar a cada resposta
   - Parar quando status for COMPLETED ou FAILED

2. **Backend**:
   - Tudo pronto! Endpoint está funcional
   - Processamento ocorre em background automaticamente
   - Logs detalhados no console da aplicação
