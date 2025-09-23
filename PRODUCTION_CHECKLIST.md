# 🚨 PRODUCTION DEPLOYMENT CHECKLIST

## ❌ CRITICAL ISSUES - MUST FIX BEFORE DEPLOYMENT

### 1. 🔐 JWT Security Configuration

**RISK: HIGH** - Hardcoded JWT secrets expose authentication system

**Current Issue:**

```java
private static final String SECRET = "mvt-events-secret-key-for-jwt-authentication-very-long-secret-key-256-bits";
```

**Required Fix:**

```java
@Value("${jwt.secret:}")
private String jwtSecret;

@Value("${jwt.expiration:3600000}")
private long jwtExpiration;
```

**Render Environment Variables to Add:**

```
JWT_SECRET=your-super-secure-256-bit-secret-key-here
JWT_EXPIRATION=3600000
```

### 2. 🗄️ Database Migration V4 Deployment

**RISK: MEDIUM** - Data integrity during User+Athlete merger

**Current Status:** ✅ Migration script ready but needs careful deployment

**Required Actions:**

1. ✅ Backup production database before migration
2. ✅ Test migration on staging environment first
3. ✅ Monitor migration execution logs
4. ✅ Verify all registrations have user_id after migration

**Validation Query:**

```sql
-- After migration, this should return 0
SELECT COUNT(*) FROM registrations WHERE user_id IS NULL;
```

### 3. 🔧 Default Password Security

**RISK: HIGH** - Default password in migration for new users

**Current Issue:**

```sql
'$2a$10$2X8wNBhT5p5Nw8aKzE3/xOQh7L.jF9yV2sL8mJ6qN3pR4tY7uZ9wG', -- Default password "password123"
```

**Required Action:**

- Force password reset for all migrated users
- Add password change requirement on first login

### 4. 🛡️ Production Security Headers

**RISK: MEDIUM** - Missing security configurations

**Add to application-prod.properties:**

```properties
# Security Headers
server.servlet.session.secure=true
server.servlet.session.http-only=true
server.servlet.session.same-site=strict

# CORS Production Configuration
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://yourdomain.com}
```

## ⚠️ MONITORING REQUIREMENTS

### Health Check Endpoints

- ✅ `/actuator/health` - configured
- ✅ `/actuator/info` - configured
- ✅ `/actuator/metrics` - configured

### Database Monitoring

Monitor these after deployment:

```sql
-- Check migration status
SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;

-- Check user data integrity
SELECT role, COUNT(*) FROM users GROUP BY role;

-- Check registration integrity
SELECT COUNT(*) as total_registrations,
       COUNT(user_id) as with_user_id,
       COUNT(athlete_id) as with_athlete_id_old
FROM registrations;
```

## 🚀 DEPLOYMENT SEQUENCE

### Phase 1: Pre-deployment

1. ⚠️ **FIX JWT CONFIGURATION** (Critical)
2. ⚠️ **ADD ENVIRONMENT VARIABLES TO RENDER**
3. ✅ Backup production database
4. ✅ Test on staging environment

### Phase 2: Deployment

1. ✅ Deploy with V4 migration
2. ✅ Monitor migration execution logs
3. ✅ Verify health checks pass
4. ✅ Test authentication endpoints

### Phase 3: Post-deployment

1. ✅ Run data integrity checks
2. ✅ Monitor application logs
3. ✅ Test critical user flows
4. ✅ Verify all services are running

## 🔥 EMERGENCY ROLLBACK PLAN

If deployment fails:

1. **Revert to previous version** via Render dashboard
2. **Restore database backup** if migration fails
3. **Check Render service logs** for error details
4. **Monitor health endpoints** during rollback

## 📊 SUCCESS CRITERIA

Deployment is successful when:

- ✅ All health checks are green
- ✅ JWT authentication works with environment variables
- ✅ User login/registration functions correctly
- ✅ Database integrity checks pass
- ✅ Event creation and registration work
- ✅ No error logs in application
