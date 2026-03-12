# API - Excluir Minha Conta

## Endpoint

```
DELETE /api/users/me
```

## Descrição

Soft-delete da conta do usuário logado. Anonimiza dados pessoais e bloqueia login permanentemente.

---

## Autenticação

**Bearer Token** (obrigatório)

```
Authorization: Bearer <token_jwt>
```

---

## Request

**Método:** `DELETE`  
**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Body:** Nenhum

---

## Response

### ✅ Sucesso (200 OK)

```json
{
  "message": "Conta excluída com sucesso"
}
```

### ❌ Erro - Conta de Demonstração (400 Bad Request)

```json
{
  "error": "Contas de demonstração não podem ser excluídas"
}
```

### ❌ Erro - Não Autenticado (401 Unauthorized)

```json
{
  "error": "Token inválido ou expirado"
}
```

---

## Comportamento

### O que acontece ao excluir:

1. **Dados anonimizados:**
   - Email → `deleted_<uuid>@removed.com`
   - Nome → `Usuário Removido`
   - CPF → CPF válido aleatório
   - Telefone → removido

2. **Acesso bloqueado:**
   - Login bloqueado permanentemente
   - Token atual invalidado
   - Impossível recuperar a conta

3. **Dados mantidos (auditoria):**
   - Histórico de entregas preservado
   - Pagamentos preservados
   - Campo `deleted_at` preenchido

### Proteções:

- ❌ **Contas demo** (`demo.*@zapi10.com`) não podem ser excluídas
- ✅ **Dados sensíveis** são anonimizados (LGPD)
- ✅ **Histórico transacional** preservado (auditoria)

---

## Implementação Mobile

### Exemplo React Native / JavaScript:

```javascript
const deleteMyAccount = async () => {
  try {
    const response = await fetch('https://api.mvt-events.com/api/users/me', {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${userToken}`,
        'Content-Type': 'application/json'
      }
    });

    const data = await response.json();

    if (response.ok) {
      // Sucesso: redirecionar para tela de login
      Alert.alert('Sucesso', data.message);
      // Limpar token local
      await AsyncStorage.removeItem('userToken');
      navigation.navigate('Login');
    } else {
      // Erro: exibir mensagem
      Alert.alert('Erro', data.error);
    }
  } catch (error) {
    Alert.alert('Erro', 'Não foi possível excluir a conta');
  }
};
```

---

## Fluxo Recomendado na UI

1. **Tela de Configurações** → Botão "Excluir Conta" (vermelho)
2. **Confirmação** → Modal/Dialog:
   ```
   "Tem certeza que deseja excluir sua conta?
   Esta ação é irreversível e seus dados serão anonimizados."
   
   [ Cancelar ]  [ Excluir Definitivamente ]
   ```
3. **Após exclusão:**
   - Limpar token local
   - Limpar dados do AsyncStorage/SecureStore
   - Redirecionar para tela de Login
   - Exibir mensagem de sucesso

---

## Notas Importantes

- ⚠️ **Ação irreversível** - usuário não pode recuperar a conta
- 🔒 **Token invalidado** - necessário novo login (impossível após exclusão)
- 📊 **Dados preservados** - histórico mantido para fins legais/auditoria
- 🛡️ **LGPD compliant** - dados pessoais anonimizados

---

## Testando

### cURL:
```bash
curl -X DELETE 'https://api.mvt-events.com/api/users/me' \
  -H 'Authorization: Bearer SEU_TOKEN_AQUI' \
  -H 'Content-Type: application/json'
```

### Resposta esperada:
```json
{
  "message": "Conta excluída com sucesso"
}
```
