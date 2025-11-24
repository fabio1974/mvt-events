# 🎫 MVT Events / Zapi10 - Plataforma de Entregas

Sistema completo de gestão de entregas com sistema inteligente de notificações em 3 níveis, multi-tenancy e integração de pagamentos.

---

## 📚 Documentação

> **Toda a documentação técnica está em [`/docs`](./docs/README.md)**

### 🚀 Quick Links

#### Documentação para Gerentes
- [📊 **Apresentação Gerencial Zapi10**](./APRESENTACAO_GERENCIAL_ZAPI10.md) ⭐ **NOVO!**
  - Sistema de Grupos e Vínculos
  - Algoritmo de Notificações em 3 Níveis
  - Divisão de Comissões (85% / 15%)
  - Impacto Financeiro e ROI
  - Estratégias de Negócio
  
- [📖 Apresentação Técnica Completa](./APRESENTACAO_SISTEMA_GRUPOS_E_NOTIFICACOES.md)

#### Documentação Técnica
- [📖 Documentação Completa](./docs/README.md)
- [🏗️ Arquitetura de Metadata](./docs/architecture/METADATA_ARCHITECTURE.md)
- [🔍 Guia de Filtros API](./docs/api/FILTERS_GUIDE.md)
- [🔗 Entity Filters](./docs/features/ENTITY_FILTERS.md)
- [📊 Status do Projeto](./docs/implementation/STATUS.md)
- [🧪 Testing Documentation](./docs/TESTING.md)
- [🔒 Segurança](./docs/SECURITY.md)

---

## 🚀 Getting Started

### Pré-requisitos

- Java 17+
- PostgreSQL 13+
- Gradle 7+

### Configuração

1. **Clone o repositório:**

   ```bash
   git clone https://github.com/fabio1974/mvt-events.git
   cd mvt-events
   ```

2. **Configure as variáveis de ambiente:**

   ```bash
   cp .env.example .env
   # Edite .env com suas credenciais
   ```

3. **Inicie o banco de dados:**

   ```bash
   docker-compose up -d postgres
   ```

4. **Execute a aplicação:**

   ```bash
   ./gradlew bootRun
   ```

5. **Acesse:**
   - API: http://localhost:8080
   - Swagger: http://localhost:8080/swagger-ui.html
   - Actuator: http://localhost:8080/actuator/health

---

## 🏗️ Arquitetura

### Stack Tecnológica

- **Backend:** Spring Boot 3.x
- **Database:** PostgreSQL
- **ORM:** JPA/Hibernate
- **Security:** Spring Security + JWT
- **Payments:** Stripe, MercadoPago, PayPal
- **Migration:** Flyway

### Principais Features

✅ **Multi-tenancy** por organização  
✅ **Filtros dinâmicos** com JPA Specifications  
✅ **Entity Filters** autodiscovery  
✅ **Metadata API** para frontend dinâmico  
✅ **Pagamentos integrados** (Stripe, MP, PayPal)  
✅ **Webhook handlers** para pagamentos

---

## 📦 Estrutura do Projeto

```
mvt-events/
├── docs/                      # 📚 Documentação completa
│   ├── architecture/          # Arquitetura e padrões
│   ├── api/                   # Documentação de API
│   ├── features/              # Features específicas
│   └── implementation/        # Status e changelog
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mvt/mvt_events/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── jpa/       # Entidades JPA
│   │   │       ├── metadata/  # Sistema de metadata
│   │   │       ├── payment/   # Integrações de pagamento
│   │   │       └── security/  # Autenticação e autorização
│   │   └── resources/
│   │       ├── db/migration/  # Flyway migrations
│   │       └── application.properties
│   └── test/
├── .env.example               # Template de variáveis de ambiente
├── docker-compose.yml         # Serviços Docker
└── README.md                  # Este arquivo
```

---

## 🔐 Segurança

### Variáveis de Ambiente

**Nunca** commite secrets no código. Use variáveis de ambiente:

```bash
# .env (não commitado)
STRIPE_SECRET_KEY=sk_test_xxx
JWT_SECRET=your-secret-here
DB_PASSWORD=secure-password
```

📖 **Leia mais:** [Guia de Segurança](./docs/SECURITY.md)

---

## 🧪 Testes

```bash
# Testes unitários
./gradlew test

# Testes de integração
./gradlew integrationTest

# Coverage
./gradlew jacocoTestReport
```

---

## 🚢 Deploy

### Docker

```bash
# Build
docker build -t mvt-events .

# Run
docker-compose up -d
```

### Heroku

```bash
heroku create mvt-events-prod
heroku addons:create heroku-postgresql:hobby-dev
heroku config:set STRIPE_SECRET_KEY=sk_live_xxx
git push heroku main
```

---

## 📊 API Endpoints

### Principais Recursos

| Recurso       | Endpoint             | Descrição                   |
| ------------- | -------------------- | --------------------------- |
| Events        | `/api/events`        | CRUD de eventos             |
| Registrations | `/api/registrations` | Inscrições em eventos       |
| Users         | `/api/users`         | Gestão de usuários          |
| Payments      | `/api/payments`      | Processamento de pagamentos |
| Metadata      | `/api/metadata`      | Metadata para frontend      |

📖 **Documentação completa:** [Guia de Filtros](./docs/api/FILTERS_GUIDE.md)

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

---

## 📝 License

Este projeto está sob a licença MIT. Veja [LICENSE](LICENSE) para mais informações.

---

## 👥 Time

- **Backend Lead:** [Nome]
- **Frontend Lead:** [Nome]
- **DevOps:** [Nome]

---

## 📞 Suporte

- **Issues:** [GitHub Issues](https://github.com/fabio1974/mvt-events/issues)
- **Docs:** [Documentação Completa](./docs/README.md)
- **Email:** support@mvt-events.com

---

## 🔗 Links Úteis

- [Documentação](./docs/README.md)
- [Changelog](./docs/implementation/CHANGELOG.md)
- [Status do Projeto](./docs/implementation/STATUS.md)
- [Segurança](./docs/SECURITY.md)

```http
GET    /api/athletes                # List all athletes
GET    /api/athletes/{id}           # Get athlete by ID
GET    /api/athletes/email/{email}  # Get athlete by email
POST   /api/athletes                # Create athlete
PUT    /api/athletes/{id}           # Update athlete
DELETE /api/athletes/{id}           # Delete athlete
```

### 📝 **Registrations** (Protected)

```http
GET    /api/registrations                    # List all registrations
GET    /api/registrations/{id}               # Get registration by ID
GET    /api/registrations/event/{eventId}    # Get registrations by event
GET    /api/registrations/athlete/{athleteId} # Get registrations by athlete
POST   /api/registrations                    # Create registration
PUT    /api/registrations/{id}               # Update registration
PATCH  /api/registrations/{id}/payment-status # Update payment status
PATCH  /api/registrations/{id}/status        # Update registration status
PATCH  /api/registrations/{id}/cancel        # Cancel registration
DELETE /api/registrations/{id}               # Delete registration
```

### 💰 **Financial Management** (Protected)

```http
GET    /api/financial/events/{eventId}/summary      # Event financial summary
POST   /api/financial/events/{eventId}/transfer     # Trigger manual transfer
GET    /api/financial/transfers                     # List transfers
GET    /api/financial/transfers/{id}                # Get transfer details
POST   /api/financial/transfers/{id}/retry          # Retry failed transfer
```

### 📊 **Example Payloads**

#### Event Creation (POST /api/events)

```json
{
  "organizationId": 1,
  "name": "Corrida de São Paulo 2025",
  "eventType": "RUNNING",
  "eventDate": "2025-12-15",
  "eventTime": "07:00:00",
  "location": "Parque Ibirapuera",
  "address": "Av. Paulista, 1000 - São Paulo, SP",
  "maxParticipants": 500,
  "price": 75.0,
  "currency": "BRL",
  "registrationStartDate": "2025-10-01",
  "registrationEndDate": "2025-12-10"
}
```

#### Athlete Registration (POST /api/athletes)

```json
{
  "email": "joao.silva@email.com",
  "name": "João Silva",
  "phone": "+55 11 99999-9999",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "document": "123.456.789-00",
  "emergencyContact": "Maria Silva - +55 11 88888-8888",
  "address": "Rua das Flores, 123, Apto 45",
  "city": "São Paulo",
  "state": "SP",
  "country": "Brasil"
}
```

#### Event Registration (POST /api/registrations)

```json
{
  "event": { "id": 1 },
  "athlete": { "id": 1 },
  "category": "Geral",
  "teamName": "Corredores SP",
  "paymentStatus": "PENDING",
  "amountPaid": 75.0,
  "paymentMethod": "PIX",
  "tShirtSize": "M",
  "status": "ACTIVE"
}
```

## 🏗️ **Architecture**

```
src/
├── main/java/com/mvt/mvt_events/
│   ├── common/          # JWT utilities and shared components
│   ├── controller/      # REST API endpoints (Auth, Events, Financial)
│   ├── jpa/            # Entity classes (Events, Organizations, Athletes, Payments, Transfers)
│   ├── repository/     # Data access layer with multi-tenant queries
│   ├── repositories/   # Legacy repository package
│   ├── service/        # Business logic (Events, Financial, Transfers, Authentication)
│   └── config/         # Security and application configuration
├── main/resources/
│   ├── db/migration/   # Flyway migrations (V1-V5)
│   │   ├── V1__init.sql                              # Basic schema
│   │   ├── V2__create_users_table.sql               # User authentication
│   │   ├── V3__recreate_users_table_for_jwt.sql     # JWT optimization
│   │   ├── V4__create_multi_tenant_schema.sql       # Multi-tenant sports events
│   │   └── V5__create_financial_management_system.sql # Financial system
│   ├── application.properties                        # Configuration
│   └── application-prod.properties                   # Production settings
```

### 🏦 **Financial System Architecture**

The platform includes a comprehensive financial management system:

- **EventFinancials**: Consolidated financial data per event
- **Transfer**: Automated and manual transfer management
- **PaymentEvent**: Complete audit trail of financial operations
- **Payment**: Individual payment processing and tracking
- **TransferSchedulingService**: Automated transfer processing with configurable frequencies

For detailed information, see [FINANCIAL_SYSTEM.md](FINANCIAL_SYSTEM.md).

## 🏗️ **Multi-Tenant Usage Guide**

### 🔄 **Setting Event Context**

Before performing any operations on tenant-scoped data, set the event context:

```java
// In your service layer
@Transactional
public void setEventContext(Long eventId) {
    jdbcTemplate.execute("SELECT set_current_event(" + eventId + ")");
}

// Clear context when done (typically in request interceptor)
@Transactional
public void clearEventContext() {
    jdbcTemplate.execute("SELECT clear_current_event()");
}
```

### 🛡️ **Automatic Data Isolation**

Once context is set, all queries are automatically filtered:

```java
// This will only return athletes for the current event
List<Athlete> athletes = athleteRepository.findAll();

// This will only create registrations for the current event
Registration registration = registrationRepository.save(newRegistration);
```

### 🌐 **Global vs Tenant Data**

```java
// Global entities (no tenant filtering)
List<Organization> orgs = organizationRepository.findAll(); // All organizations
List<Event> events = eventRepository.findAll(); // All events

// Tenant-scoped entities (automatically filtered)
List<Athlete> athletes = athleteRepository.findAll(); // Only current event athletes
List<Payment> payments = paymentRepository.findAll(); // Only current event payments
```

│ ├── db/migration/ # Flyway SQL migrations
│ ├── application.properties # Default config
│ └── application-prod.properties # Production config

````

## 🚀 **Quick Start**

### 1. **Clone & Setup**

```bash
git clone <your-repo-url>
cd mvt-events
./gradlew build
````

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

| Category           | Technology                                |
| ------------------ | ----------------------------------------- |
| **Framework**      | Spring Boot 3.5.6, Spring Security 6      |
| **Language**       | Java 17 (Amazon Corretto)                 |
| **Database**       | PostgreSQL 16 with RLS, Flyway migrations |
| **Authentication** | JWT (io.jsonwebtoken 0.12.6)              |
| **Build Tool**     | Gradle 8.11.1                             |
| **Container**      | Docker, Docker Compose                    |
| **Deployment**     | Render.com, GitHub Container Registry     |
| **Testing**        | JUnit 5, Mockito, TDD methodology         |

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

**Application available at:** `http://localhost:8080`

### 3. **Authentication Workflow**

```bash
# 1. Register new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@mvtevents.com",
    "password": "SecurePass123!"
  }'

# 2. Login & get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@mvtevents.com",
    "password": "SecurePass123!"
  }'

# 3. Use token for protected endpoints
curl -X GET http://localhost:8080/api/organizations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### 4. **Testing the System**

```bash
# Run all tests
./gradlew test

# Run specific test suites
./gradlew test --tests "*.service.*"
./gradlew test --tests "*IntegrationTest"
```

## 🌍 **Production Deployment**

### **Render.com Deployment**

The application is configured for automatic deployment to Render.com:

1. **GitHub Integration**: Push to main branch triggers automatic deployment
2. **Docker Container**: Uses multi-stage build for optimized production image
3. **Environment Configuration**: Production-ready settings with PostgreSQL RLS
4. **Health Checks**: Actuator endpoints for monitoring

### **Environment Variables (Production)**

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=postgresql://username:password@host:port/database
JWT_SECRET=your-super-secure-jwt-secret-here
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

### **Database Migrations**

**Complete migration history:**

- **V1\_\_init.sql** - Initial schema setup
- **V2\_\_create_users_table.sql** - User authentication
- **V3\_\_recreate_users_table_for_jwt.sql** - JWT optimization
- **V4\_\_create_multi_tenant_schema.sql** - Multi-tenant sports events system
- **V5\_\_create_financial_management_system.sql** - Financial system integration
- **V6\_\_fix_athlete_registration_constraints.sql** - Registration uniqueness

## 🧪 **Test-Driven Development (TDD)**

This project was built using **TDD methodology** with comprehensive test coverage:

### **Test Architecture**

- **Unit Tests**: All service layer methods with Mockito stubs
- **Integration Tests**: Complete API endpoint testing
- **Repository Tests**: Data access layer validation
- **Security Tests**: JWT authentication and authorization

### **Key Testing Features**

- **Mockito Stubs**: Isolated unit testing with dependency mocking
- **@MockBean Integration**: Spring Boot test context with mocked dependencies
- **Custom Assertions**: Business logic validation with meaningful error messages
- **Test Data Builders**: Consistent test data generation with realistic scenarios

### **Running Tests**

```bash
# All tests with coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## 📊 **Business Features**

### **🏢 Organization Management**

- Multi-organization support with complete isolation
- Slug-based public URLs for SEO-friendly access
- Contact information and social media integration
- Financial tracking per organization

### **🏆 Event Management**

- Multi-sport events (running, cycling, triathlon, swimming, etc.)
- Advanced registration periods with precise date/time control
- Dynamic participant limits and tiered pricing
- Location management with address geocoding
- Comprehensive event lifecycle management

### **👤 Athlete Profiles**

- Complete participant registration with validation
- Document management (CPF, passport, international formats)
- Emergency contact and medical information
- Address and demographic data management
- Registration history and performance tracking

### **📝 Registration System**

- Flexible event-athlete relationship management
- Multi-state payment processing (pending, paid, refunded, failed)
- Category management (age groups, competitive levels)
- Team registration with group management
- T-shirt sizing and logistics coordination
- Advanced cancellation and refund workflows

### **💰 Financial Management**

- Automated payment processing and tracking
- Event-based financial reporting and analytics
- Transfer management with scheduled automation
- Complete audit trail for all financial operations
- Multi-currency support with conversion tracking

## 🎯 **Perfect For**

- **Event Management Platforms** requiring multi-tenant architecture
- **Sports Organizations** needing comprehensive athlete management
- **Payment Processing Systems** with financial compliance requirements
- **Enterprise Applications** requiring JWT authentication and security
- **SaaS Platforms** with complex business domain modeling
- **Any Spring Boot Application** needing production-ready architecture

---

**🎉 Production-Ready Sports Events Management Platform!**

_Built with TDD methodology, comprehensive testing, and enterprise-grade security. Ready for immediate deployment and scale._
