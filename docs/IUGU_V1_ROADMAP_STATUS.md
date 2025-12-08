# 🗺️ Roadmap Iugu v1.0 - Status de Implementação

**Data**: 2025-12-02  
**Versão**: v1.0  
**Status Geral**: 🟡 70% Completo (Core pronto, endpoints pendentes)

---

## 📊 Visão Geral do Progresso

```
███████████████████░░░░░░░░░ 70% Complete

✅ CONCLUÍDO    ████████████████ (16/23 itens)
🔄 EM PROGRESSO ░░░░░░░░░░░░░░░░ (0/23 itens)
⏳ PENDENTE     ░░░░░░░░ (7/23 itens)
```

---

## ✅ Fase 1: Infraestrutura e Entidades (100% ✅)

### 1.1 Database & Migrations

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Migration V4 | ✅ | `V4__create_bank_accounts.sql` | Tabela `bank_accounts` criada com 9 indexes |
| Aplicação V4 | ✅ | Logs | Aplicada em 31ms com sucesso |
| User.iuguAccountId | ✅ | `User.java` | Campo adicionado (nullable) |
| User.bankAccount | ✅ | `User.java` | Relacionamento 1:1 opcional |

**Resultado**: ✅ **Base de dados pronta para Iugu**

---

### 1.2 Entities & Repositories

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| BankAccount entity | ✅ | `BankAccount.java` | 15 campos, validações Bean Validation |
| BankAccountStatus enum | ✅ | `BankAccountStatus.java` | DRAFT, PENDING_VALIDATION, ACTIVE, BLOCKED |
| BankAccountRepository | ✅ | `BankAccountRepository.java` | Query `findByStatus()` implementada |
| UserRepository | ✅ | `UserRepository.java` | Query `findByIuguAccountId()` adicionada |
| BrazilianBanks utility | ✅ | `BrazilianBanks.java` | 50+ bancos brasileiros |
| @ValidBankCode | ✅ | `ValidBankCode.java` | Custom validator |

**Resultado**: ✅ **Modelo de dados robusto e validado**

---

### 1.3 Metadata Integration

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Translations PT/EN/ES | ✅ | `messages_*.properties` | 12 chaves traduzidas |
| Entity registration | ✅ | `MetadataService.java` | BankAccount registrada |
| Bank options endpoint | ✅ | `MetadataService.java` | Lista 50+ bancos via API |

**Resultado**: ✅ **Sistema i18n pronto**

---

## ✅ Fase 2: Core Iugu Integration (100% ✅)

### 2.1 Configuração

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| application.properties | ✅ | `application.properties` | 14 propriedades Iugu configuradas |
| IuguConfig class | ✅ | `IuguConfig.java` | @ConfigurationProperties com 6 inner classes |
| Bean validation | ✅ | `IuguConfig.java` | @NotBlank, @DecimalMin, @Min em todos os campos |
| validatePercentages() | ✅ | `SplitConfig` | Valida soma = 100% |
| iuguRestTemplate bean | ✅ | `IuguConfig.java` | Timeout 10s configurado |

**Resultado**: ✅ **Configuração centralizada e validada**

---

### 2.2 DTOs (Java 17+ Records)

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| CreateSubAccountRequest | ✅ | `CreateSubAccountRequest.java` | Factory: `withDefaults()` |
| SubAccountResponse | ✅ | `SubAccountResponse.java` | Helpers: `canReceivePayments()`, `isPendingVerification()` |
| SplitRule | ✅ | `SplitRule.java` | 6 factory methods: `forCourier()`, `forManager()`, etc. |
| CreateInvoiceRequest | ✅ | `CreateInvoiceRequest.java` | Factory: `forDelivery()` |
| InvoiceResponse | ✅ | `InvoiceResponse.java` | Helpers: `isPending()`, `isPaid()`, `getDeliveryId()` |
| WebhookEvent | ✅ | `WebhookEvent.java` | 8 helpers: `isPaymentConfirmed()`, `getInvoiceId()`, etc. |

**Resultado**: ✅ **DTOs imutáveis, type-safe e com helpers inteligentes**

---

### 2.3 IuguService (Core Business Logic)

| Item | Status | Arquivo | Método | Descrição |
|------|--------|---------|--------|-----------|
| Criar subconta | ✅ | `IuguService.java` | `createSubAccount()` | POST /marketplace/create_account |
| Atualizar banco | ✅ | `IuguService.java` | `updateBankAccount()` | PUT /accounts/{id}/bank_verification |
| Criar invoice | ✅ | `IuguService.java` | `createInvoiceWithSplit()` | POST /invoices com 87/5/8 split |
| Validar webhook | ✅ | `IuguService.java` | `validateWebhookSignature()` | Validação por token |
| Consultar status | ✅ | `IuguService.java` | `getSubAccountStatus()` | GET /accounts/{id} |
| Basic Auth | ✅ | `IuguService.java` | `createAuthHeaders()` | Base64(apiKey:) |
| Exception handling | ✅ | `IuguService.java` | `IuguApiException` | Exceção customizada |

**Resultado**: ✅ **Service completo com 5 métodos e tratamento de erros**

---

## ✅ Fase 3: Verificação Assíncrona (100% ✅)

### 3.1 Job Agendado

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| IuguVerificationSyncService | ✅ | `IuguVerificationSyncService.java` | Service com @Scheduled (264 linhas) |
| @Scheduled job | ✅ | `syncPendingVerifications()` | Cron: `0 0 */6 * * *` (a cada 6h) |
| Query PENDING accounts | ✅ | `syncPendingVerifications()` | Busca via `findByStatus()` |
| Sync logic | ✅ | `syncAccountVerification()` | Chama `getSubAccountStatus()` |
| Update local status | ✅ | `handleVerified()/handleRejected()` | Atualiza banco local |
| Rate limiting | ✅ | `syncPendingVerifications()` | Thread.sleep(1000) entre requests |
| Logging detalhado | ✅ | Todo o service | Emojis: ✅, ❌, ⏳, 🔄 |
| @ConditionalOnProperty | ✅ | Class annotation | Flag enable/disable |

**Resultado**: ✅ **Job robusto com rate limit e logging detalhado**

---

### 3.2 Push Notifications

| Item | Status | Arquivo | Método | Descrição |
|------|--------|---------|--------|-----------|
| Notificar verificado | ✅ | `PushNotificationService.java` | `notifyBankDataVerified()` | "✅ Dados Bancários Verificados!" |
| Notificar rejeitado | ✅ | `PushNotificationService.java` | `notifyBankDataRejected()` | "⚠️ Dados Bancários Rejeitados" |
| Notificar pagamento | ✅ | `PushNotificationService.java` | `notifyPaymentReceived()` | "💰 Pagamento Recebido!" |
| Notificar transferência | ✅ | `PushNotificationService.java` | `notifyWithdrawalCompleted()` | "🏦 Transferência Concluída!" |
| Integração job | ✅ | `IuguVerificationSyncService.java` | Chama pushNotificationService em handleVerified/Rejected |
| Error handling | ✅ | `IuguVerificationSyncService.java` | Try-catch para não quebrar job |

**Resultado**: ✅ **Push notifications integradas com Expo/FCM**

---

### 3.3 Configuração do Job

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Cron expression | ✅ | `application.properties` | `iugu.verification-sync.cron=0 0 */6 * * *` |
| Enable flag | ✅ | `application.properties` | `iugu.verification-sync.enabled=true` |
| Max pending days | ✅ | `application.properties` | `iugu.verification-sync.max-pending-days=10` |
| @EnableScheduling | ✅ | `MvtApplication.java` | Já habilitado no projeto |

**Resultado**: ✅ **Job configurável via environment variables**

---

## ⏳ Fase 4: REST API Endpoints (0% ⏳)

### 4.1 BankAccountController (Motoboy/Gerente)

| Item | Status | Endpoint | Descrição |
|------|--------|----------|-----------|
| Cadastrar dados | ⏳ | `POST /api/motoboy/bank-data` | Cria BankAccount + Iugu subconta |
| Listar dados | ⏳ | `GET /api/motoboy/bank-data` | Retorna dados bancários do usuário |
| Atualizar dados | ⏳ | `PUT /api/motoboy/bank-data` | Atualiza BankAccount + Iugu |
| Verificar status | ⏳ | `GET /api/motoboy/bank-data/verification-status` | Consulta manual no Iugu |
| Validações | ⏳ | All endpoints | @Valid + Bean Validation |
| Security | ⏳ | All endpoints | @PreAuthorize("hasRole('COURIER') or hasRole('ORGANIZER')") |

**Prioridade**: 🔴 **ALTA** (Necessário para motoboys cadastrarem dados)

---

### 4.2 PaymentController (Sistema de Pagamentos)

| Item | Status | Endpoint | Descrição |
|------|--------|----------|-----------|
| Criar pagamento PIX | ⏳ | `POST /api/payment/create-with-split` | Cria invoice com split 87/5/8 |
| Consultar invoice | ⏳ | `GET /api/payment/invoice/{id}` | Retorna InvoiceResponse |
| Listar pagamentos | ⏳ | `GET /api/payment/history` | Histórico de pagamentos do usuário |
| Cancelar invoice | ⏳ | `DELETE /api/payment/invoice/{id}` | Cancela invoice pendente |
| Security | ⏳ | All endpoints | @PreAuthorize baseado em role |

**Prioridade**: 🔴 **ALTA** (Necessário para clientes pagarem entregas)

---

### 4.3 WebhookController (Eventos Iugu)

| Item | Status | Endpoint | Descrição |
|------|--------|----------|-----------|
| Receber webhooks | ⏳ | `POST /api/webhooks/iugu` | Endpoint público para Iugu |
| Validar assinatura | ⏳ | Webhook handler | Chama `validateWebhookSignature()` |
| invoice.paid | ⏳ | Event handler | Atualiza Payment status |
| withdrawal.completed | ⏳ | Event handler | Notifica motoboy (D+1) |
| invoice.refunded | ⏳ | Event handler | Atualiza Payment + notifica |
| Idempotência | ⏳ | All handlers | Verificar se evento já processado |
| Logging | ⏳ | All handlers | Log detalhado de cada evento |

**Prioridade**: 🟡 **MÉDIA** (Sistema funciona sem, mas melhora UX)

---

## ⏳ Fase 5: Payment Entity Updates (0% ⏳)

### 5.1 Migration V5

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Add Iugu fields | ⏳ | `V5__add_payment_iugu_fields.sql` | iugu_invoice_id, pix_qr_code, etc. |
| Add split_rules | ⏳ | `V5__add_payment_iugu_fields.sql` | JSONB com splits aplicados |
| Add expires_at | ⏳ | `V5__add_payment_iugu_fields.sql` | Vencimento da invoice |
| Create indexes | ⏳ | `V5__add_payment_iugu_fields.sql` | Index em iugu_invoice_id |

**Prioridade**: 🟡 **MÉDIA** (Necessário para rastreabilidade)

---

### 5.2 Payment Entity

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Add @Column fields | ⏳ | `Payment.java` | iuguInvoiceId, pixQrCode, expiresAt |
| Add split rules | ⏳ | `Payment.java` | @Type(JsonType.class) Map<String, Object> |
| Add helper methods | ⏳ | `Payment.java` | isExpired(), getMotoboyShare(), etc. |

**Prioridade**: 🟡 **MÉDIA**

---

## ⏳ Fase 6: Tests & Validation (0% ⏳)

### 6.1 Unit Tests

| Item | Status | Arquivo | Cobertura |
|------|--------|---------|-----------|
| IuguConfig tests | ⏳ | `IuguConfigTest.java` | Validações, validatePercentages() |
| IuguService tests | ⏳ | `IuguServiceTest.java` | Mock RestTemplate, testar 5 métodos |
| DTO tests | ⏳ | `IuguDtosTest.java` | Testar factory methods e helpers |
| SplitRule validation | ⏳ | `SplitRuleTest.java` | Testar validate() com valores inválidos |

**Prioridade**: 🟢 **BAIXA** (Após endpoints prontos)

---

### 6.2 Integration Tests

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| Job execution test | ⏳ | `IuguVerificationSyncServiceIT.java` | Testar @Scheduled execution |
| Controller tests | ⏳ | `BankAccountControllerIT.java` | Testar endpoints com @SpringBootTest |
| Webhook tests | ⏳ | `WebhookControllerIT.java` | Simular eventos Iugu |
| End-to-end flow | ⏳ | `IuguPaymentFlowIT.java` | Cadastro → pagamento → webhook |

**Prioridade**: 🟢 **BAIXA** (Após tudo funcionar)

---

## ⏳ Fase 7: Security & Production (0% ⏳)

### 7.1 Security Enhancements

| Item | Status | Descrição |
|------|--------|-----------|
| Encrypt bank data | ⏳ | Criptografar accountNumber com JPA AttributeConverter |
| HMAC webhook validation | ⏳ | Substituir validação simples por HMAC SHA256 |
| Rate limiting | ⏳ | Limitar requests por IP/user em endpoints públicos |
| Audit trail | ⏳ | Log todas as alterações em BankAccount |
| PCI compliance | ⏳ | Garantir que não logamos dados sensíveis |

**Prioridade**: 🔴 **ALTA** (Antes de produção)

---

### 7.2 Monitoring & Observability

| Item | Status | Descrição |
|------|--------|-----------|
| Metrics | ⏳ | Micrometer metrics para Iugu API calls |
| Alerts | ⏳ | Alertar quando job falha 3x seguidas |
| Dashboard | ⏳ | Grafana dashboard com taxa de aprovação |
| Error tracking | ⏳ | Sentry/Rollbar para erros Iugu |

**Prioridade**: 🟡 **MÉDIA** (Após tudo funcionar)

---

### 7.3 Documentation

| Item | Status | Arquivo | Descrição |
|------|--------|---------|-----------|
| API Documentation | ✅ | `API_ENDPOINTS_CRUD.md` | Swagger/OpenAPI gerado |
| Architecture Roadmap | ✅ | `PAYMENT_ARCHITECTURE_ROADMAP.md` | v1.0 → v2.0 |
| Implementation Summary | ✅ | `IUGU_IMPLEMENTATION_SUMMARY.md` | Core implementado |
| Service Usage Guide | ✅ | `IUGU_SERVICE_USAGE_GUIDE.md` | Como usar IuguService |
| Sync Job Guide | ✅ | `IUGU_VERIFICATION_SYNC_JOB.md` | Como funciona o job |
| Push Notifications | ✅ | `IUGU_PUSH_NOTIFICATIONS_INTEGRATION.md` | Integração FCM |
| Deployment Guide | ⏳ | `IUGU_DEPLOYMENT_GUIDE.md` | Checklist de produção |

**Prioridade**: 🟡 **MÉDIA** (Deployment guide pendente)

---

## 📊 Resumo por Fase

| Fase | Progresso | Status | Prioridade |
|------|-----------|--------|------------|
| **1. Infraestrutura** | ████████████████████ 100% | ✅ Completo | - |
| **2. Core Integration** | ████████████████████ 100% | ✅ Completo | - |
| **3. Verificação Async** | ████████████████████ 100% | ✅ Completo | - |
| **4. REST Endpoints** | ░░░░░░░░░░░░░░░░░░░░ 0% | ⏳ Pendente | 🔴 Alta |
| **5. Payment Updates** | ░░░░░░░░░░░░░░░░░░░░ 0% | ⏳ Pendente | 🟡 Média |
| **6. Tests** | ░░░░░░░░░░░░░░░░░░░░ 0% | ⏳ Pendente | 🟢 Baixa |
| **7. Production** | ░░░░░░░░░░░░░░░░░░░░ 0% | ⏳ Pendente | 🔴 Alta |

---

## 🎯 Próximos Passos Recomendados

### 1️⃣ Testar Job Executando (1h)
```bash
# 1. Subir aplicação
./gradlew bootRun

# 2. Verificar logs do job (esperar 6h OU mudar cron para */5 * * * * para 5min)
tail -f app-boot.log | grep IuguVerificationSyncService

# 3. Criar BankAccount de teste com status PENDING_VALIDATION
# 4. Forçar status "verified" no Iugu sandbox
# 5. Verificar se job atualiza para ACTIVE e envia push notification
```

**Tempo estimado**: 1 hora  
**Prioridade**: 🟡 Média (Validar implementação)

---

### 2️⃣ Implementar BankAccountController (4-6h)

**Endpoints críticos:**
```java
POST   /api/motoboy/bank-data            // Cadastrar dados + criar subconta Iugu
GET    /api/motoboy/bank-data            // Consultar dados cadastrados
PUT    /api/motoboy/bank-data            // Atualizar dados + Iugu
GET    /api/motoboy/bank-data/verification-status  // Consulta manual
```

**Arquivos a criar:**
- `BankAccountController.java` (~200 linhas)
- `BankAccountService.java` (~150 linhas) - Orquestrar IuguService + Repository
- `BankAccountRequest.java` (DTO) (~50 linhas)
- `BankAccountResponse.java` (DTO) (~40 linhas)

**Tempo estimado**: 4-6 horas  
**Prioridade**: 🔴 **ALTA** (Bloqueia motoboys de cadastrarem dados)

---

### 3️⃣ Implementar PaymentController (6-8h)

**Endpoints críticos:**
```java
POST   /api/payment/create-with-split    // Cliente paga entrega
GET    /api/payment/invoice/{id}         // Consultar invoice
GET    /api/payment/history              // Histórico
```

**Arquivos a criar:**
- `PaymentController.java` (~250 linhas)
- `PaymentService.java` (~200 linhas) - Orquestrar Delivery + IuguService
- Migration V5 (~30 linhas)
- Atualizar `Payment.java` (~20 linhas)

**Tempo estimado**: 6-8 horas  
**Prioridade**: 🔴 **ALTA** (Bloqueia pagamentos de entregas)

---

### 4️⃣ Implementar WebhookController (4-6h)

**Endpoints:**
```java
POST   /api/webhooks/iugu    // Receber eventos (invoice.paid, withdrawal.completed)
```

**Arquivos a criar:**
- `WebhookController.java` (~150 linhas)
- `IuguWebhookService.java` (~200 linhas) - Processar eventos
- Event handlers para cada tipo de evento

**Tempo estimado**: 4-6 horas  
**Prioridade**: 🟡 **MÉDIA** (Melhora UX, não bloqueia fluxo)

---

### 5️⃣ Security Enhancements (8-12h)

**Tarefas:**
- Criptografar `accountNumber` com JPA AttributeConverter
- Implementar HMAC SHA256 em `validateWebhookSignature()`
- Adicionar rate limiting em endpoints públicos
- Audit trail completo (Hibernate Envers)
- Revisar logs para não expor dados sensíveis

**Tempo estimado**: 8-12 horas  
**Prioridade**: 🔴 **ALTA** (Antes de produção)

---

### 6️⃣ Tests (12-16h)

**Tarefas:**
- Unit tests: IuguConfig, IuguService, DTOs, SplitRule
- Integration tests: Controllers, Job, Webhooks
- End-to-end flow test
- Cobertura mínima: 80%

**Tempo estimado**: 12-16 horas  
**Prioridade**: 🟢 **BAIXA** (Após tudo funcionar)

---

## 🏁 Estimativa de Conclusão Total

| Fase | Tempo Estimado | Prioridade |
|------|----------------|------------|
| ✅ **Fases 1-3 concluídas** | - | ✅ |
| 🧪 Teste do Job | 1h | 🟡 |
| 👤 BankAccountController | 4-6h | 🔴 |
| 💰 PaymentController | 6-8h | 🔴 |
| 🪝 WebhookController | 4-6h | 🟡 |
| 🔒 Security | 8-12h | 🔴 |
| ✅ Tests | 12-16h | 🟢 |
| **TOTAL** | **35-49h** | - |

**Tempo até MVP funcional (endpoints + security)**: **~25-35 horas**  
**Tempo até produção (MVP + tests + monitoring)**: **~40-50 horas**

---

## ✅ O Que Já Está Pronto para Usar

### Você pode HOJE:
1. ✅ Configurar credenciais Iugu em `application.properties`
2. ✅ Chamar `iuguService.createSubAccount(user, bankAccount)` manualmente
3. ✅ Chamar `iuguService.createInvoiceWithSplit()` para gerar PIX
4. ✅ O job já sincroniza status de verificação a cada 6h
5. ✅ Push notifications são enviadas quando contas são verificadas/rejeitadas
6. ✅ Consultar status manualmente via `iuguService.getSubAccountStatus()`

### O que NÃO funciona ainda:
- ❌ Motoboy não consegue cadastrar dados via API (sem controller)
- ❌ Cliente não consegue pagar entrega via API (sem PaymentController)
- ❌ Sistema não recebe webhooks do Iugu (sem WebhookController)
- ❌ Dados bancários não estão criptografados
- ❌ Sem tests automatizados

---

## 🚀 Recomendação Final

**Para ter um MVP funcional rapidamente:**

1. **Dia 1 (8h)**: Implementar `BankAccountController` + `BankAccountService`
2. **Dia 2 (8h)**: Implementar `PaymentController` + Migration V5
3. **Dia 3 (6h)**: Implementar `WebhookController` + testar fluxo end-to-end
4. **Dia 4 (8h)**: Security (criptografia + HMAC + rate limit)
5. **Dia 5 (8h)**: Testar em staging + fix bugs + deploy produção

**Total**: ~38 horas (~5 dias úteis)

Depois disso, você tem um **sistema de pagamentos Iugu 100% funcional em produção**! 🎉

---

**Mantido por**: Equipe de Backend  
**Última atualização**: 2025-12-02
