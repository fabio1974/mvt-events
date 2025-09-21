# Render Environment Variables

This project uses environment variables configured directly in the Render dashboard for security.

## 🔐 Required Variables in Render Dashboard:

### Database Connection:

- `SPRING_DATASOURCE_URL` = `jdbc:postgresql://[HOST]:5432/[DATABASE]`
- `SPRING_DATASOURCE_USERNAME` = `[USERNAME]`
- `SPRING_DATASOURCE_PASSWORD` = `[PASSWORD]`

### How to configure:

1. Access the Render dashboard
2. Go to your `mvt-events-api` service
3. Click on "Environment"
4. Add the variables manually
5. Perform redeploy

## ⚠️ IMPORTANT:

- **NEVER** commit passwords to Git
- Use only the Render dashboard for sensitive variables
- The `render.yaml` does not contain credentials for security
