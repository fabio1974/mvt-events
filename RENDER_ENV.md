# Render Environment Variables

Este projeto usa variáveis de ambiente configuradas diretamente no dashboard do Render para segurança.

## 🔐 Variáveis Obrigatórias no Render Dashboard:

### Database Connection:

- `SPRING_DATASOURCE_URL` = `jdbc:postgresql://[HOST]:5432/[DATABASE]`
- `SPRING_DATASOURCE_USERNAME` = `[USERNAME]`
- `SPRING_DATASOURCE_PASSWORD` = `[PASSWORD]`

### Como configurar:

1. Acesse o dashboard do Render
2. Vá para seu serviço `mvt-events-api`
3. Clique em "Environment"
4. Adicione as variáveis manualmente
5. Faça redeploy

## ⚠️ IMPORTANTE:

- **NUNCA** commite senhas no Git
- Use apenas o dashboard do Render para variáveis sensíveis
- O `render.yaml` não contém credenciais por segurança
