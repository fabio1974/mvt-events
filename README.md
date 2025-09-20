# 🚀 Enterprise Spring Boot Starter Template

A **production-ready Spring Boot 3.5.6** template with **JWT authentication**, **PostgreSQL integration**, **Docker containerization**, and **automated CI/CD deployment**. Perfect for kickstarting new backend projects with enterprise-grade features.

## ✨ **Features**

### 🔐 **Security & Authentication**

- **JWT Authentication** with role-based authorization (`ROLE_USER`, `ROLE_ADMIN`)
- **Spring Security 6** integration with custom authentication filters
- **Password encryption** using BCrypt
- **Automatic admin role assignment** for usernames containing "admin"
- **Protected endpoints** with role-based access control

### 🗄️ **Database & Persistence**

- **PostgreSQL 16** integration
- **Flyway migrations** for database versioning (3 migration files included)
- **Spring Data JPA** with Hibernate
- **User entity** with JWT-compatible UserDetails implementation
- **Connection pooling** with HikariCP

### 🐳 **DevOps & Deployment**

- **Multi-stage Dockerfile** optimized for production
- **Docker Compose** setup for local development
- **GitHub Actions CI/CD** with automated GHCR publishing
- **Render.com deployment** configuration
- **Health checks** via Spring Boot Actuator

### 🛠️ **Development Experience**

- **Spring Boot DevTools** for hot reloading
- **Lombok** for reduced boilerplate code
- **Validation** with Spring Boot Starter Validation
- **Actuator endpoints** for monitoring (`/actuator/health`, `/actuator/metrics`)
- **Application profiles** (default, prod)

## 🏗️ **Architecture**

```
src/
├── main/java/com/mvt/mvt_events/
│   ├── common/          # JWT utilities and shared components
│   ├── controllers/     # REST API endpoints (Auth, Events)
│   ├── jpa/            # Entity classes
│   ├── repositories/   # Data access layer
│   ├── services/       # Business logic layer
│   └── events/         # Event management features
├── main/resources/
│   ├── db/migration/   # Flyway SQL migrations
│   ├── application.properties      # Default config
│   └── application-prod.properties # Production config
```

## 🚀 **Quick Start**

### 1. **Clone & Setup**

```bash
git clone <your-repo-url>
cd mvt-events
./gradlew build
```

### 2. **Local Development**

```bash
docker-compose up -d  # Start PostgreSQL
./gradlew bootRun     # Start application
```

### 3. **Authentication**

```bash
# Register new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Login & get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

## 🔧 **Tech Stack**

| Category           | Technology                            |
| ------------------ | ------------------------------------- |
| **Framework**      | Spring Boot 3.5.6, Spring Security 6  |
| **Language**       | Java 17 (Amazon Corretto)             |
| **Database**       | PostgreSQL 16, Flyway migrations      |
| **Authentication** | JWT (io.jsonwebtoken 0.12.6)          |
| **Build Tool**     | Gradle 8.11.1                         |
| **Container**      | Docker, Docker Compose                |
| **Deployment**     | Render.com, GitHub Container Registry |
| **CI/CD**          | GitHub Actions                        |

## 📡 **API Endpoints**

### 🔓 **Public Endpoints**

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User authentication
- `GET /actuator/health` - Health check

### 🔒 **Protected Endpoints**

- `GET /api/auth/me` - Current user info (requires JWT)
- `GET /actuator/metrics` - Application metrics (requires ADMIN role)

## 🌍 **Production Deployment**

### **Automated Deployment**

1. **Push to main branch** → Triggers GitHub Actions
2. **Docker image built** → Published to GHCR
3. **Render.com deploys** → Automatically from GHCR
4. **Health checks** → Ensure successful deployment

### **Environment Variables (Production)**

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
```

## 🎯 **Use Cases**

Perfect for:

- **REST API backends** with authentication
- **Microservices** with JWT integration
- **Enterprise applications** requiring security
- **SaaS platforms** with user management
- **Event management systems**
- **Any Spring Boot project** needing production-ready setup

## 📝 **Migration Files Included**

1. **V1\_\_init.sql** - Initial schema setup
2. **V2\_\_create_users_table.sql** - User management
3. **V3\_\_recreate_users_table_for_jwt.sql** - JWT-compatible user structure

---

**🎉 Ready to use! Just clone, configure your database, and start building your features!**

_This template was battle-tested in production and includes all the essential components for a modern Spring Boot application._
