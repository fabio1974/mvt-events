# 📱 Guia Completo: Ativar Notificações FCM no iPhone

## 🎯 Situação Atual

✅ **O que está funcionando:**
- Backend configurado e rodando
- Sistema de notificações implementado
- Delivery criada dispara o fluxo de notificação

⚠️ **O que NÃO está funcionando:**
- Backend está em **MODO SIMULAÇÃO**
- Token configurado: `development-test-token`
- Notificações **NÃO** são enviadas para o Expo/FCM real
- Apenas aparecem logs simulados (🧪 📱)

## 🔧 O Que Precisa Fazer

### Passo 1: Obter Token de Acesso do Expo

Você tem **2 opções**:

#### Opção A: Token de Conta Expo (Recomendado - Produção)

1. **Criar conta Expo** (se não tiver):
   ```
   https://expo.dev/signup
   ```

2. **Fazer login**:
   ```
   https://expo.dev/login
   ```

3. **Ir em Settings → Access Tokens**:
   ```
   https://expo.dev/accounts/[seu-username]/settings/access-tokens
   ```

4. **Criar novo token**:
   - Clique em "Create Token"
   - Nome: "MVT Events Production"
   - Copie o token (formato: `ExpoAccessToken[xxxxx...]`)

#### Opção B: Usar Expo CLI (Desenvolvimento)

```bash
# No projeto mobile
npm install -g expo-cli
expo login
expo whoami  # confirmar login
```

### Passo 2: Configurar Token no Backend

Execute o script helper:

```bash
cd /home/fbarros/Documents/projects/mvt-events
./setup-expo-token.sh
```

Ou manualmente:

#### Via Variável de Ambiente (Temporário):
```bash
export EXPO_ACCESS_TOKEN="ExpoAccessToken[SEU_TOKEN_AQUI]"
pkill -f 'gradle.*bootRun'
./gradlew bootRun
```

#### Via application.properties (Permanente):
```properties
# Editar: src/main/resources/application.properties
expo.access-token=ExpoAccessToken[SEU_TOKEN_AQUI]
```

Depois reiniciar:
```bash
pkill -f 'gradle.*bootRun'
./gradlew bootRun
```

### Passo 3: No App Mobile (iPhone) - Obter Push Token

No seu app React Native/Expo, adicione este código:

```javascript
import * as Notifications from 'expo-notifications';
import Constants from 'expo-constants';

// Configurar handler de notificações
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

// Função para registrar token
async function registerForPushNotificationsAsync() {
  let token;

  // Verificar se é dispositivo físico
  if (Constants.isDevice) {
    // Solicitar permissão
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    
    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    
    if (finalStatus !== 'granted') {
      alert('Falha ao obter permissão para notificações!');
      return;
    }
    
    // Obter token Expo Push
    token = await Notifications.getExpoPushTokenAsync({
      projectId: 'SEU_PROJECT_ID_DO_EAS', // Obtenha em app.json
    });
    
    console.log('📱 Expo Push Token:', token.data);
    
    // Registrar no backend
    await registerTokenWithBackend(token.data);
    
  } else {
    alert('É necessário um dispositivo físico para push notifications');
  }

  // Configurar canal Android (opcional)
  if (Platform.OS === 'android') {
    Notifications.setNotificationChannelAsync('default', {
      name: 'default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#FF231F7C',
    });
  }

  return token;
}

// Função para registrar token no backend
async function registerTokenWithBackend(expoPushToken) {
  try {
    const response = await fetch('http://SEU_IP:8080/api/push/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${yourJwtToken}`,
      },
      body: JSON.stringify({
        token: expoPushToken,
        deviceType: 'MOBILE',
        deviceName: 'iPhone de Teste',
      }),
    });

    const data = await response.json();
    console.log('✅ Token registrado no backend:', data);
    
  } catch (error) {
    console.error('❌ Erro ao registrar token:', error);
  }
}

// Chamar no useEffect ou ao fazer login
useEffect(() => {
  registerForPushNotificationsAsync();
}, []);
```

### Passo 4: Obter Project ID do Expo

No seu `app.json` do projeto mobile:

```json
{
  "expo": {
    "extra": {
      "eas": {
        "projectId": "ESTE_É_O_PROJECT_ID"
      }
    }
  }
}
```

Ou execute:
```bash
cd /path/to/mvt-mobile
npx expo config
```

### Passo 5: Testar!

1. **Abrir app no iPhone**
2. **Fazer login** (para obter JWT)
3. **Token será registrado automaticamente**
4. **Criar uma delivery:**

```bash
curl -X POST http://SEU_IP:8080/api/deliveries \
  -H "Authorization: Bearer SEU_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "client": "189c7d79-cb21-40c1-9b7c-006ebaa3289a",
    "fromAddress": "R. Teste, 123",
    "fromLatitude": -3.8710,
    "fromLongitude": -40.9163,
    "toAddress": "R. Destino, 456",
    "toLatitude": -3.8669,
    "toLongitude": -40.9176,
    "totalAmount": 35.00,
    "itemDescription": "Teste Push Notification",
    "recipientName": "Cliente Teste",
    "recipientPhone": "85999999999"
  }'
```

5. **Verificar no iPhone** - notificação deve aparecer! 🎉

## 🔍 Como Confirmar que Está Funcionando

### No Backend (Logs):

Você deve ver:
```
✅ Notificação Expo enviada para X dispositivos móveis
📱 Notificações push enviadas com sucesso: status=200
```

**NÃO** deve ver:
```
🧪 MODO DESENVOLVIMENTO: Simulando envio...
```

### No iPhone:

- Notificação aparece mesmo com app fechado
- Som de notificação
- Badge no ícone do app

## ⚠️ Troubleshooting

### Erro: "Token Expo não configurado"
**Solução**: Configure o `expo.access-token` válido

### Erro: "DeviceNotRegistered"
**Solução**: Token do dispositivo expirou. Registre novamente.

### Erro: "InvalidCredentials"
**Solução**: Token de acesso do Expo está errado.

### Notificação não chega no iPhone
1. Verificar se permissões estão ativadas
2. Verificar se token foi registrado corretamente no backend
3. Verificar logs do backend
4. Testar com Expo Push Notification Tool: https://expo.dev/notifications

## 📚 Referências

- Expo Push Notifications: https://docs.expo.dev/push-notifications/overview/
- Expo Push Tool (teste manual): https://expo.dev/notifications
- FCM Documentation: https://firebase.google.com/docs/cloud-messaging

## ✅ Checklist Final

- [ ] Token Expo configurado no backend
- [ ] Backend reiniciado com novo token
- [ ] App mobile obtém push token
- [ ] Token registrado no backend via API
- [ ] Delivery criada para teste
- [ ] Notificação recebida no iPhone

## 💡 Dica Extra

Para testar rapidamente se o token funciona, use o Expo Push Notification Tool:

1. Acesse: https://expo.dev/notifications
2. Cole o token do seu iPhone
3. Envie uma notificação de teste
4. Deve aparecer no iPhone imediatamente!

Se aparecer lá, significa que o problema está no backend. Se não aparecer, o problema está no app mobile ou nas permissões do iOS.
