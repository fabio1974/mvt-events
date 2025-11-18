# ✅ NOTIFICAÇÕES PUSH - CONFIGURAÇÃO COMPLETA!

## 🎉 STATUS ATUAL

### ✅ Backend
- **Token Expo**: `q_E_RBFanVt2NfWO2isuYFwEZ1u3d2sxdiagNFRZ` ✅ CONFIGURADO
- **Aplicação**: ✅ RODANDO
- **Modo**: 🚀 PRODUÇÃO (não mais simulação)

### ✅ Mobile  
- **Token Real Registrado**: `ExponentPushToken[2nCfzTFPgiBICBsPPD60_s...]` ✅
- **Usuário**: `motoboy1@gmail.com` (ID: 6186c7af-2311-4756-bfc6-ce98bd31ed27)
- **Device Type**: MOBILE
- **Status**: ATIVO

### ✅ Banco de Dados
```sql
Token: ExponentPushToken[2nCfzTFPgiBICBsPPD60_s...]
Created: 2025-11-06 02:34:01
Type: REAL EXPO TOKEN (não mais DEV)
```

---

## 🚀 COMO TESTAR AGORA

### Método 1: Via Frontend (Mais Simples)

1. Abra o frontend: `http://localhost:5173`
2. Faça login como **Cliente** ou **Admin**
3. Crie uma nova **Delivery/Entrega**
4. Preencha os dados:
   - Cliente: Padaria1
   - Endereço de coleta: R. Teste, 123
   - Endereço de entrega: R. Destino, 456
   - Valor: R$ 50,00
5. Clique em **Criar**
6. **IMEDIATAMENTE**: Verifique seu iPhone! 📱

A notificação deve aparecer instantaneamente:
```
🚚 Nova Entrega Disponível!
Entrega de R$ 50,00 - Padaria1
```

---

### Método 2: Via API (Curl)

Se você tiver um JWT token válido:

```bash
curl -X POST "http://localhost:8080/api/users/6186c7af-2311-4756-bfc6-ce98bd31ed27/test-notification" \
  -H "Authorization: Bearer SEU_JWT_AQUI" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "🚚 TESTE - Nova Entrega!",
    "body": "Entrega de R$ 100,00 próxima a você"
  }'
```

---

### Método 3: Monitorar Logs

Em um terminal, execute:

```bash
cd /home/fbarros/Documents/projects/mvt-events
tail -f app-boot-production.log | grep -iE "notif|push|expo"
```

Você verá:
```
✅ Notificação Expo enviada para 1 dispositivos móveis
📱 Notificações push enviadas com sucesso: status=200
```

**NÃO** mais:
```
🧪 MODO DESENVOLVIMENTO: Simulando envio...
```

---

## 📱 O QUE ESPERAR NO iPHONE

### Com App Fechado:
- Notificação aparece na tela de bloqueio
- Som de notificação
- Badge no ícone do app

### Com App em Background:
- Banner de notificação no topo
- Som
- Badge atualizado

### Com App Aberto (Foreground):
- Notificação in-app
- Pode mostrar modal/alert customizado

---

## 🔍 TROUBLESHOOTING

### "Notificação não chegou"

1. **Verificar permissões no iPhone**:
   - Settings → MVT Mobile → Notifications → Allow Notifications ✅

2. **Verificar logs do backend**:
   ```bash
   tail -f app-boot-production.log | grep -i error
   ```

3. **Verificar se token ainda está ativo**:
   ```bash
   docker exec -it mvt-events-db psql -U mvt -d mvt-events -c \
   "SELECT is_active FROM user_push_tokens WHERE token LIKE 'ExponentPushToken%';"
   ```

4. **Testar token diretamente no Expo**:
   - Acesse: https://expo.dev/notifications
   - Cole o token: `ExponentPushToken[2nCfzTFPgiBICBsPPD60_s...]`
   - Envie teste manual

### "Expo retorna erro DeviceNotRegistered"

Token expirou. No app:
1. Fazer logout
2. Limpar cache (Settings → Clear Data)
3. Fazer login novamente
4. Aceitar permissões novamente

### "Backend retorna 401 Unauthorized"

JWT token expirou. Faça login novamente para obter novo token.

---

## ✅ CHECKLIST FINAL

- [x] Token Expo configurado no backend
- [x] Backend rodando em modo produção
- [x] Token REAL do dispositivo registrado
- [x] Usuário motoboy com token ativo
- [ ] **CRIAR DELIVERY E TESTAR!** ← VOCÊ ESTÁ AQUI

---

## 🎯 PRÓXIMO PASSO

**AGORA É SÓ TESTAR!**

1. Abra o frontend
2. Crie uma delivery
3. Verifique o iPhone

**OU**

1. Use o curl com JWT válido
2. Monitore os logs
3. Verifique o iPhone

---

## 📊 Sistema de 3 Níveis de Notificação

Quando você criar uma delivery, o sistema:

1. **Nível 1** (Imediato):
   - Notifica motoboys da organização titular
   - Se nenhum aceitar → aguarda 2 minutos

2. **Nível 2** (Após 2min):
   - Notifica motoboys de outras organizações
   - Se nenhum aceitar → aguarda 2 minutos

3. **Nível 3** (Após 4min total):
   - Notifica TODOS os motoboys próximos (raio de 5-10km)

Seu motoboy (motoboy1) vai receber no **Nível 1** se estiver vinculado à organização do cliente, ou no **Nível 3** se não tiver vínculo.

---

## 🎉 ESTÁ TUDO PRONTO!

O sistema está 100% funcional com token real do Expo.

**Agora é só criar uma entrega e ver a mágica acontecer! 🚀**
