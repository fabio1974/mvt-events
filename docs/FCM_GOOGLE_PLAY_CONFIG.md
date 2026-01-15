# Configuração Firebase Cloud Messaging (FCM) para Google Play

## ⚠️ IMPORTANTE
Este guia é necessário para que as notificações push funcionem no aplicativo **distribuído via Google Play Store** (APK/AAB standalone). O Expo Push Notifications continua funcionando automaticamente no **Expo Go** durante desenvolvimento.

## Por que é necessário?

- **Expo Go**: Usa os servidores do Expo automaticamente ✅
- **APK/AAB do Google Play**: Requer Firebase Cloud Messaging configurado ⚠️
- **Backend MVT Events**: Já está pronto, usa `https://exp.host/--/api/v2/push/send` ✅

O Expo Push Service automaticamente roteia notificações através do FCM quando detecta um token de standalone app.

---

## 📋 Checklist de Configuração

### 1. Criar Projeto Firebase

1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Clique em "Adicionar projeto"
3. Nome sugerido: `mvt-events-mobile` ou `mvt-courier-app`
4. Desabilite Google Analytics (opcional para notificações)
5. Clique em "Criar projeto"

### 2. Adicionar App Android ao Firebase

1. No projeto Firebase, clique no ícone Android ⚙️
2. Preencha os dados:
   - **Package name**: Deve ser o mesmo do `app.json` do projeto mobile
     - Exemplo: `com.mvtevents.courier` ou `com.mvt.mvtevents`
     - ⚠️ **DEVE SER EXATAMENTE IGUAL** ao `android.package` no app.json
   - **App nickname**: "MVT Courier" (opcional)
   - **SHA-1**: Pode deixar em branco por enquanto
3. Clique em "Registrar app"

### 3. Baixar google-services.json

1. No mesmo fluxo, clique em "Download google-services.json"
2. Salve o arquivo na raiz do projeto mobile
3. ⚠️ **NÃO commitar no git** - adicione ao `.gitignore`:
   ```
   google-services.json
   GoogleService-Info.plist
   ```

### 4. Configurar app.json no Projeto Mobile

Edite o `app.json` e adicione a configuração do FCM:

```json
{
  "expo": {
    "name": "MVT Courier",
    "slug": "mvt-courier",
    "android": {
      "package": "com.mvtevents.courier",
      "googleServicesFile": "./google-services.json",
      "permissions": [
        "ACCESS_FINE_LOCATION",
        "ACCESS_COARSE_LOCATION",
        "NOTIFICATIONS"
      ]
    },
    "notification": {
      "icon": "./assets/notification-icon.png",
      "color": "#FF6B00",
      "androidMode": "default",
      "androidCollapsedTitle": "#{unread_notifications} novas entregas"
    }
  }
}
```

### 5. Instalar Dependências (se necessário)

Se o projeto mobile ainda não tiver, instale:

```bash
npx expo install expo-notifications
npx expo install expo-device
npx expo install @react-native-firebase/app
npx expo install @react-native-firebase/messaging
```

### 6. Obter Server Key do Firebase (para Backend)

⚠️ **NOTA**: O backend MVT Events **já está configurado** e usa Expo Push Service. Esta chave é backup/referência.

1. No Firebase Console, vá em **Project Settings** (⚙️)
2. Aba **Cloud Messaging**
3. Procure por "Server key" na seção "Project credentials"
4. Copie a chave (começa com `AAAA...`)

### 7. Build e Teste

#### Build do APK/AAB:

```bash
# Para APK (teste local)
eas build --platform android --profile preview

# Para AAB (Google Play)
eas build --platform android --profile production
```

#### Instalar e Testar:

1. Instale o APK no dispositivo físico
2. Abra o app e faça login
3. O token deve ser registrado automaticamente
4. Verifique no banco de dados:
   ```sql
   SELECT id, user_id, token, is_active, created_at 
   FROM user_push_tokens 
   WHERE is_active = true 
   ORDER BY created_at DESC 
   LIMIT 10;
   ```
5. Crie uma entrega para testar notificação

---

## 🔍 Verificação de Configuração

### ✅ Checklist Completo

- [ ] Projeto Firebase criado
- [ ] App Android registrado no Firebase
- [ ] `google-services.json` baixado e colocado na raiz do projeto mobile
- [ ] `google-services.json` adicionado ao `.gitignore`
- [ ] `app.json` configurado com `googleServicesFile`
- [ ] Package name no Firebase == package no `app.json`
- [ ] Build EAS executado com sucesso
- [ ] APK/AAB instalado em dispositivo físico
- [ ] Token registrado no banco após login
- [ ] Notificação recebida em teste real

### 🧪 Teste de Notificação Manual

Após instalação, teste com curl no backend:

```bash
# Obter token ativo do usuário
psql -h localhost -p 5435 -U mvt -d mvt-events -c \
  "SELECT token FROM user_push_tokens WHERE user_id = [ID_COURIER] AND is_active = true;"

# Criar entrega de teste via API
curl -X POST https://mvt-events-api.onrender.com/api/deliveries \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [TOKEN_JWT]" \
  -d '{
    "clientId": [ID_CLIENTE],
    "pickupAddress": "Rua Teste, 123",
    "pickupLatitude": -23.550520,
    "pickupLongitude": -46.633308,
    "deliveryAddress": "Av Paulista, 1000",
    "deliveryLatitude": -23.561684,
    "deliveryLongitude": -46.655981,
    "totalAmount": 25.00
  }'
```

---

## 🐛 Troubleshooting

### Notificação não chega no dispositivo

**1. Verificar se token foi registrado:**
```sql
SELECT * FROM user_push_tokens 
WHERE user_id = [ID] AND is_active = true;
```

**2. Verificar formato do token:**
- Token do Expo Go: `ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]`
- Token standalone: pode ser diferente mas ainda começa com `ExponentPushToken`

**3. Verificar logs do backend:**
```bash
# No Render.com, verificar logs da aplicação
# Procurar por: "Sending push notification to token: ExponentPushToken[...]"
```

**4. Testar manualmente com Expo Push Tool:**
- Acesse: https://expo.dev/notifications
- Cole o token do usuário
- Envie uma notificação de teste
- Se funcionar → problema no backend
- Se não funcionar → problema na configuração FCM

### Build falha com erro do google-services.json

**Erro comum:**
```
google-services.json not found
```

**Solução:**
1. Verifique se arquivo está na **raiz do projeto mobile**
2. Verifique se `app.json` tem caminho correto: `"googleServicesFile": "./google-services.json"`
3. Execute `eas build:configure` novamente

### Package name não coincide

**Erro:**
```
Package name mismatch
```

**Solução:**
1. Verifique package name no Firebase Console
2. Verifique `android.package` no `app.json`
3. **Devem ser idênticos**
4. Se mudou, delete o app no Firebase e registre novamente

---

## 📱 Diferenças: Expo Go vs Standalone

| Aspecto | Expo Go | Standalone (Play Store) |
|---------|---------|-------------------------|
| FCM Config | ❌ Não precisa | ✅ **Obrigatório** |
| google-services.json | ❌ Não precisa | ✅ **Obrigatório** |
| Token format | ExponentPushToken | ExponentPushToken |
| Backend code | ✅ Mesmo código | ✅ Mesmo código |
| Expo Push API | ✅ Funciona | ✅ Funciona (via FCM) |
| Teste local | ✅ Imediato | Requer build (~10min) |

---

## 📞 Contato Backend

**Backend já configurado e funcionando:**
- API: `https://mvt-events-api.onrender.com/api`
- Endpoint notificações: `POST /deliveries` (automático)
- Token registration: `POST /auth/register-push-token`

**Tabela tokens:**
```sql
TABLE user_push_tokens (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id),
  token VARCHAR(255) UNIQUE,
  device_type VARCHAR(50),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

**Constraint único (V38):**
- Somente 1 token ativo por vez (por token)
- Ao registrar token existente → desativa do usuário anterior
- Suporta troca de usuário no mesmo dispositivo ✅

---

## ✅ Status Atual

- ✅ Backend configurado com Expo Push API
- ✅ Algoritmo de notificação em 3 níveis funcionando
- ✅ Tokens únicos por usuário (V38 migration)
- ✅ Filtro de motoboys livres
- ✅ Envio sequencial com 5s de delay
- ✅ Ordenação por proximidade
- ⚠️ **Mobile precisa**: google-services.json + app.json configurado

---

**Última atualização:** 9 de janeiro de 2026  
**Responsável backend:** Sistema MVT Events  
**Documentação Expo:** https://docs.expo.dev/push-notifications/fcm-credentials/
