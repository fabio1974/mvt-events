# 📱 Integração Push Notifications - Verificação Bancária Iugu

**Data**: 2025-12-02  
**Status**: ✅ Implementado e compilado com sucesso

---

## 🎯 Visão Geral

Sistema de notificações push via **Firebase Cloud Messaging (FCM/Expo)** para alertar motoboys/gerentes sobre o status de verificação de seus dados bancários no Iugu.

---

## 🔔 Tipos de Notificações Implementadas

### 1️⃣ Dados Bancários Verificados ✅

**Quando:** Iugu aprova os dados bancários (2-5 dias após cadastro)

**Método:** `PushNotificationService.notifyBankDataVerified()`

**Notificação:**
```json
{
  "title": "✅ Dados Bancários Verificados!",
  "body": "Seus dados do Nubank foram aprovados! Você já pode receber pagamentos via PIX.",
  "data": {
    "type": "BANK_VERIFICATION_APPROVED",
    "bankName": "Nubank",
    "maskedAccount": "****-5678",
    "screen": "BankDataScreen"
  },
  "sound": "default",
  "priority": "normal"
}
```

**Ação no App:**
- Toca som de notificação
- Exibe banner no topo
- Ao clicar, navega para tela de Dados Bancários
- Mostra status ✅ **Verificado**

---

### 2️⃣ Dados Bancários Rejeitados ❌

**Quando:** Iugu rejeita os dados bancários (CPF não bate, conta inválida, etc.)

**Método:** `PushNotificationService.notifyBankDataRejected()`

**Notificação:**
```json
{
  "title": "⚠️ Dados Bancários Rejeitados",
  "body": "Seus dados bancários foram rejeitados. Por favor, revise e atualize as informações.",
  "data": {
    "type": "BANK_VERIFICATION_REJECTED",
    "reason": "Dados bancários incorretos ou conta inválida. Verifique CPF, agência e conta.",
    "screen": "BankDataScreen"
  },
  "sound": "default",
  "priority": "normal"
}
```

**Ação no App:**
- Toca som de alerta
- Exibe banner vermelho
- Ao clicar, navega para tela de Dados Bancários
- Mostra botão **"Atualizar Dados"**

---

### 3️⃣ Pagamento Recebido 💰 (Bônus)

**Quando:** Cliente paga a entrega via PIX

**Método:** `PushNotificationService.notifyPaymentReceived()`

**Notificação:**
```json
{
  "title": "💰 Pagamento Recebido!",
  "body": "Você recebeu R$ 87,00 de pagamento. A transferência será feita em D+1.",
  "data": {
    "type": "PAYMENT_RECEIVED",
    "amount": "87.00",
    "deliveryId": "uuid-123",
    "screen": "PaymentsScreen"
  },
  "sound": "default",
  "priority": "normal"
}
```

---

### 4️⃣ Transferência Bancária Concluída 🏦 (Bônus)

**Quando:** Iugu transfere o dinheiro para a conta do motoboy (D+1)

**Método:** `PushNotificationService.notifyWithdrawalCompleted()`

**Notificação:**
```json
{
  "title": "🏦 Transferência Concluída!",
  "body": "R$ 87,00 foram transferidos para sua conta Nubank.",
  "data": {
    "type": "WITHDRAWAL_COMPLETED",
    "amount": "87.00",
    "bankName": "Nubank",
    "screen": "PaymentsScreen"
  },
  "sound": "default",
  "priority": "normal"
}
```

---

## 🔄 Fluxo Completo de Verificação com Notificações

```
┌─────────────────────────────────────────────────────────────┐
│ DIA 1 - 14:30: Motoboy cadastra dados bancários            │
├─────────────────────────────────────────────────────────────┤
│ • POST /api/motoboy/bank-data                               │
│ • Backend cria BankAccount (status: PENDING_VALIDATION)     │
│ • Backend cria subconta Iugu (verification_status: pending) │
│ • App mostra: "⏳ Verificação em andamento (2-5 dias)"      │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ DIA 1 - 18:00: Job Agendado #1                             │
├─────────────────────────────────────────────────────────────┤
│ • IuguVerificationSyncService executa                       │
│ • GET /v1/accounts/acc_ABC123                               │
│ • Response: verification_status = "pending"                 │
│ • Nada muda (ainda pendente)                                │
│ • ❌ Notificação NÃO enviada                                │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ DIA 2 - 00:00: Job Agendado #2                             │
├─────────────────────────────────────────────────────────────┤
│ • IuguVerificationSyncService executa                       │
│ • GET /v1/accounts/acc_ABC123                               │
│ • Response: verification_status = "pending"                 │
│ • Nada muda (ainda pendente)                                │
│ • ❌ Notificação NÃO enviada                                │
└─────────────────────────────────────────────────────────────┘
                           ↓
                       (... 2-3 dias ...)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ DIA 3 - 12:00: Job Agendado #6 (Iugu aprovou!)             │
├─────────────────────────────────────────────────────────────┤
│ 1. IuguVerificationSyncService executa                      │
│    • GET /v1/accounts/acc_ABC123                            │
│    • Response: verification_status = "verified" ✅          │
│                                                             │
│ 2. Backend atualiza banco local                             │
│    • BankAccount.status = ACTIVE                            │
│    • bankAccountRepository.save()                           │
│                                                             │
│ 3. Backend envia Push Notification 📱                       │
│    • pushNotificationService.notifyBankDataVerified()       │
│    • FCM envia para todos os tokens do usuário              │
│                                                             │
│ 4. App Mobile recebe notificação                            │
│    • Toca som: "ding.mp3"                                   │
│    • Exibe banner: "✅ Dados Bancários Verificados!"        │
│    • Motoboy clica na notificação                           │
│    • App navega para BankDataScreen                         │
│    • Mostra status: ✅ Verificado                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Implementação Técnica

### 1. Métodos Adicionados ao PushNotificationService

```java
@Service
public class PushNotificationService {
    
    /**
     * Notifica aprovação de dados bancários
     */
    public void notifyBankDataVerified(UUID userId, String bankName, String maskedAccount) {
        Map<String, Object> data = Map.of(
            "type", "BANK_VERIFICATION_APPROVED",
            "bankName", bankName,
            "maskedAccount", maskedAccount,
            "screen", "BankDataScreen"
        );
        
        sendNotificationToUser(
            userId,
            "✅ Dados Bancários Verificados!",
            String.format("Seus dados do %s foram aprovados! Você já pode receber pagamentos via PIX.", bankName),
            data
        );
    }
    
    /**
     * Notifica rejeição de dados bancários
     */
    public void notifyBankDataRejected(UUID userId, String reason) {
        Map<String, Object> data = Map.of(
            "type", "BANK_VERIFICATION_REJECTED",
            "reason", reason,
            "screen", "BankDataScreen"
        );
        
        sendNotificationToUser(
            userId,
            "⚠️ Dados Bancários Rejeitados",
            "Seus dados bancários foram rejeitados. Por favor, revise e atualize as informações.",
            data
        );
    }
}
```

---

### 2. Integração no IuguVerificationSyncService

```java
@Service
public class IuguVerificationSyncService {
    
    private final PushNotificationService pushNotificationService;
    
    private SyncResult handleVerified(BankAccount bankAccount, User user, String iuguAccountId) {
        // 1. Atualiza status local
        bankAccount.markAsActive();
        bankAccountRepository.save(bankAccount);
        
        // 2. Envia notificação push 📱
        try {
            pushNotificationService.notifyBankDataVerified(
                user.getId(),
                bankAccount.getBankName(),
                bankAccount.getAccountNumberMasked()
            );
            log.info("   ├─ 📱 Push notification enviada com sucesso");
        } catch (Exception e) {
            log.error("   ├─ ⚠️ Erro ao enviar push notification: {}", e.getMessage());
        }
        
        return SyncResult.VERIFIED;
    }
    
    private SyncResult handleRejected(BankAccount bankAccount, User user, String iuguAccountId) {
        // 1. Atualiza status local
        bankAccount.setStatus(BankAccountStatus.BLOCKED);
        bankAccountRepository.save(bankAccount);
        
        // 2. Envia notificação push 📱
        try {
            pushNotificationService.notifyBankDataRejected(
                user.getId(),
                "Dados bancários incorretos ou conta inválida. Verifique CPF, agência e conta."
            );
            log.warn("   ├─ 📱 Push notification de rejeição enviada");
        } catch (Exception e) {
            log.error("   ├─ ⚠️ Erro ao enviar push notification: {}", e.getMessage());
        }
        
        return SyncResult.REJECTED;
    }
}
```

---

## 📊 Logs Esperados

### Job Executando com Sucesso

```
2025-12-03 12:00:00.123 INFO  [IuguVerificationSyncService] 🔄 ========================================
2025-12-03 12:00:00.124 INFO  [IuguVerificationSyncService] 🔄 Iniciando sincronização de verificações Iugu...
2025-12-03 12:00:00.125 INFO  [IuguVerificationSyncService] 🔄 ========================================
2025-12-03 12:00:00.234 INFO  [IuguVerificationSyncService] 📋 Encontradas 3 contas pendentes de verificação

2025-12-03 12:00:01.345 DEBUG [IuguVerificationSyncService] 🔍 Consultando status da subconta: acc_ABC123 (User: joao_motoboy)
2025-12-03 12:00:01.567 INFO  [IuguService] GET https://api.iugu.com/v1/accounts/acc_ABC123
2025-12-03 12:00:02.123 INFO  [IuguVerificationSyncService] ✅ Conta bancária VERIFICADA: acc_ABC123 (User: joao_motoboy)
2025-12-03 12:00:02.234 INFO  [PushNotificationService] 📢 Notificando usuário uuid-456 sobre verificação bancária aprovada
2025-12-03 12:00:02.345 INFO  [PushNotificationService] 📱 PUSH: ✅ Dados Bancários Verificados! -> Seus dados do Nubank foram aprovados!
2025-12-03 12:00:02.456 INFO  [PushNotificationService] ✅ Notificação de verificação aprovada enviada para usuário uuid-456
2025-12-03 12:00:02.567 INFO  [IuguVerificationSyncService]    ├─ 📱 Push notification enviada com sucesso
2025-12-03 12:00:02.678 INFO  [IuguVerificationSyncService]    └─ ✅ Status atualizado para ACTIVE no banco local

2025-12-03 12:00:03.789 DEBUG [IuguVerificationSyncService] 🔍 Consultando status da subconta: acc_DEF456 (User: maria_gerente)
2025-12-03 12:00:04.012 INFO  [IuguService] GET https://api.iugu.com/v1/accounts/acc_DEF456
2025-12-03 12:00:04.234 WARN  [IuguVerificationSyncService] ❌ Conta bancária REJEITADA: acc_DEF456 (User: maria_gerente)
2025-12-03 12:00:04.345 INFO  [PushNotificationService] 📢 Notificando usuário uuid-789 sobre verificação bancária rejeitada
2025-12-03 12:00:04.456 WARN  [IuguVerificationSyncService]    ├─ 📱 Push notification de rejeição enviada
2025-12-03 12:00:04.567 WARN  [IuguVerificationSyncService]    └─ ❌ Status atualizado para BLOCKED no banco local

2025-12-03 12:00:05.678 DEBUG [IuguVerificationSyncService] 🔍 Consultando status da subconta: acc_GHI789 (User: pedro_motoboy)
2025-12-03 12:00:05.890 INFO  [IuguService] GET https://api.iugu.com/v1/accounts/acc_GHI789
2025-12-03 12:00:06.123 DEBUG [IuguVerificationSyncService] ⏳ Conta ainda PENDENTE: acc_GHI789 (User: pedro_motoboy)

2025-12-03 12:00:06.234 INFO  [IuguVerificationSyncService] 🔄 ========================================
2025-12-03 12:00:06.235 INFO  [IuguVerificationSyncService] ✅ Sincronização concluída!
2025-12-03 12:00:06.236 INFO  [IuguVerificationSyncService]    ├─ ✅ Verificadas: 1
2025-12-03 12:00:06.237 INFO  [IuguVerificationSyncService]    ├─ ❌ Rejeitadas: 1
2025-12-03 12:00:06.238 INFO  [IuguVerificationSyncService]    ├─ ⏳ Ainda pendentes: 1
2025-12-03 12:00:06.239 INFO  [IuguVerificationSyncService]    └─ ⚠️ Erros: 0
2025-12-03 12:00:06.240 INFO  [IuguVerificationSyncService] 🔄 ========================================
```

---

## 📱 Comportamento no App Mobile

### Cenário 1: Notificação de Aprovação

**App em Background:**
1. Push notification chega
2. Toca som "ding.mp3"
3. Exibe banner no topo: "✅ Dados Bancários Verificados!"
4. Mostra badge no ícone do app (se configurado)
5. Motoboy clica na notificação
6. App abre na tela `BankDataScreen`
7. Status exibido: ✅ **Verificado - Você pode receber pagamentos**

**App em Foreground:**
1. Push notification chega
2. App mostra modal/snackbar: "✅ Seus dados foram verificados!"
3. Motoboy pode clicar para ir à tela de dados bancários
4. Ou continuar o que estava fazendo

---

### Cenário 2: Notificação de Rejeição

**App em Background:**
1. Push notification chega
2. Toca som de alerta "alert.mp3"
3. Exibe banner vermelho: "⚠️ Dados Bancários Rejeitados"
4. Motoboy clica na notificação
5. App abre na tela `BankDataScreen`
6. Status exibido: ❌ **Rejeitado - Atualize seus dados**
7. Botão destacado: **"Atualizar Dados Bancários"**

---

## 🔧 Configuração no App Mobile

### React Native / Expo

```typescript
// services/notificationHandler.ts
import * as Notifications from 'expo-notifications';

// Configurar handler de notificações
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

// Listener para quando usuário clica na notificação
Notifications.addNotificationResponseReceivedListener(response => {
  const data = response.notification.request.content.data;
  
  switch (data.type) {
    case 'BANK_VERIFICATION_APPROVED':
      navigation.navigate('BankDataScreen');
      showSuccessToast('✅ Seus dados foram verificados!');
      break;
      
    case 'BANK_VERIFICATION_REJECTED':
      navigation.navigate('BankDataScreen');
      showErrorToast('❌ Revise seus dados bancários');
      break;
      
    case 'PAYMENT_RECEIVED':
      navigation.navigate('PaymentsScreen');
      showSuccessToast(`💰 Você recebeu R$ ${data.amount}`);
      break;
      
    case 'WITHDRAWAL_COMPLETED':
      navigation.navigate('PaymentsScreen');
      showSuccessToast(`🏦 R$ ${data.amount} transferidos!`);
      break;
  }
});
```

---

## ✅ Checklist de Implementação

- [x] ✅ Adicionar métodos de notificação ao `PushNotificationService`
- [x] ✅ Integrar com `IuguVerificationSyncService`
- [x] ✅ Compilação bem-sucedida
- [ ] ⏳ Testar envio de notificações em ambiente de dev
- [ ] ⏳ Configurar telas no app mobile para receber deep links
- [ ] ⏳ Testar fluxo end-to-end (cadastro → verificação → notificação)
- [ ] ⏳ Adicionar analytics para rastrear taxa de abertura das notificações

---

## 🎯 Próximos Passos

1. **Testar Job em Dev:**
   - Cadastrar dados bancários de teste
   - Forçar status `verified` no Iugu (sandbox)
   - Verificar se notificação foi enviada

2. **Configurar Deep Links no App:**
   - Tela `BankDataScreen` deve abrir ao clicar
   - Passar parâmetros via `data.screen`

3. **Adicionar Analytics:**
   - Rastrear quantas notificações foram enviadas
   - Taxa de abertura (click-through rate)
   - Tempo médio até abrir a notificação

4. **Melhorias Futuras:**
   - Adicionar botões de ação na notificação (Android)
   - Agrupar notificações (múltiplas entregas)
   - Notificações ricas com imagens

---

**Mantido por**: Equipe de Backend  
**Última atualização**: 2025-12-02
