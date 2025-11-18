# Configuração de Token Expo para Push Notifications

## 🎯 Objetivo
Configurar token válido do Expo para enviar notificações push reais para dispositivos móveis.

## 📋 Opções de Token

### Opção 1: Token de Acesso do Expo (Recomendado para Produção)

1. **Criar conta no Expo** (se ainda não tiver):
   - Acesse: https://expo.dev/signup
   - Crie uma conta gratuita

2. **Obter Access Token**:
   - Faça login em: https://expo.dev
   - Vá em: Account Settings → Access Tokens
   - Clique em "Create Token"
   - Dê um nome (ex: "MVT Events Production")
   - Copie o token gerado (começa com `ExpoAccessToken[...]`)

3. **Configurar no projeto**:
   ```bash
   # Adicionar ao .env ou exportar:
   export EXPO_ACCESS_TOKEN="ExpoAccessToken[seu-token-aqui]"
   ```

   Ou editar `application.properties`:
   ```properties
   expo.access-token=ExpoAccessToken[seu-token-aqui]
   ```

### Opção 2: Usar Token de Push do App (Para Testes Locais)

1. **No app mobile** (React Native/Expo):
   ```javascript
   import * as Notifications from 'expo-notifications';
   
   async function registerForPushNotifications() {
     const { status } = await Notifications.requestPermissionsAsync();
     if (status !== 'granted') {
       alert('Permissão negada!');
       return;
     }
     
     const token = await Notifications.getExpoPushTokenAsync({
       projectId: 'seu-project-id-do-expo'
     });
     
     console.log('Expo Push Token:', token.data);
     // token.data será algo como: "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]"
     
     // Enviar este token para o backend via API
     await registerTokenWithBackend(token.data);
   }
   ```

2. **Registrar token no backend**:
   ```bash
   curl -X POST http://localhost:8080/api/push/register \
     -H "Authorization: Bearer SEU_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "token": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
       "deviceType": "MOBILE",
       "deviceName": "iPhone 13"
     }'
   ```

### Opção 3: Teste sem Token (Modo Simulação)

O sistema já está configurado para simular em modo desenvolvimento:
- Token atual: `development-test-token-for-local`
- Logs simulados aparecem com 🧪 e 📱
- Útil para testar lógica sem dispositivo real

## 🔧 Configuração Atual

### Token Atual (application.properties):
```properties
expo.access-token=ExpoAccessToken[development-test-token-for-local]
```

### Para Ativar Token Real:

1. **Via Variável de Ambiente** (Recomendado):
   ```bash
   export EXPO_ACCESS_TOKEN="ExpoAccessToken[SEU_TOKEN_AQUI]"
   ./gradlew bootRun
   ```

2. **Via application.properties**:
   ```properties
   expo.access-token=ExpoAccessToken[SEU_TOKEN_AQUI]
   ```

3. **Via Docker Compose**:
   ```yaml
   environment:
     - EXPO_ACCESS_TOKEN=ExpoAccessToken[SEU_TOKEN_AQUI]
   ```

## 📱 Testando Notificações

### 1. Registrar Token de Dispositivo

```bash
curl -X POST http://localhost:8080/api/push/register \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "ExponentPushToken[seu-token-do-dispositivo]",
    "deviceType": "MOBILE",
    "deviceName": "Meu iPhone"
  }'
```

### 2. Criar Delivery para Disparar Notificação

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "client": "CLIENT_UUID",
    "fromAddress": "Endereço de coleta",
    "fromLatitude": -3.8710,
    "fromLongitude": -40.9163,
    "toAddress": "Endereço de entrega",
    "toLatitude": -3.8669,
    "toLongitude": -40.9176,
    "totalAmount": 25.00,
    "itemDescription": "Teste de notificação"
  }'
```

### 3. Verificar Logs

```bash
tail -f app.log | grep -i "push\|notif\|expo"
```

Você deverá ver:
- ✅ `Notificação híbrida enviada para X dispositivos`
- ✅ `Notificações push enviadas com sucesso`

## 🔍 Troubleshooting

### Erro: "Token Expo não configurado"
```
WARN: Token Expo não configurado. Notificação não será enviada.
```
**Solução**: Configure o `expo.access-token` com um token válido.

### Erro: "ExpoPushError: DeviceNotRegistered"
```
ERROR: Expo API returned error: DeviceNotRegistered
```
**Solução**: O token do dispositivo expirou ou é inválido. Registre um novo token.

### Erro: "ExpoPushError: InvalidCredentials"
```
ERROR: Expo API returned error: InvalidCredentials
```
**Solução**: O `expo.access-token` está incorreto. Verifique o token.

## 📚 Documentação Oficial

- Expo Push Notifications: https://docs.expo.dev/push-notifications/overview/
- Getting Push Tokens: https://docs.expo.dev/push-notifications/push-notifications-setup/
- Expo Access Tokens: https://docs.expo.dev/accounts/personal-account/#personal-access-tokens

## 🚀 Próximos Passos

1. ✅ Obter token de acesso do Expo
2. ✅ Configurar token no backend
3. ✅ Obter tokens de dispositivos móveis
4. ✅ Registrar tokens via API
5. ✅ Criar delivery de teste
6. ✅ Verificar recebimento no dispositivo

## 💡 Dica Pro

Para desenvolvimento, você pode usar o **Expo Go** app:
1. Instale o app Expo Go no celular
2. Execute o projeto mobile com `expo start`
3. Escaneie o QR code
4. O token será gerado automaticamente
5. Use esse token para testes
