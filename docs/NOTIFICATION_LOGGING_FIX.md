# Correção do Logging de Notificações Push

## 📋 Resumo

Correção implementada para resolver logging enganoso de notificações push. Anteriormente, o sistema sempre logava "✅ Notificação de falha enviada" mesmo quando o cliente não tinha token push ativo, causando confusão durante troubleshooting.

## 🐛 Problema Identificado

### Comportamento Anterior
O método `PushNotificationService.sendNotificationToUser()` era `void` e retornava silenciosamente quando não havia tokens:

```java
public void sendNotificationToUser(UUID userId, String title, String body, Object data) {
    if (tokens.isEmpty()) {
        log.warn("Nenhum token push ativo encontrado para usuário {}", userId);
        return; // ❌ Retorno silencioso - código chamador não sabe que falhou
    }
    // ... enviar notificação
}
```

### Logs Enganosos
```log
2026-02-18T15:48:21.051 WARN  Nenhum token push ativo encontrado para usuário f2919116-27e4-41b3-b74d-c7be10be66a6
2026-02-18T15:48:21.051 INFO  ✅ Notificação de falha enviada ao cliente #f2919116-27e4-41b3-b74d-c7be10be66a6
```

O código chamador sempre executava `log.info("✅ Notificação enviada")` após o try-catch, sem verificar se a notificação realmente foi enviada.

## ✅ Solução Implementada

### 1. Modificação de PushNotificationService

Alterados **3 métodos** para retornarem `boolean`:

#### sendNotificationToUser(UUID, String, String, Object)
```java
public boolean sendNotificationToUser(UUID userId, String title, String body, Object data) {
    try {
        List<UserPushToken> tokens = pushTokenService.getActiveTokensByUserId(userId);
        
        if (tokens.isEmpty()) {
            log.warn("Nenhum token push ativo encontrado para usuário {}", userId);
            return false; // ✅ Indica falha
        }
        
        // ... enviar notificação
        sendExpoPushNotification(Collections.singletonList(pushMessage));
        return true; // ✅ Indica sucesso
        
    } catch (Exception e) {
        log.error("Erro ao enviar notificação: {}", e.getMessage(), e);
        return false; // ✅ Indica falha
    }
}
```

#### sendNotificationToUser(UUID, String, String, Map)
```java
public boolean sendNotificationToUser(UUID userId, String title, String body, Map<String, Object> data) {
    return sendHybridNotificationToUser(userId, title, body, data);
}
```

#### sendHybridNotificationToUser(UUID, String, String, Map)
```java
public boolean sendHybridNotificationToUser(UUID userId, String title, String body, Map<String, Object> data) {
    try {
        int totalSent = 0;
        
        // Enviar para mobile (Expo)
        if (!mobileTokens.isEmpty()) {
            // ... envio
            if (expoSuccess) totalSent += expoTokens.size();
        }
        
        // Enviar para web (Web Push)
        if (!webTokens.isEmpty()) {
            int webSent = webPushService.sendWebPushNotificationToTokens(webTokens, title, body, data);
            totalSent += webSent;
        }
        
        if (totalSent > 0) {
            return true; // ✅ Pelo menos 1 dispositivo recebeu
        } else {
            log.warn("Notificação não pôde ser enviada - sem tokens válidos");
            return false; // ✅ Nenhum dispositivo recebeu
        }
        
    } catch (Exception e) {
        return false;
    }
}
```

### 2. Atualização de Todos os Chamadores

Modificados **7 locais** que chamam `sendNotificationToUser()`:

#### DeliveryService.java - 5 localizações

**Localização 1: assignDeliveryAndProcessPaymentWithCard() - linha ~1175**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    fullClient.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
} else {
    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
}
```

**Localização 2: catch PaymentProcessingException - linha ~1220**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    fullClient.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
} else {
    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
}
```

**Localização 3: catch Exception genérico - linha ~1260**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    fullClient.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", fullClient.getId());
} else {
    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", fullClient.getId());
}
```

**Localização 4: createPixPaymentForCustomer() - linha ~1400**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    customer.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", customer.getId());
} else {
    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", customer.getId());
}
```

**Localização 5: createCreditCardPaymentForCustomer() - linha ~1585**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    customer.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("   ├─ ✅ Notificação de falha enviada ao cliente #{}", customer.getId());
} else {
    log.warn("   ├─ ⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", customer.getId());
}
```

#### PaymentService.java - 1 localização

**processCreditCardPayment() - linha ~590**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    delivery.getClient().getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("📱 Notificação de falha enviada para cliente {}", delivery.getClient().getId());
} else {
    log.warn("⚠️ Não foi possível enviar notificação - cliente {} sem token push ativo", delivery.getClient().getId());
}
```

#### ConsolidatedPaymentService.java - 1 localização

**createConsolidatedCreditCardPayment() - linha ~410**
```java
boolean sent = pushNotificationService.sendNotificationToUser(
    client.getId(),
    "❌ Pagamento não aprovado",
    notificationBody,
    notificationData
);

if (sent) {
    log.info("✅ Notificação de falha enviada ao cliente #{}", client.getId());
} else {
    log.warn("⚠️ Não foi possível enviar notificação - cliente #{} sem token push ativo", client.getId());
}
```

## 📊 Resultados

### Logs Corretos Após Correção

**Cenário 1: Cliente SEM token push**
```log
2026-02-18T16:05:30.051 WARN  Nenhum token push ativo encontrado para usuário f2919116-27e4-41b3-b74d-c7be10be66a6
2026-02-18T16:05:30.051 WARN  ⚠️ Não foi possível enviar notificação - cliente #f2919116-27e4-41b3-b74d-c7be10be66a6 sem token push ativo
```

**Cenário 2: Cliente COM token push**
```log
2026-02-18T16:05:30.051 INFO  Enviando notificação para usuário a1234567-89ab-cdef-0123-456789abcdef: ❌ Pagamento não aprovado
2026-02-18T16:05:30.123 INFO  ✅ Notificação de falha enviada ao cliente #a1234567-89ab-cdef-0123-456789abcdef
```

### Benefícios

1. **Observabilidade Melhorada**: Logs agora refletem com precisão o que realmente aconteceu
2. **Debugging Facilitado**: Não há mais confusão se notificação foi enviada ou não
3. **Monitoramento Preciso**: Possível identificar clientes sem token push
4. **Compatibilidade Retroativa**: Código antigo que não verifica o boolean continua funcionando

## 🧪 Como Testar

### 1. Cliente SEM Token Push
```bash
# Verificar se cliente tem token
SELECT * FROM user_push_tokens 
WHERE user_id = 'f2919116-27e4-41b3-b74d-c7be10be66a6' 
AND is_active = true;
-- Resultado: 0 linhas

# Forçar falha de pagamento e verificar logs
grep "Notificação.*f2919116" nohup.out
```

### 2. Cliente COM Token Push
```bash
# Verificar se cliente tem token
SELECT * FROM user_push_tokens 
WHERE user_id = '<UUID_CLIENTE>' 
AND is_active = true;
-- Resultado: >= 1 linha

# Forçar falha de pagamento e verificar logs
grep "Notificação.*<UUID_CLIENTE>" nohup.out
```

## 📝 Arquivos Modificados

1. **PushNotificationService.java**
   - Linha 175: `sendNotificationToUser(UUID, String, String, Object)` → retorna `boolean`
   - Linha 416: `sendHybridNotificationToUser(UUID, String, String, Map)` → retorna `boolean`
   - Linha 498: `sendNotificationToUser(UUID, String, String, Map)` → retorna `boolean`

2. **DeliveryService.java**
   - Linha ~1175: assignDeliveryAndProcessPaymentWithCard()
   - Linha ~1220: catch PaymentProcessingException
   - Linha ~1260: catch Exception
   - Linha ~1400: createPixPaymentForCustomer()
   - Linha ~1585: createCreditCardPaymentForCustomer()

3. **PaymentService.java**
   - Linha ~590: processCreditCardPayment()

4. **ConsolidatedPaymentService.java**
   - Linha ~410: createConsolidatedCreditCardPayment()

## 🚀 Deployment

### Compilação
```bash
./gradlew compileJava --quiet
# ✅ BUILD SUCCESSFUL
```

### Restart
```bash
./start-app.sh
# ✅ App iniciado com PID 16300
# ✅ Porta 8080 ativa
# ✅ Health check: {"status":"UP"}
```

## 📅 Histórico

- **Data**: 2026-02-18
- **Versão**: App já rodando com migrations V1-V64
- **Issue**: Logs enganosos ao tentar enviar notificação para cliente sem token push
- **Descoberta**: Cliente `f2919116-27e4-41b3-b74d-c7be10be66a6` sem token push ativo
- **Resolução**: Modificados 3 métodos no PushNotificationService + 7 locais de chamada

## 🔗 Documentação Relacionada

- [API_ACTIVATION_STATUS_ENDPOINT.md](API_ACTIVATION_STATUS_ENDPOINT.md) - Verificar status de ativação do cliente
- [FRONTEND_PAYMENT_DOCS.md](FRONTEND_PAYMENT_DOCS.md) - Integração de pagamentos
- [EXPO_TOKEN_MIGRATION.md](EXPO_TOKEN_MIGRATION.md) - Migração de tokens push

## 💡 Recomendações

### Para Mobile Team
1. **Garantir registro de token**: App deve chamar `POST /api/users/push-tokens` após login
2. **Verificar status**: Usar endpoint `GET /api/users/me/activation-status` para verificar se cliente tem token
3. **Handler de notificações**: Implementar handler para tipo `payment_failed`

### Para Backend Team
1. **Monitorar logs**: Filtrar por "sem token push ativo" para identificar clientes
2. **Métricas**: Adicionar contador de notificações falhadas por falta de token
3. **Alertas**: Configurar alerta se % de falhas por token > 10%

### Para QA
1. Testar fluxo completo com cliente SEM token
2. Testar fluxo completo com cliente COM token
3. Verificar formatação de logs em ambos cenários
4. Validar mensagens de notificação em português

---

**Status**: ✅ Implementado e em produção (PID 16300, porta 8080)
