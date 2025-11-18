# 🔍 Diagnóstico Completo - Push Notifications

## Status Atual (06/11/2025 02:59)

### ✅ O que ESTÁ funcionando:
1. Backend envia para Expo: **200 OK**
2. Expo aceita mensagem: **status: "ok"**
3. IDs de notificação gerados:
   - `019a57bc-b555-7971-a6de-902b02bac912` (Delivery 13)
   - `019a57bf-10c6-7f93-adfd-fb0753b110e6` (Delivery 14)
   - `019a57c3-5bf8-7301-a9fb-7418cc0a27d6` (Teste direto)

### ❌ Problema:
- Notificações NÃO chegam no iPhone

### 🔎 Possível Causa Identificada:
**Token pode estar TRUNCADO ou INVÁLIDO**

Token registrado no banco:
```
ExponentPushToken[2nCfzTFPgiBICBsPPD60_s]
```

**Comprimento: 41 caracteres** - Isso parece incompleto!

Um ExponentPushToken normal tem este formato:
```
ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]
```
Onde `x` são 22+ caracteres alfanuméricos.

## 🔧 SOLUÇÃO IMEDIATA:

### Passo 1: Limpar token antigo
```bash
docker exec mvt-events-db psql -U mvt -d mvt-events -c "DELETE FROM user_push_tokens WHERE user_id = '6186c7af-2311-4756-bfc6-ce98bd31ed27';"
```

### Passo 2: No iPhone
1. **FECHE COMPLETAMENTE** o app (deslizar para cima no App Switcher)
2. **FORCE-QUIT** o app
3. **REABRA** o app
4. **FAÇA LOGIN** novamente com motoboy1@gmail.com
5. **AGUARDE 10 segundos** para o token ser registrado

### Passo 3: Verificar novo token
```bash
docker exec mvt-events-db psql -U mvt -d mvt-events -c "SELECT token, length(token), created_at FROM user_push_tokens WHERE user_id = '6186c7af-2311-4756-bfc6-ce98bd31ed27' ORDER BY created_at DESC LIMIT 1;"
```

**O token deve ter 50+ caracteres!**

### Passo 4: Criar nova delivery para testar
```bash
# Obter token JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"moveltrack@gmail.com","password":"123456"}' | jq -r '.token')

# Criar delivery
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "client": "189c7d79-cb21-40c1-9b7c-006ebaa3289a",
    "fromAddress": "R. Nova Tentativa, 111",
    "fromLatitude": -3.8710,
    "fromLongitude": -40.9163,
    "toAddress": "R. Destino Novo, 222",
    "toLatitude": -3.8669,
    "toLongitude": -40.9176,
    "totalAmount": 999.99,
    "itemDescription": "🎯 TESTE APÓS REREGISTRAR TOKEN",
    "recipientName": "Cliente Novo",
    "recipientPhone": "85944444444"
  }'
```

## 📋 Checklist de Verificação no iPhone:

- [ ] App fechado completamente (não apenas em background)
- [ ] App reaberto e login feito
- [ ] Permissões de notificação ATIVADAS (Configurações > Notificações > [App])
- [ ] Token novo registrado no backend (verificar com SQL acima)
- [ ] Nova delivery criada
- [ ] **AGUARDAR 10 segundos** com o app FECHADO
- [ ] Verificar se notificação apareceu na tela de bloqueio

## 🎯 Verificação de Tickets do Expo:

Você pode verificar se a notificação foi entregue consultando os receipts:

```bash
# Para verificar delivery dos últimos IDs
curl -X POST https://exp.host/--/api/v2/push/getReceipts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer q_E_RBFanVt2NfWO2isuYFwEZ1u3d2sxdiagNFRZ" \
  -d '{
    "ids": [
      "019a57bc-b555-7971-a6de-902b02bac912",
      "019a57bf-10c6-7f93-adfd-fb0753b110e6",
      "019a57c3-5bf8-7301-a9fb-7418cc0a27d6"
    ]
  }'
```

Se o status for `error`, veremos o motivo (ex: `DeviceNotRegistered`, `InvalidCredentials`, etc.)

## 📱 Outras Verificações no iOS:

1. **Modo Não Perturbe**: Desative no iPhone
2. **Notificações Agrupadas**: Verifique se não estão ocultas
3. **Configurações de Tela de Bloqueio**: Ative "Mostrar na Tela de Bloqueio"
4. **Foco**: Desative qualquer modo Foco ativo

## 💡 Informação Importante:

**Expo Go** e apps em **desenvolvimento** podem ter limitações com notificações push em iOS. Para produção total, considere:

1. Build standalone/EAS Build
2. Configurar APNs (Apple Push Notification service)
3. Upload de certificados no Expo Dashboard
