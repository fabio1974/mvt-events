# 🏃‍♂️ MVT Events - Sports Events Management Platform

A **comprehensive multi-tenant sports events management platform** built with **Spring Boot 3.5.6**, featuring **JWT authentication**, **PostgreSQL with Row Level Security (RLS)**, **automated financial management**, and **enterprise-grade multi-tenancy architecture**. Designed for organizing and managing sporting events like marathons, cycling races, triathlons, and more.

## 🚀 **Latest Updates**

### 🏗️ **Multi-Tenancy Architecture** (September 2025)

- **🎯 Events-as-Tenants**: Each event operates as an independent tenant
- **🔒 PostgreSQL RLS**: Row Level Security for automatic data isolation
- **🌐 Global Organizations**: Shared across all events
- **🛡️ Automatic Security**: Context-aware database policies
- **⚡ Performance Optimized**: Composite indexes for multi-tenant queries

### ✅ **Complete CRUD Implementation** (September 2025)

- **Full TDD development** with JUnit 5 + Mockito test coverage
- **All entity CRUD operations** implemented and tested
- **API-first design** with comprehensive REST endpoints
- **Event creation with DTO pattern** for complex object handling
- **Lazy loading optimization** with strategic @JsonIgnore annotations
- **Live API testing validated** via Postman with JWT authentication

### 🎯 **Core Entities & Features Implemented**

#### 🏢 **Organizations**

- ✅ Complete CRUD with automatic slug generation
- ✅ Contact validation and address management
- ✅ Event ownership and hierarchy

#### 🏆 **Events**

- ✅ **EventCreateRequest DTO** for clean API design
- ✅ Automatic `startsAt` calculation from date + time
- ✅ Organization validation and association
- ✅ Unique slug generation and validation
- ✅ Multiple event types (Running, Cycling, Triathlon, etc.)

#### 👤 **Athletes**

- ✅ Complete profile management with demographics
- ✅ Unique email and document validation
- ✅ Emergency contact and address information
- ✅ Gender and date of birth tracking

#### 📝 **Registrations**

- ✅ **Athlete ↔ Event relationship management**
- ✅ Unique constraint (one athlete per event)
- ✅ Payment status and method tracking
- ✅ Category and team assignment
- ✅ Special needs and T-shirt size management

### 🔐 **Authentication & Security**

#### 🛡️ **JWT Authentication System**

- ✅ User registration with name/email/password
- ✅ JWT token generation and validation
- ✅ Bearer token authentication for all protected endpoints
- ✅ Role-based access control
- ✅ **Live tested** with Postman authentication flow

#### 🏛️ **Multi-Tenant Architecture**

- ✅ **PostgreSQL Row Level Security (RLS)** implementation
- ✅ Event-as-Tenant strategy for data isolation
- ✅ Tenant-aware queries and operations
- ✅ Security context propagation

## ✨ **Key Features**

### 🏆 **Sports Events Management**

- **Multi-tenant architecture** with event-level isolation using PostgreSQL Row Level Security (RLS)
- **Comprehensive event types**: Running, cycling, triathlon, swimming, trail running, mountain biking, and more
- **Organization management** with event hierarchies
- **Athlete registration system** with category management
- **Results tracking** and performance analytics
- **Event categories** and participant management

### 🏗️ **Multi-Tenancy Architecture**

Our platform implements a sophisticated **Events-as-Tenants** architecture:

#### 🎯 **Tenant Strategy**

- **Events are Tenants**: Each event operates as an independent data silo
- **Organizations are Global**: Shared across all events for scalability
- **Automatic Isolation**: Row Level Security (RLS) policies enforce data separation

#### 🔒 **Security Implementation**

```sql
-- Set event context at request start
SELECT set_current_event(123);

-- All queries automatically filtered by tenant_id
SELECT * FROM athletes; -- Only returns athletes for event 123
```

#### 📊 **Isolated Entities**

- ✅ **Athletes** - Scoped per event
- ✅ **Registrations** - Event-specific enrollments
- ✅ **Payments** - Isolated financial records
- ✅ **Transfers** - Event-based financial operations
- ✅ **Users** - Event-context user management
- ✅ **Event Financials** - Per-event financial tracking

#### 🌐 **Global Entities**

- ✅ **Organizations** - Shared across all events
- ✅ **Events** - Tenant definitions themselves

### 💰 **Financial Management System**

- **Automated payment processing** with configurable platform fees
- **Flexible transfer scheduling**: Immediate, daily, weekly, monthly, or on-demand
- **Multi-payment method support**: PIX, credit card, bank transfer, TED
- **Gateway integration** with retry logic and failure handling
- **Complete audit trail** with payment events tracking
- **Real-time financial reporting** per event and organization

### 🔐 **Security & Authentication**

- **JWT Authentication** with tenant-aware authorization
- **Role-based access control** (`ORGANIZER`, `ATHLETE`, `ADMIN`)
- **Event-context security** ensuring data isolation
- **Spring Security 6** with custom authentication filters
- **Multi-tenant Row Level Security** at database level

### 🗄️ **Database & Architecture**

- **PostgreSQL 16** with Row Level Security (RLS)
- **Event-as-Tenant strategy** for optimal scalability
- **Flyway migrations** with multi-tenancy support:
  - **V1**: Initial schema from production dump
  - **V2**: Complete multi-tenancy implementation with RLS
- **Spring Data JPA** with optimized queries
- **Connection pooling** with HikariCP

#### 🔄 **Migration Strategy**

```bash
# V1: Initialize complete schema
./gradlew flywayMigrate -Dflyway.target=1

# V2: Enable multi-tenancy with RLS
./gradlew flywayMigrate -Dflyway.target=2
```

### 🐳 **DevOps & Deployment**

- **Multi-stage Dockerfile** optimized for production
- **Docker Compose** setup for local development
- **GitHub Actions CI/CD** with automated GHCR publishing
- **Render.com deployment** configuration
- **Health checks** via Spring Boot Actuator

## 🔗 **API Endpoints**

### � **Authentication**

```http
POST /api/auth/register    # User registration (name, email, password)
POST /api/auth/login       # JWT token generation
```

### 🏢 **Organizations** (Protected)

```http
GET    /api/organizations           # List all organizations
GET    /api/organizations/{id}      # Get organization by ID
GET    /api/organizations/slug/{slug} # Get organization by slug
POST   /api/organizations           # Create organization
PUT    /api/organizations/{id}      # Update organization
DELETE /api/organizations/{id}      # Delete organization
```

### 🏆 **Events** (Protected)

```http
GET    /api/events                  # List all events
GET    /api/events/{id}             # Get event by ID
GET    /api/events/slug/{slug}      # Get event by slug
POST   /api/events                  # Create event (uses EventCreateRequest DTO)
PUT    /api/events/{id}             # Update event
DELETE /api/events/{id}             # Delete event
```

### 👤 **Athletes** (Protected)

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
