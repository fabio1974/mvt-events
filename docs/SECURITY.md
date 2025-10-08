# 🔒 Segurança

## 🔑 Gestão de Secrets

### ❌ NUNCA Fazer

```properties
# NÃO commitar secrets hardcoded
stripe.api.key=sk_test_51QScv0Bxz4r3BQvg5jHn...
jwt.secret=my-super-secret-key
db.password=admin123
```

### ✅ SEMPRE Fazer

```properties
# Usar variáveis de ambiente
stripe.api.key=${STRIPE_API_KEY}
jwt.secret=${JWT_SECRET}
spring.datasource.password=${DB_PASSWORD}
```

---

## 🌍 Variáveis de Ambiente

### Desenvolvimento Local

1. Criar arquivo `.env` (não commitado):

```bash
STRIPE_API_KEY=sk_test_your_key
JWT_SECRET=your_jwt_secret
DB_PASSWORD=your_password
```

2. Carregar no IntelliJ:
   - Run > Edit Configurations
   - Environment Variables: carrega do `.env`

### Produção

Configurar no servidor/cloud:

**Heroku:**

```bash
heroku config:set STRIPE_API_KEY=sk_live_xxx
```

**AWS:**

```bash
aws ssm put-parameter --name STRIPE_API_KEY --value sk_live_xxx --type SecureString
```

**Docker:**

```yaml
environment:
  - STRIPE_API_KEY=${STRIPE_API_KEY}
```

---

## 🚨 Se um Secret Vazar

1. **Revogar imediatamente** no serviço original
2. **Gerar novo** secret
3. **Atualizar** em todos os ambientes
4. **Limpar histórico** do Git (se possível)
5. **Notificar equipe** de segurança

---

## 📋 Checklist de Segurança

- [ ] `.env` no `.gitignore`
- [ ] Secrets como variáveis de ambiente
- [ ] `.env.example` documentado
- [ ] Secrets diferentes para dev/prod
- [ ] Rotação regular de secrets
- [ ] Monitoramento de vazamentos (GitHub Secret Scanning)

---

## 🔗 Referências

- [GitHub Secret Scanning](https://docs.github.com/en/code-security/secret-scanning)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [Stripe Security Best Practices](https://stripe.com/docs/security/guide)
