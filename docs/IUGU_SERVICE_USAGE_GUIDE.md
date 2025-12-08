# 📘 Guia de Uso do IuguService

**Data**: 2025-12-02  
**Versão**: 1.0

---

## 🎯 Visão Geral

Este guia mostra **como usar o IuguService** em controllers e outros services para integrar pagamentos PIX com split.

---

## 🔧 Injeção de Dependência

```java
@RestController
@RequestMapping("/api/motoboy")
@RequiredArgsConstructor
public class BankAccountController {
    
    private final IuguService iuguService;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    
    // ... métodos
}
```

---

## 📦 Caso de Uso 1: Cadastrar Dados Bancários do Motoboy

### Controller

```java
@PostMapping("/bank-data")
@PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
public ResponseEntity<BankDataResponse> registerBankData(
        @RequestBody @Valid BankDataRequest request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Busca usuário autenticado
    User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    
    // 2. Valida se já tem conta bancária
    if (user.getBankDataComplete()) {
        throw new BusinessException("Usuário já possui dados bancários cadastrados");
    }
    
    // 3. Cria BankAccount
    BankAccount bankAccount = BankAccount.builder()
            .user(user)
            .bankCode(request.getBankCode())
            .bankName(BrazilianBanks.getBankName(request.getBankCode()))
            .agency(request.getAgency())
            .accountNumber(request.getAccountNumber())
            .accountType(request.getAccountType())
            .status(BankAccountStatus.PENDING_VALIDATION)
            .build();
    
    // 4. Salva no banco primeiro
    bankAccount = bankAccountRepository.save(bankAccount);
    
    // 5. Cria subconta no Iugu
    try {
        SubAccountResponse iuguResponse = iuguService.createSubAccount(user, bankAccount);
        
        // 6. Atualiza User com iuguAccountId
        user.setIuguAccountId(iuguResponse.accountId());
        user.setBankDataComplete(true);
        user.setAutoWithdrawEnabled(iuguResponse.autoWithdraw());
        userRepository.save(user);
        
        // 7. Atualiza status do BankAccount
        if (iuguResponse.canReceivePayments()) {
            bankAccount.markAsActive();
        }
        bankAccountRepository.save(bankAccount);
        
        // 8. Retorna sucesso
        return ResponseEntity.ok(BankDataResponse.builder()
                .message("Dados bancários cadastrados com sucesso!")
                .iuguAccountId(iuguResponse.accountId())
                .verificationStatus(iuguResponse.verificationStatus())
                .canReceivePayments(iuguResponse.canReceivePayments())
                .build());
        
    } catch (IuguService.IuguApiException e) {
        // Rollback: remove BankAccount criado
        bankAccountRepository.delete(bankAccount);
        throw new IuguIntegrationException("Erro ao criar subconta no Iugu: " + e.getMessage(), e);
    }
}
```

### DTOs

```java
// Request
@Data
public class BankDataRequest {
    @NotBlank @Pattern(regexp = "\\d{3}")
    private String bankCode;
    
    @NotBlank
    private String agency;
    
    @NotBlank
    private String accountNumber;
    
    @NotNull
    private AccountType accountType;
}

// Response
@Data
@Builder
public class BankDataResponse {
    private String message;
    private String iuguAccountId;
    private String verificationStatus;
    private Boolean canReceivePayments;
}
```

---

## 💳 Caso de Uso 2: Criar Pagamento PIX com Split

### Controller

```java
@PostMapping("/deliveries/{deliveryId}/payment")
@PreAuthorize("hasRole('CLIENT')")
public ResponseEntity<PaymentResponse> createPayment(
        @PathVariable UUID deliveryId,
        @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Busca entrega
    Delivery delivery = deliveryService.findById(deliveryId)
            .orElseThrow(() -> new DeliveryNotFoundException("Entrega não encontrada"));
    
    // 2. Valida se cliente é dono da entrega
    if (!delivery.getClient().getUsername().equals(userDetails.getUsername())) {
        throw new ForbiddenException("Você não pode pagar esta entrega");
    }
    
    // 3. Valida se entrega já foi paga
    if (delivery.getPaymentStatus() == PaymentStatus.PAID) {
        throw new BusinessException("Entrega já foi paga");
    }
    
    // 4. Valida se motoboy e gerente têm subcontas
    User courier = delivery.getCourier();
    User manager = delivery.getManager();
    
    if (!courier.canReceivePayments()) {
        throw new BusinessException("Motoboy não pode receber pagamentos (dados bancários incompletos)");
    }
    if (!manager.canReceivePayments()) {
        throw new BusinessException("Gerente não pode receber pagamentos (dados bancários incompletos)");
    }
    
    // 5. Cria invoice no Iugu
    try {
        InvoiceResponse invoice = iuguService.createInvoiceWithSplit(
                deliveryId.toString(),
                delivery.getTotalAmount(),
                delivery.getClient().getUsername(), // email
                courier.getIuguAccountId(),
                manager.getIuguAccountId()
        );
        
        // 6. Cria Payment no banco
        Payment payment = Payment.builder()
                .delivery(delivery)
                .amount(delivery.getTotalAmount())
                .paymentMethod(PaymentMethod.PIX)
                .status(PaymentStatus.PENDING)
                .iuguInvoiceId(invoice.id())
                .pixQrCode(invoice.pixQrCode())
                .pixQrCodeUrl(invoice.pixQrCodeUrl())
                .expiresAt(parseExpiryDate(invoice.dueDate()))
                .courierAmount(calculateCourierAmount(delivery.getTotalAmount()))
                .admAmount(calculateManagerAmount(delivery.getTotalAmount()))
                .platformAmount(calculatePlatformAmount(delivery.getTotalAmount()))
                .build();
        
        paymentRepository.save(payment);
        
        // 7. Atualiza status da entrega
        delivery.setPaymentStatus(PaymentStatus.PENDING);
        deliveryRepository.save(delivery);
        
        // 8. Retorna response com QR Code
        return ResponseEntity.ok(PaymentResponse.builder()
                .paymentId(payment.getId())
                .invoiceId(invoice.id())
                .pixQrCode(invoice.pixQrCode())
                .pixQrCodeUrl(invoice.pixQrCodeUrl())
                .amount(delivery.getTotalAmount())
                .expiresAt(payment.getExpiresAt())
                .secureUrl(invoice.secureUrl())
                .build());
        
    } catch (IuguService.IuguApiException e) {
        throw new IuguIntegrationException("Erro ao criar pagamento PIX: " + e.getMessage(), e);
    }
}

// Helpers para calcular valores
private BigDecimal calculateCourierAmount(BigDecimal total) {
    return total.multiply(BigDecimal.valueOf(0.87)).setScale(2, RoundingMode.HALF_UP);
}

private BigDecimal calculateManagerAmount(BigDecimal total) {
    return total.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
}

private BigDecimal calculatePlatformAmount(BigDecimal total) {
    return total.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
}

private LocalDateTime parseExpiryDate(String dueDateStr) {
    // Converte DD/MM/YYYY para LocalDateTime (23:59:59 do dia)
    LocalDate dueDate = LocalDate.parse(dueDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    return dueDate.atTime(23, 59, 59);
}
```

### Response

```java
@Data
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private String invoiceId;
    private String pixQrCode;      // Código PIX (texto para copiar)
    private String pixQrCodeUrl;   // URL da imagem do QR Code
    private BigDecimal amount;
    private LocalDateTime expiresAt;
    private String secureUrl;      // URL de pagamento (alternativa)
}
```

---

## 🔔 Caso de Uso 3: Webhook - Processar Pagamento Confirmado

### Controller

```java
@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class IuguWebhookController {
    
    private final IuguService iuguService;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    
    @PostMapping("/iugu")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            @RequestBody WebhookEvent event,
            @RequestHeader(value = "X-Iugu-Signature", required = false) String signature
    ) {
        log.info("🔔 Webhook recebido: {}", event.event());
        
        // 1. Valida assinatura
        if (!iuguService.validateWebhookSignature(signature)) {
            log.warn("❌ Webhook com assinatura inválida: {}", signature);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("received", false));
        }
        
        // 2. Processa evento
        try {
            if (event.isPaymentConfirmed()) {
                handlePaymentConfirmed(event);
            } else if (event.isWithdrawalCompleted()) {
                handleWithdrawalCompleted(event);
            } else if (event.isRefunded()) {
                handleRefund(event);
            } else if (event.isCanceled() || event.isExpired()) {
                handleCancelOrExpire(event);
            } else {
                log.info("⏭️ Evento ignorado: {}", event.event());
            }
            
            return ResponseEntity.ok(Map.of("received", true));
            
        } catch (Exception e) {
            log.error("❌ Erro ao processar webhook: {}", e.getMessage(), e);
            // Retorna 200 para Iugu não reenviar
            return ResponseEntity.ok(Map.of("received", false));
        }
    }
    
    private void handlePaymentConfirmed(WebhookEvent event) {
        String invoiceId = event.getInvoiceId();
        log.info("💰 Pagamento confirmado: invoice {}", invoiceId);
        
        // 1. Busca Payment pelo iuguInvoiceId
        Payment payment = paymentRepository.findByIuguInvoiceId(invoiceId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment não encontrado: " + invoiceId));
        
        // 2. Atualiza status
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // 3. Atualiza Delivery
        Delivery delivery = payment.getDelivery();
        delivery.setPaymentStatus(PaymentStatus.PAID);
        deliveryRepository.save(delivery);
        
        // 4. Notifica motoboy e gerente
        notificationService.notifyPaymentConfirmed(delivery);
        
        log.info("✅ Pagamento processado: delivery {} pago com sucesso", delivery.getId());
    }
    
    private void handleWithdrawalCompleted(WebhookEvent event) {
        String accountId = event.getAccountId();
        log.info("🏦 Transferência bancária concluída: account {}", accountId);
        
        // Notifica usuário que o dinheiro foi transferido (D+1)
        notificationService.notifyWithdrawalCompleted(accountId);
    }
    
    private void handleRefund(WebhookEvent event) {
        String invoiceId = event.getInvoiceId();
        log.info("↩️ Reembolso processado: invoice {}", invoiceId);
        
        Payment payment = paymentRepository.findByIuguInvoiceId(invoiceId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment não encontrado"));
        
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // Atualiza Delivery
        payment.getDelivery().setPaymentStatus(PaymentStatus.REFUNDED);
        deliveryRepository.save(payment.getDelivery());
    }
    
    private void handleCancelOrExpire(WebhookEvent event) {
        String invoiceId = event.getInvoiceId();
        log.info("❌ Invoice cancelada/expirada: {}", invoiceId);
        
        Payment payment = paymentRepository.findByIuguInvoiceId(invoiceId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment não encontrado"));
        
        PaymentStatus newStatus = event.isCanceled() ? PaymentStatus.CANCELED : PaymentStatus.EXPIRED;
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        
        payment.getDelivery().setPaymentStatus(newStatus);
        deliveryRepository.save(payment.getDelivery());
    }
}
```

---

## 🔄 Caso de Uso 4: Atualizar Dados Bancários

```java
@PutMapping("/bank-data")
@PreAuthorize("hasRole('COURIER')")
public ResponseEntity<String> updateBankData(
        @RequestBody @Valid BankDataRequest request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    // 1. Busca usuário
    User user = userRepository.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    
    // 2. Valida se tem subconta Iugu
    if (user.getIuguAccountId() == null) {
        throw new BusinessException("Usuário não possui subconta Iugu");
    }
    
    // 3. Busca BankAccount
    BankAccount bankAccount = user.getBankAccount();
    if (bankAccount == null) {
        throw new BusinessException("Usuário não possui conta bancária");
    }
    
    // 4. Atualiza campos
    bankAccount.setBankCode(request.getBankCode());
    bankAccount.setBankName(BrazilianBanks.getBankName(request.getBankCode()));
    bankAccount.setAgency(request.getAgency());
    bankAccount.setAccountNumber(request.getAccountNumber());
    bankAccount.setAccountType(request.getAccountType());
    bankAccount.setStatus(BankAccountStatus.PENDING_VALIDATION);
    
    // 5. Atualiza no Iugu
    try {
        iuguService.updateBankAccount(user.getIuguAccountId(), bankAccount);
        
        // 6. Salva no banco
        bankAccountRepository.save(bankAccount);
        
        return ResponseEntity.ok("Dados bancários atualizados com sucesso!");
        
    } catch (IuguService.IuguApiException e) {
        throw new IuguIntegrationException("Erro ao atualizar dados no Iugu: " + e.getMessage(), e);
    }
}
```

---

## ⚠️ Tratamento de Erros

### Exception Handler Global

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IuguService.IuguApiException.class)
    public ResponseEntity<ErrorResponse> handleIuguApiException(IuguService.IuguApiException ex) {
        log.error("Erro na API Iugu: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.builder()
                        .error("IUGU_API_ERROR")
                        .message("Erro ao comunicar com o gateway de pagamentos")
                        .details(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    
    @ExceptionHandler(IuguIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIuguIntegrationException(IuguIntegrationException ex) {
        log.error("Erro de integração Iugu: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .error("IUGU_INTEGRATION_ERROR")
                        .message("Erro ao processar pagamento")
                        .details(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}

// Custom exception
public class IuguIntegrationException extends RuntimeException {
    public IuguIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 📊 Exemplo de Logs

```
2025-12-02 14:30:15.123 INFO  [IuguService] Criando subconta Iugu para usuário: joao_motoboy (uuid-123)
2025-12-02 14:30:15.456 DEBUG [IuguService] POST https://api.iugu.com/v1/marketplace/create_account - Criando subconta para joao@example.com
2025-12-02 14:30:16.789 INFO  [IuguService] ✅ Subconta criada com sucesso: acc_ABC123 (status: pending)

2025-12-02 15:45:30.111 INFO  [IuguService] Criando fatura PIX para entrega delivery-456 - Valor: R$ 50.00
2025-12-02 15:45:30.222 DEBUG [IuguService] POST https://api.iugu.com/v1/invoices - Criando invoice com splits
2025-12-02 15:45:31.333 INFO  [IuguService] ✅ Fatura criada com sucesso: inv_XYZ789 (PIX QR Code gerado)
2025-12-02 15:45:31.444 DEBUG [IuguService]    └─ Splits: 87.0% motoboy (acc_ABC123), 5.0% gerente (acc_DEF456), 8.0% plataforma

2025-12-02 16:00:00.555 INFO  [IuguWebhookController] 🔔 Webhook recebido: invoice.paid
2025-12-02 16:00:00.666 DEBUG [IuguService] ✅ Assinatura do webhook validada
2025-12-02 16:00:00.777 INFO  [IuguWebhookController] 💰 Pagamento confirmado: invoice inv_XYZ789
2025-12-02 16:00:00.888 INFO  [IuguWebhookController] ✅ Pagamento processado: delivery delivery-456 pago com sucesso
```

---

## 🎯 Resumo

**IuguService fornece 4 métodos públicos:**

1. ✅ `createSubAccount(User, BankAccount)` → Criar subconta
2. ✅ `updateBankAccount(accountId, BankAccount)` → Atualizar dados
3. ✅ `createInvoiceWithSplit(...)` → Criar fatura PIX com splits
4. ✅ `validateWebhookSignature(signature)` → Validar webhook

**Padrão de uso:**
1. Validar dados de entrada
2. Chamar método do IuguService
3. Tratar `IuguApiException` se falhar
4. Salvar resultado no banco de dados
5. Notificar usuário (opcional)

**Boas práticas:**
- ✅ Sempre validar se User/BankAccount podem receber pagamentos
- ✅ Fazer rollback em caso de erro na API Iugu
- ✅ Logar todas as operações
- ✅ Tratar erros de forma amigável ao usuário
- ✅ Implementar idempotência em webhooks

---

**Mantido por**: Equipe de Backend  
**Última atualização**: 2025-12-02
