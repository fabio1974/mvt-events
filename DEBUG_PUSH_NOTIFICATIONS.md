# 🔍 DEBUG - Notificações Push Não Chegam no iPhone

## ✅ O que já está funcionando:
1. ✅ Backend envia notificações com sucesso (HTTP 200 OK)
2. ✅ Expo API aceita e confirma recebimento (status: "ok")
3. ✅ Token registrado no banco: `ExponentPushToken[2nCfzTFPgiBICBsPPD60_s...]`
4. ✅ Sistema de notificações 3-níveis funcionando

## ❓ Possíveis causas da notificação não chegar:

### 1. **Permissões no iPhone**
- [ ] Verificar se as notificações estão ATIVADAS nas Configurações do iOS
- [ ] Ir em: **Configurações > Notificações > Expo Go** (ou seu app)
- [ ] Garantir que "Permitir Notificações" está ATIVADO
- [ ] Verificar se "Sons" e "Alertas" estão habilitados

### 2. **App em Foreground**
- [ ] Notificações podem não aparecer se o app estiver aberto (em primeiro plano)
- [ ] Testar com o app FECHADO ou em background
- [ ] No iOS, notificações só aparecem quando o app não está ativo

### 3. **Token Expo Go vs Standalone**
- [ ] Verificar se está usando **Expo Go** ou app **standalone/build próprio**
- [ ] Expo Go: Funciona para desenvolvimento, mas pode ter limitações
- [ ] Standalone: Requer configurações específicas de APNs

### 4. **Certificados APNs (Apple Push Notification service)**
- [ ] Para produção iOS, é necessário configurar certificados APNs
- [ ] Verificar se o projeto tem `ios.bundleIdentifier` configurado
- [ ] Confirmar se as credenciais APNs estão configuradas no Expo

### 5. **Token pode estar inválido ou expirado**
- [ ] Tokens Expo podem expirar se o app for desinstalado/reinstalado
- [ ] Forçar re-registro do token no app móvel

## 🔧 Testes para fazer no MOBILE:

### Teste 1: Verificar se o token é válido
Abra o app móvel e execute no console:
```javascript
import * as Notifications from 'expo-notifications';
const token = await Notifications.getExpoPushTokenAsync();
console.log('Token completo:', token);
```

### Teste 2: Testar notificação local
Adicione este código no app para testar se notificações funcionam localmente:
```javascript
await Notifications.scheduleNotificationAsync({
  content: {
    title: "Teste Local",
    body: "Se você vê isso, notificações funcionam!",
  },
  trigger: { seconds: 2 },
});
```

### Teste 3: Verificar configuração de notificações
```javascript
const settings = await Notifications.getPermissionsAsync();
console.log('Permissões:', settings);
// Deve retornar: { status: 'granted', ... }
```

## 🧪 Teste Manual via cURL (teste direto na API Expo):

```bash
# Obter o token completo do banco
TOKEN=$(docker exec mvt-events-db psql -U mvt -d mvt-events -t -c "SELECT token FROM user_push_tokens WHERE user_id = '6186c7af-2311-4756-bfc6-ce98bd31ed27' AND is_active = true;")

# Enviar notificação diretamente para Expo
curl -X POST https://exp.host/--/api/v2/push/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer q_E_RBFanVt2NfWO2isuYFwEZ1u3d2sxdiagNFRZ" \
  -d "{
    \"to\": \"$TOKEN\",
    \"title\": \"Teste Direto\",
    \"body\": \"Notificação enviada diretamente para Expo\",
    \"sound\": \"default\"
  }"
```

## 📱 Configuração necessária no app.json (mvt-mobile):

Verifique se o arquivo `app.json` tem:
```json
{
  "expo": {
    "notification": {
      "icon": "./assets/notification-icon.png",
      "color": "#000000",
      "androidMode": "default",
      "androidCollapsedTitle": "#{unread_notifications} novas notificações"
    },
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "com.mvt.mobile"
    },
    "android": {
      "package": "com.mvt.mobile",
      "googleServicesFile": "./google-services.json"
    }
  }
}
```

## 🚨 Ação Imediata:

1. **Feche o app completamente no iPhone** (deslizar para cima no App Switcher)
2. **Verifique as permissões de notificação** nas Configurações do iOS
3. **Reabra o app** e aguarde 10 segundos
4. **Crie uma nova delivery** e veja se a notificação chega

## 📊 Logs Recentes:

### Delivery 14 (última tentativa):
- ✅ Criada em: 2025-11-06 02:58:50
- ✅ Status API Expo: 200 OK
- ✅ ID Notificação: 019a57bf-10c6-7f93-adfd-fb0753b110e6
- ❌ Não recebida no iPhone

### Próximos passos:
1. Executar teste manual com cURL diretamente na API Expo
2. Verificar resposta detalhada (pode haver erros de delivery que não aparecem no status inicial)
3. Testar notificação local no app móvel
4. Verificar se o token está completo e válido
