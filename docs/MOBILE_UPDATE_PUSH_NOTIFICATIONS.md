# 📱 Atualização - Sistema de Notificações Push

**Data:** 18/02/2026  
**Versão Backend:** v1.2.0  
**Impacto:** Correções importantes no sistema de push notifications

---

## 🎯 Resumo das Mudanças

Foram implementadas **2 correções críticas** no sistema de notificações push:

1. ✅ **Correção do erro 500** ao registrar token push duplicado
2. ✅ **Melhoria no observability** - logs agora mostram corretamente se notificação foi enviada

---

## 🔧 Mudança 1: Erro 500 ao Registrar Token Push - CORRIGIDO

### ❌ Problema Anterior

Quando o app tentava registrar o mesmo token push novamente, recebia erro 500:

```json
POST /api/users/push-token
Status: 500

{
  "success": false,
  "message": "Erro interno do servidor",
  "data": null
}
```

**Causa:** Race condition ao tentar inserir token que já existia no banco.

### ✅ Comportamento Atual

O endpoint agora trata duplicatas corretamente:

```json
POST /api/users/push-token
Status: 200 OK

{
  "success": true,
  "message": "Token já está registrado",
  "data": null
}
```

### 📋 O Que o Mobile Precisa Fazer

**NADA! 🎉** A correção é transparente para o app.

- ✅ Continuar chamando `POST /api/users/push-token` após login
- ✅ Mesmo payload de sempre
- ✅ Agora funciona mesmo se token já existir

**Exemplo de payload que estava falhando (agora funciona):**

```typescript
// Request
POST /api/users/push-token
Headers: {
  "Authorization": "Bearer <JWT>",
  "Content-Type": "application/json"
}
Body: {
  "token": "ExponentPushToken[5k1OIsH7bWXzsRyhqP49V4]",
  "platform": "android",
  "deviceType": "mobile"
}

// Response - ANTES (erro 500)
{
  "success": false,
  "message": "Erro interno do servidor"
}

// Response - AGORA (sucesso 200)
{
  "success": true,
  "message": "Token já está registrado"
}
```

---

## 📊 Mudança 2: Notificações de Falha de Pagamento

### ✅ O Que Foi Melhorado

Backend agora envia notificação push **automaticamente** quando pagamento falha por:

- ❌ Cartão recusado
- ❌ Saldo insuficiente  
- ❌ Antifraude reprovou
- ❌ Dados do cartão inválidos
- ❌ Qualquer outro erro de pagamento

### 📱 Payload da Notificação

O app receberá notificação push com este formato:

```typescript
// Notificação Expo Push
{
  title: "❌ Pagamento não aprovado",
  body: "Pagamento de R$ 15.50 não foi aprovado. Cartão recusado pela operadora. Por favor, escolha outro método de pagamento.",
  data: {
    type: "payment_failed",
    deliveryId: "uuid-da-entrega",
    paymentId: "uuid-do-pagamento",
    amount: "15.50",
    failureReason: "Cartão recusado pela operadora"
  }
}
```

### 📋 O Que o Mobile Precisa Implementar

**Adicionar handler para tipo `payment_failed`:**

```typescript
// Exemplo React Native
import * as Notifications from 'expo-notifications';

Notifications.addNotificationReceivedListener((notification) => {
  const { data } = notification.request.content;
  
  if (data.type === 'payment_failed') {
    // 1. Mostrar alert ou toast ao usuário
    Alert.alert(
      notification.request.content.title,
      notification.request.content.body,
      [
        { text: 'Cancelar', style: 'cancel' },
        { 
          text: 'Escolher Outro Cartão', 
          onPress: () => {
            // Navegar para tela de pagamento
            navigation.navigate('PaymentMethods', {
              deliveryId: data.deliveryId
            });
          }
        }
      ]
    );
    
    // 2. Atualizar lista de entregas (refetch)
    refetchDeliveries();
  }
});
```

**Possíveis valores de `failureReason`:**

- `"Cartão recusado pela operadora"`
- `"Pagamento recusado por suspeita de fraude"`
- `"Dados do cartão inválidos"`
- `"Saldo insuficiente"`
- `"Transação não autorizada"`
- `"Erro ao processar pagamento"`

---

## 🔍 Por Que Usuário Não Recebe Notificação?

Se um usuário **NÃO** receber notificação de falha de pagamento, pode ser:

### ✅ Verificar Status de Ativação

Use o endpoint de status para diagnosticar:

```typescript
GET /api/users/me/activation-status

// Resposta indica se tem token push:
{
  "enabled": false,
  "role": "CUSTOMER",
  "missing": ["paymentMethod"],  // ← Falta cadastrar cartão
  "messages": {
    "paymentMethod": "Cadastre um meio de pagamento"
  },
  "suggested": []
}
```

### 🔧 Verificar Token Push Registrado

```typescript
// Após login, sempre chamar:
async function registerPushToken() {
  const token = await Notifications.getExpoPushTokenAsync({
    projectId: 'your-expo-project-id'
  });
  
  const response = await fetch('/api/users/push-token', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      token: token.data,  // Ex: "ExponentPushToken[...]"
      platform: Platform.OS,  // "android" ou "ios"
      deviceType: "mobile"
    })
  });
  
  const result = await response.json();
  
  if (!result.success) {
    console.error('❌ Falha ao registrar token push:', result.message);
  } else {
    console.log('✅ Token push registrado:', result.message);
  }
}
```

---

## 🧪 Como Testar

### Teste 1: Registro de Token Push

```bash
# 1. Fazer login no app
# 2. App deve chamar automaticamente POST /api/users/push-token
# 3. Verificar logs do app - deve ver "✅ Token push registrado"
# 4. Fechar e reabrir app
# 5. App chama novamente POST /api/users/push-token
# 6. Deve ver "✅ Token já está registrado" (não mais erro 500)
```

### Teste 2: Notificação de Falha de Pagamento

```bash
# 1. Criar entrega que requer pagamento
# 2. Usar cartão de teste que falha (Pagar.me sandbox):
#    Número: 4000 0000 0000 0002 (sempre recusa)
# 3. Aguardar ~5 segundos
# 4. App deve receber notificação push:
#    "❌ Pagamento não aprovado"
# 5. Ao clicar, deve navegar para escolher outro método
```

---

## 📊 Endpoints Relevantes

### 1. Registrar Token Push

```http
POST /api/users/push-token
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "token": "ExponentPushToken[...]",
  "platform": "android|ios",
  "deviceType": "mobile|web"
}

Status: 200 OK
{
  "success": true,
  "message": "Token registrado com sucesso" | "Token já está registrado",
  "data": "uuid-do-registro" | null
}
```

### 2. Remover Token Push

```http
DELETE /api/users/push-token
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "token": "ExponentPushToken[...]"
}

Status: 200 OK
{
  "success": true,
  "message": "Token removido com sucesso"
}
```

### 3. Verificar Status de Ativação

```http
GET /api/users/me/activation-status
Authorization: Bearer <JWT>

Status: 200 OK
{
  "enabled": true,
  "role": "CUSTOMER",
  "missing": [],
  "messages": {},
  "suggested": []
}
```

---

## � Best Practices - Token Refresh

### ⏰ Quando Atualizar o Push Token

Expo Push Tokens **não expiram por tempo**, mas podem se tornar inválidos. Recomendamos atualizar o token em múltiplos momentos:

#### ✅ Momentos Obrigatórios

```typescript
// 1. Após Login/Signup
async function onLoginSuccess(authToken: string) {
  await registerPushToken(authToken);
}

// 2. Após reinstalação do app ou limpar dados
// (detectado automaticamente quando token muda)
useEffect(() => {
  const checkTokenChange = async () => {
    const currentToken = await Notifications.getExpoPushTokenAsync();
    const savedToken = await AsyncStorage.getItem('lastPushToken');
    
    if (currentToken.data !== savedToken) {
      await registerPushToken(authToken);
      await AsyncStorage.setItem('lastPushToken', currentToken.data);
    }
  };
  
  checkTokenChange();
}, []);
```

#### 💡 Momentos Recomendados

```typescript
// 3. Quando app volta do background (após > 1 hora)
import { AppState } from 'react-native';

useEffect(() => {
  let lastActiveTime = Date.now();
  
  const subscription = AppState.addEventListener('change', async (nextAppState) => {
    if (nextAppState === 'active') {
      const inactiveTime = Date.now() - lastActiveTime;
      const oneHour = 60 * 60 * 1000;
      
      // Se ficou inativo por mais de 1 hora, atualizar token
      if (inactiveTime > oneHour) {
        await registerPushToken(authToken);
      }
    } else {
      lastActiveTime = Date.now();
    }
  });
  
  return () => subscription.remove();
}, []);

// 4. Periodicamente (1x por semana em background task)
import * as BackgroundFetch from 'expo-background-fetch';
import * as TaskManager from 'expo-task-manager';

const BACKGROUND_TOKEN_REFRESH = 'background-token-refresh';

TaskManager.defineTask(BACKGROUND_TOKEN_REFRESH, async () => {
  try {
    await registerPushToken(await getAuthToken());
    return BackgroundFetch.BackgroundFetchResult.NewData;
  } catch (error) {
    return BackgroundFetch.BackgroundFetchResult.Failed;
  }
});

// Registrar tarefa para rodar 1x por semana
await BackgroundFetch.registerTaskAsync(BACKGROUND_TOKEN_REFRESH, {
  minimumInterval: 60 * 60 * 24 * 7, // 7 dias
  stopOnTerminate: false,
  startOnBoot: true,
});
```

### ⚠️ Quando Token Fica Inválido

Push tokens podem se tornar inválidos quando:

| Cenário | Como Detectar | Ação Recomendada |
|---------|---------------|------------------|
| 📱 App desinstalado/reinstalado | Token mudou ao obter novamente | Registrar novo token |
| 🗑️ Dados/cache limpos | Token mudou | Registrar novo token |
| 📲 Troca de dispositivo | Login em novo device | Registrar automaticamente |
| ⚙️ Configurações mudaram | Expo retorna erro | Obter e registrar novo token |
| 🔄 Expo revogou token (raro) | Push retorna `DeviceNotRegistered` | Obter e registrar novo token |

### 🛡️ Implementação Completa com Error Handling

```typescript
import * as Notifications from 'expo-notifications';
import AsyncStorage from '@react-native-async-storage/async-storage';

const PUSH_TOKEN_KEY = 'expo_push_token';
const LAST_REFRESH_KEY = 'push_token_last_refresh';

/**
 * Registra push token com retry e error handling
 */
async function registerPushToken(authToken: string): Promise<boolean> {
  try {
    // 1. Verificar permissões
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    
    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    
    if (finalStatus !== 'granted') {
      console.warn('⚠️ Permissões de notificação não concedidas');
      return false;
    }
    
    // 2. Obter token do Expo
    const tokenData = await Notifications.getExpoPushTokenAsync({
      projectId: 'your-expo-project-id',
    });
    
    // 3. Verificar se mudou
    const savedToken = await AsyncStorage.getItem(PUSH_TOKEN_KEY);
    if (tokenData.data === savedToken) {
      console.log('✅ Token não mudou, skip');
      return true;
    }
    
    // 4. Enviar ao backend com retry
    const maxRetries = 3;
    let attempt = 0;
    
    while (attempt < maxRetries) {
      try {
        const response = await fetch('/api/users/push-token', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            token: tokenData.data,
            platform: Platform.OS,
            deviceType: 'mobile',
          }),
        });
        
        const result = await response.json();
        
        if (result.success) {
          // 5. Salvar localmente
          await AsyncStorage.setItem(PUSH_TOKEN_KEY, tokenData.data);
          await AsyncStorage.setItem(LAST_REFRESH_KEY, Date.now().toString());
          
          console.log('✅ Push token registrado:', result.message);
          return true;
        } else {
          console.error('❌ Falha ao registrar token:', result.message);
          attempt++;
        }
      } catch (error) {
        console.error(`❌ Erro ao registrar token (tentativa ${attempt + 1}):`, error);
        attempt++;
        
        if (attempt < maxRetries) {
          // Esperar antes de retry (exponential backoff)
          await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, attempt)));
        }
      }
    }
    
    return false;
    
  } catch (error) {
    console.error('❌ Erro crítico ao registrar push token:', error);
    return false;
  }
}

/**
 * Verificar se token precisa ser atualizado
 */
async function shouldRefreshToken(): Promise<boolean> {
  try {
    const lastRefresh = await AsyncStorage.getItem(LAST_REFRESH_KEY);
    
    if (!lastRefresh) {
      return true; // Nunca foi registrado
    }
    
    const lastRefreshTime = parseInt(lastRefresh);
    const sevenDaysAgo = Date.now() - (7 * 24 * 60 * 60 * 1000);
    
    return lastRefreshTime < sevenDaysAgo; // Atualizar se > 7 dias
    
  } catch (error) {
    return true; // Em caso de erro, atualizar
  }
}

/**
 * Handler para erro DeviceNotRegistered do Expo
 */
Notifications.addNotificationResponseReceivedListener(async (response) => {
  // Se receber erro de token inválido, reregistrar
  const errorData = response.notification.request.content.data;
  
  if (errorData?.error === 'DeviceNotRegistered') {
    console.warn('⚠️ Token inválido detectado, reregistrando...');
    
    // Limpar token antigo
    await AsyncStorage.removeItem(PUSH_TOKEN_KEY);
    
    // Obter e registrar novo token
    const authToken = await getAuthToken(); // Sua função para obter JWT
    await registerPushToken(authToken);
  }
});
```

### 📊 Backend: Limpeza Automática de Tokens Antigos

O backend agora possui **cleanup automático** de tokens inativos:

- 🗑️ Tokens não atualizados há **> 90 dias** são automaticamente desativados
- 🔄 Roda diariamente às 3h da manhã
- 📝 Logs indicam quantos tokens foram limpos

**Você não precisa fazer nada** - o backend cuida disso automaticamente.

### ✅ Checklist de Implementação

- [ ] Registrar token após login/signup
- [ ] Detectar mudança de token ao abrir app
- [ ] Atualizar token quando app volta do background (> 1 hora)
- [ ] Implementar retry com exponential backoff
- [ ] Salvar token localmente para detectar mudanças
- [ ] Handler para erro `DeviceNotRegistered`
- [ ] Background task para refresh semanal (opcional)
- [ ] Logs de debug para troubleshooting

---

## �🐛 Troubleshooting

### Problema: "Não recebo notificações de pagamento falhado"

**Checklist:**

1. ✅ Token push foi registrado após login?
   - Verificar chamada `POST /api/users/push-token`
   - Deve retornar `success: true`

2. ✅ Permissões de notificação concedidas?
   ```typescript
   const { status } = await Notifications.getPermissionsAsync();
   if (status !== 'granted') {
     await Notifications.requestPermissionsAsync();
   }
   ```

3. ✅ App está em foreground ou background?
   - Notificações funcionam nos dois casos
   - Em foreground, usar listener para capturar

4. ✅ Token Expo é válido?
   ```typescript
   const token = await Notifications.getExpoPushTokenAsync();
   console.log('Token:', token.data); // Deve começar com "ExponentPushToken["
   ```

### Problema: "Erro 500 ao registrar token"

**Se ainda ocorrer erro 500:**

1. Verificar formato do payload
2. Verificar token JWT válido no header
3. Verificar logs do backend em `nohup.out`
4. Chamar novamente - pode ter sido erro transitório

---

## 📚 Documentação Adicional

- [API_ACTIVATION_STATUS_ENDPOINT.md](./API_ACTIVATION_STATUS_ENDPOINT.md) - Status de ativação do usuário
- [EXPO_TOKEN_MIGRATION.md](./EXPO_TOKEN_MIGRATION.md) - Migração de tokens Expo
- [NOTIFICATION_LOGGING_FIX.md](./NOTIFICATION_LOGGING_FIX.md) - Detalhes técnicos do fix
- [FRONTEND_PAYMENT_DOCS.md](./FRONTEND_PAYMENT_DOCS.md) - Fluxo de pagamentos

---

## ✅ Checklist para Mobile Team

- [ ] Atualizar handler de notificações para tipo `payment_failed`
- [ ] Implementar navegação para tela de métodos de pagamento ao clicar na notificação
- [ ] Testar fluxo completo: pagamento falha → recebe notificação → escolhe outro cartão
- [ ] Testar registro de token push em login
- [ ] Testar registro de token push ao reabrir app (não deve dar erro 500)
- [ ] Verificar logs - não deve mais aparecer "Erro interno do servidor" ao registrar token
- [ ] Atualizar UI para exibir mensagem amigável em caso de falha de pagamento

---

## 🚀 Deploy

**Backend:**
- ✅ Versão: v1.2.0
- ✅ Deploy: 18/02/2026 às 16h09min
- ✅ PID: 20633
- ✅ Porta: 8080
- ✅ Status: UP

**Breaking Changes:** ❌ Nenhuma  
**Requer Update App:** ❌ Não obrigatório (mas recomendado para melhor UX)

---

## 📞 Suporte

Dúvidas sobre as mudanças:

- 💬 Slack: #mobile-backend-integration
- 📧 Email: dev@zapi10.com
- 🐛 Issues: Criar ticket no Jira

**Logs Backend:** `/mvt-events/nohup.out`  
**Health Check:** `GET http://localhost:8080/actuator/health`

---

**Última atualização:** 18/02/2026 às 16:15
