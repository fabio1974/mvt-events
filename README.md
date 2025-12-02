# 🚀 Zapi10 - Plataforma de Entregas Inteligente

> Sistema completo de gestão de entregas com notificações inteligentes em 3 níveis, multi-tenancy e integração de pagamentos.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Private-red.svg)]()

---

## 📋 Índice Rápido

- [🎯 Visão Geral](#-visão-geral)
- [🚀 Começando](#-começando)
- [📚 Documentação](#-documentação)
- [🏗️ Arquitetura](#️-arquitetura)
- [🔧 Configuração](#-configuração)
- [🧪 Testes](#-testes)
- [🤝 Contribuindo](#-contribuindo)

---

## 🎯 Visão Geral

O **Zapi10** é uma plataforma de entregas que conecta:
- 👥 **Clientes** (estabelecimentos)
- 🏢 **Grupos de Logística**
- 🏍️ **Motoboys**

### Principais Características

✨ **Sistema de Notificações em 3 Níveis**
- 🥇 Nível 1: Grupo Principal (prioridade)
- 🥈 Nível 2: Grupos Secundários
- 🥉 Nível 3: Todos os disponíveis

💰 **Divisão Justa de Valores**
- 85% para o motoboy
- 15% para o grupo de logística

🔒 **Multi-tenancy com Segurança**
- Isolamento de dados por organização
- Row-Level Security (RLS) no PostgreSQL
- Autenticação e autorização robustas

📱 **Notificações Push em Tempo Real**
- Firebase Cloud Messaging (FCM)
- Suporte iOS e Android
- Sistema de fallback inteligente

---

## 🚀 Começando

### Pré-requisitos

```bash
# Versões necessárias
Java 17+
PostgreSQL 13+
Gradle 7+
Docker (opcional)
```

### Instalação Rápida

```bash
# 1. Clone o repositório
git clone https://github.com/fabio1974/mvt-events.git
cd mvt-events

# 2. Configure o banco de dados
docker-compose up -d postgres

# 3. Execute as migrações
./gradlew flywayMigrate

# 4. Inicie a aplicação
./gradlew bootRun
```

📖 **[Guia de Início Rápido Completo](docs/QUICK_START.md)**

---

## 📚 Documentação

### 📊 Para Gestores e Gerentes

| Documento | Descrição |
|-----------|-----------|
| [📊 Apresentação Gerencial](docs/APRESENTACAO_GERENCIAL_ZAPI10.md) | Visão executiva do sistema, ROI e estratégias de negócio |
| [📖 Sistema de Grupos e Notificações](docs/APRESENTACAO_SISTEMA_GRUPOS_E_NOTIFICACOES.md) | Detalhamento técnico do algoritmo de 3 níveis |
| [💼 Roles e Organizações](docs/ROLES_E_ORGANIZACOES.md) | Estrutura de permissões e hierarquia |

### 🔧 Para Desenvolvedores

#### 🏁 Início

| Documento | Descrição |
|-----------|-----------|
| [🚀 Quick Start](docs/QUICK_START.md) | Como iniciar o projeto em 5 minutos |
| [🏗️ Arquitetura Geral](docs/INDEX.md) | Visão geral da arquitetura do sistema |
| [🗄️ Migrações de Banco](docs/RUN_MIGRATIONS_GUIDE.md) | Como executar e criar migrações com Flyway |

#### 📡 API e Endpoints

| Documento | Descrição |
|-----------|-----------|
| [📖 Documentação da API](docs/API_DOCUMENTATION.md) | Referência completa da API REST |
| [🔄 Fluxo de Entregas](docs/API_DELIVERY_FLOW.md) | Como funciona o ciclo de vida de uma entrega |
| [📝 Endpoints CRUD](docs/API_ENDPOINTS_CRUD.md) | Operações básicas de todas as entidades |
| [🌐 Configuração de Sites](docs/SITE_CONFIGURATION_ENDPOINTS.md) | Endpoints de configuração |

#### 🏗️ Arquitetura e Design

| Documento | Descrição |
|-----------|-----------|
| [🏛️ Arquitetura de Metadata](docs/architecture/) | Sistema de filtros e multi-tenancy |
| [🔍 Guia de Filtros](docs/api/FILTERS_GUIDE.md) | Como usar filtros na API |
| [🔗 Entity Filters](docs/features/ENTITY_FILTERS.md) | Filtros automáticos por entidade |
| [📊 Backend Architecture](docs/backend/) | Estrutura do backend |

#### 💼 Funcionalidades do Sistema

| Documento | Descrição |
|-----------|-----------|
| [🚚 Tipos de Entrega](docs/TIPOS_DE_ENTREGA.md) | Entregas on-demand, agendadas e recorrentes |
| [📦 Entregas On-Demand](docs/ENTREGAS_ON_DEMAND.md) | Sistema de entregas imediatas |
| [🤝 Sistema de Contratos](docs/SISTEMA_CONTRATOS_BIDIRECIONAL.md) | Contratos bidirecionais (cliente ↔ grupo ↔ motoboy) |
| [🔔 Notificações Push](docs/SISTEMA_NOTIFICACAO_PUSH_COMPLETO.md) | Sistema completo de push notifications |
| [💳 Sistema de Pagamentos](docs/PAYMENT_SYSTEM_COMPLETE.md) | Gestão de pagamentos e comissões |
| [🔐 Permissões](docs/PERMISSOES_CRIAR_ENTREGAS.md) | Controle de acesso para criar entregas |
| [📍 Geolocalização](docs/GEOLOCATION_FIELDS.md) | Campos de geolocalização e cálculo de distâncias |

#### 📱 Configuração de Push Notifications

| Documento | Descrição |
|-----------|-----------|
| [🔔 Sistema Completo de Push](docs/SISTEMA_NOTIFICACAO_PUSH_COMPLETO.md) | Arquitetura e implementação |
| [🍎 Guia FCM para iPhone](docs/GUIA_ATIVAR_FCM_IPHONE.md) | Como configurar FCM no iOS |
| [📲 Setup Expo Token](docs/EXPO_TOKEN_SETUP.md) | Configuração de tokens Expo |

#### 🧪 Testes e Qualidade

| Documento | Descrição |
|-----------|-----------|
| [🧪 Testing Documentation](docs/TESTING.md) | Estratégia de testes e cobertura |
| [🔒 Security](docs/SECURITY.md) | Práticas de segurança implementadas |
| [🐛 Troubleshooting](docs/TROUBLESHOOTING.md) | Solução de problemas comuns |

#### 🗂️ Histórico e Mudanças

| Documento | Descrição |
|-----------|-----------|
| [🧹 Cleanup Summary](docs/CLEANUP_SUMMARY.md) | Resumo de limpezas realizadas |
| [📝 Reorganization Summary](docs/REORGANIZATION_SUMMARY.md) | Reestruturações do código |
| [🔄 Session Summaries](docs/SESSION_SUMMARY.md) | Resumos de sessões de desenvolvimento |
| [✅ Tests Removed](docs/TESTS_REMOVED.md) | Testes removidos e motivos |
| [💸 Unified Payout Removed](docs/UNIFIED_PAYOUT_REMOVED.md) | Remoção do sistema de payout unificado |
| [🔄 Transfer Removed](docs/TRANSFER_REMOVED.md) | Remoção do sistema de transferências |

#### 🌍 Traduções e Modelos

| Documento | Descrição |
|-----------|-----------|
| [🇧🇷 Tradução Contratos Motoboy](docs/TRADUCAO_CONTRATO_MOTOBOY.md) | Tradução dos contratos |
| [📋 Modelo Simplificado](docs/MODELO_SIMPLIFICADO.md) | Modelo de dados simplificado |
| [✨ Sistema Simplificado Completo](docs/SISTEMA_SIMPLIFICADO_COMPLETO.md) | Visão simplificada do sistema |

---

## 🏗️ Arquitetura

### Stack Tecnológica

```
Backend:
├── Java 17
├── Spring Boot 3.5.6
├── Spring Security
├── Spring Data JPA
├── Hibernate 6
└── Flyway (Migrations)

Database:
├── PostgreSQL 13+
└── Row-Level Security (RLS)

Notificações:
├── Firebase Cloud Messaging (FCM)
└── Expo Push Notifications

DevOps:
├── Docker
├── Docker Compose
└── Gradle
```

### Arquitetura de Alto Nível

```
┌─────────────────┐
│  Mobile Apps    │
│  (iOS/Android)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API REST      │
│  Spring Boot    │
└────────┬────────┘
         │
    ┌────┴─────┐
    ▼          ▼
┌─────────┐ ┌──────────┐
│PostgreSQL│ │   FCM    │
│  + RLS   │ │  Push    │
└──────────┘ └──────────┘
```

**[📖 Documentação Completa da Arquitetura](docs/architecture/)**

---

## 🔧 Configuração

### Variáveis de Ambiente

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/mvt-events
SPRING_DATASOURCE_USERNAME=mvt
SPRING_DATASOURCE_PASSWORD=mvtpass

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Firebase (Push Notifications)
FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

### Perfis de Execução

```bash
# Desenvolvimento
./gradlew bootRun --args='--spring.profiles.active=dev'

# Produção
./gradlew bootRun --args='--spring.profiles.active=prod'

# Com porta específica
./gradlew bootRun --args='--server.port=8080'
```

### Docker Compose

```bash
# Iniciar todos os serviços
docker-compose up -d

# Apenas PostgreSQL
docker-compose up -d postgres

# Ver logs
docker-compose logs -f

# Parar serviços
docker-compose down
```

**[📖 Guia Completo de Configuração](docs/QUICK_START.md)**

---

## 🧪 Testes

```bash
# Executar todos os testes
./gradlew test

# Testes com relatório
./gradlew test --info

# Testes de integração
./gradlew integrationTest

# Cobertura de código
./gradlew jacocoTestReport
```

**[📖 Documentação de Testes](docs/TESTING.md)**

---

## 📱 API Endpoints

### Principais Recursos

```
POST   /api/deliveries          # Criar entrega
GET    /api/deliveries          # Listar entregas
GET    /api/deliveries/{id}     # Buscar entrega
PUT    /api/deliveries/{id}     # Atualizar entrega
DELETE /api/deliveries/{id}     # Deletar entrega

POST   /api/notifications       # Enviar notificação push
GET    /api/contracts           # Listar contratos
POST   /api/contracts           # Criar contrato

# Swagger UI
GET    /swagger-ui.html         # Interface Swagger
GET    /v3/api-docs             # OpenAPI JSON
```

**[📖 Documentação Completa da API](docs/API_DOCUMENTATION.md)**

---

## 🔐 Segurança

- ✅ Multi-tenancy com isolamento por organização
- ✅ Row-Level Security (RLS) no PostgreSQL
- ✅ Spring Security com JWT
- ✅ Validação de permissões em todos os endpoints
- ✅ Sanitização de inputs
- ✅ Rate limiting

**[📖 Guia de Segurança](docs/SECURITY.md)**

---

## 🤝 Contribuindo

### Fluxo de Desenvolvimento

1. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
2. Faça suas alterações
3. Commit: `git commit -m "feat: adiciona nova funcionalidade"`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

### Convenções de Commit

```
feat: nova funcionalidade
fix: correção de bug
docs: alteração na documentação
refactor: refatoração de código
test: adição/alteração de testes
chore: tarefas de manutenção
```

---

## 📞 Suporte

### Problemas Comuns

Consulte o **[Troubleshooting Guide](docs/TROUBLESHOOTING.md)** para soluções de problemas comuns.

### Contato

- 📧 Email: suporte@zapi10.com
- 📱 WhatsApp: (11) 99999-9999
- 🌐 Site: https://zapi10.com

---

## 📄 Licença

Este projeto é proprietário e confidencial.

---

## 📊 Status do Projeto

```
✅ Sistema de Entregas
✅ Multi-tenancy e RLS
✅ Sistema de Contratos Bidirecional
✅ Notificações Push (FCM)
✅ Algoritmo de 3 Níveis
✅ Sistema de Pagamentos
✅ API REST Completa
🚧 Dashboard Web (em desenvolvimento)
🚧 App Mobile (em desenvolvimento)
```

**Última atualização:** Dezembro 2024

---

<div align="center">

**[⬆ Voltar ao topo](#-zapi10---plataforma-de-entregas-inteligente)**

Made with ❤️ by Zapi10 Team

</div>
