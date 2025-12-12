# 💻 Pagar.me - Exemplos de Código Java

**Data:** 9 de dezembro de 2025  
**Versão:** Draft v1.0

---

## 📦 DTOs Pagar.me

### 1. RecipientRequest.java

```java
package com.mvt.mvt_events.payment.dto.pagarme;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
public class RecipientRequest {
    
    private String name;
    private String email;
    private String description;
    private String document;
    private String type; // "individual" ou "company"
    
    @JsonProperty("default_bank_account")
    private BankAccountRequest defaultBankAccount;
    
    @JsonProperty("transfer_settings")
    private TransferSettings transferSettings;
    
    @JsonProperty("automatic_anticipation_settings")
    private AutomaticAnticipationSettings automaticAnticipationSettings;
    
    @Data
    @Builder
    public static class BankAccountRequest {
        @JsonProperty("holder_name")
        private String holderName;
        
        @JsonProperty("holder_type")
        private String holderType; // "individual" ou "company"
        
        @JsonProperty("holder_document")
        private String holderDocument;
        
        private String bank; // Código do banco (3 dígitos)
        
        @JsonProperty("branch_number")
        private String branchNumber; // Agência
        
        @JsonProperty("branch_check_digit")
        private String branchCheckDigit; // Dígito da agência
        
        @JsonProperty("account_number")
        private String accountNumber; // Conta
        
        @JsonProperty("account_check_digit")
        private String accountCheckDigit; // Dígito da conta
        
        private String type; // "checking" ou "savings"
    }
    
    @Data
    @Builder
    public static class TransferSettings {
        @JsonProperty("transfer_enabled")
        private Boolean transferEnabled;
        
        @JsonProperty("transfer_interval")
        private String transferInterval; // "daily", "weekly", "monthly"
        
        @JsonProperty("transfer_day")
        private Integer transferDay;
    }
    
    @Data
    @Builder
    public static class AutomaticAnticipationSettings {
        private Boolean enabled;
    }
}
```

### 2. SplitRequest.java

```java
package com.mvt.mvt_events.payment.dto.pagarme;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
public class SplitRequest {
    
    private Integer amount; // Percentual (0-100) ou valor em centavos
    private String type; // "percentage" ou "flat"
    
    @JsonProperty("recipient_id")
    private String recipientId;
    
    private SplitOptions options;
    
    @Data
    @Builder
    public static class SplitOptions {
        private Boolean liable; // Responsável por chargeback
        
        @JsonProperty("charge_processing_fee")
        private Boolean chargeProcessingFee; // Cobra taxa de processamento
        
        @JsonProperty("charge_remainder_fee")
        private Boolean chargeRemainderFee; // Recebe resto de arredondamento
    }
}
```

### 3. OrderRequest.java

```java
package com.mvt.mvt_events.payment.dto.pagarme;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
public class OrderRequest {
    
    private CustomerRequest customer;
    private List<ItemRequest> items;
    private List<PaymentRequest> payments;
    
    @Data
    @Builder
    public static class CustomerRequest {
        private String name;
        private String email;
        private String document;
        
        @JsonProperty("document_type")
        private String documentType; // "CPF" ou "CNPJ"
        
        private String type; // "individual" ou "company"
    }
    
    @Data
    @Builder
    public static class ItemRequest {
        private Integer amount; // Valor em centavos
        private String description;
        private Integer quantity;
        private String code; // Código único do item
    }
    
    @Data
    @Builder
    public static class PaymentRequest {
        @JsonProperty("payment_method")
        private String paymentMethod; // "pix"
        
        private PixRequest pix;
        private List<SplitRequest> split;
    }
    
    @Data
    @Builder
    public static class PixRequest {
        @JsonProperty("expires_in")
        private Integer expiresIn; // Tempo de expiração em segundos
        
        @JsonProperty("additional_information")
        private List<AdditionalInfo> additionalInformation;
    }
    
    @Data
    @Builder
    public static class AdditionalInfo {
        private String name;
        private String value;
    }
}
```

---

## 🔧 PagarMeService.java (Skeleton)

```java
package com.mvt.mvt_events.payment.service;

import com.mvt.mvt_events.config.PagarMeConfig;
import com.mvt.mvt_events.jpa.BankAccount;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.pagarme.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Serviço de integração com Pagar.me
 * 
 * Funcionalidades:
 * - Criar recipients (subcontas)
 * - Criar orders com PIX e split
 * - Validar webhooks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagarMeService {
    
    private final RestTemplate pagarMeRestTemplate;
    private final PagarMeConfig pagarMeConfig;
    
    /**
     * Cria um recipient (subconta) no Pagar.me
     * 
     * @param user Usuário (courier ou manager)
     * @param bankAccount Conta bancária
     * @return RecipientResponse com recipient_id
     */
    public RecipientResponse createRecipient(User user, BankAccount bankAccount) {
        log.info("🚀 Criando recipient Pagar.me: {} ({})", user.getUsername(), user.getId());
        
        // Validações
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username obrigatório");
        }
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Conta bancária deve estar ativa");
        }
        
        // Monta request
        RecipientRequest request = RecipientRequest.builder()
            .name(user.getName())
            .email(user.getUsername())
            .description(user.getRole().name() + " - MVT Events")
            .document(user.getDocumentClean())
            .type("individual")
            .defaultBankAccount(RecipientRequest.BankAccountRequest.builder()
                .holderName(user.getName())
                .holderType("individual")
                .holderDocument(user.getDocumentClean())
                .bank(bankAccount.getBankCode())
                .branchNumber(bankAccount.getAgency().replaceAll("-", ""))
                .branchCheckDigit(extractBranchCheckDigit(bankAccount.getAgency()))
                .accountNumber(extractAccountNumber(bankAccount.getAccountNumber()))
                .accountCheckDigit(extractAccountCheckDigit(bankAccount.getAccountNumber()))
                .type(bankAccount.getAccountType().getPagarMeValue())
                .build())
            .transferSettings(RecipientRequest.TransferSettings.builder()
                .transferEnabled(true)
                .transferInterval("daily")
                .transferDay(1) // D+1
                .build())
            .automaticAnticipationSettings(RecipientRequest.AutomaticAnticipationSettings.builder()
                .enabled(false)
                .build())
            .build();
        
        try {
            String url = pagarMeConfig.getApi().getUrl() + "/recipients";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<RecipientRequest> entity = new HttpEntity<>(request, headers);
            
            log.debug("POST {} - Criando recipient", url);
            ResponseEntity<RecipientResponse> response = pagarMeRestTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                RecipientResponse.class
            );
            
            RecipientResponse recipient = response.getBody();
            if (recipient == null || recipient.getId() == null) {
                throw new PagarMeApiException("Resposta inválida: " + response);
            }
            
            log.info("✅ Recipient criado: {}", recipient.getId());
            return recipient;
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar recipient", e);
            throw new PagarMeApiException("Erro ao criar recipient: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cria uma order com PIX e split de pagamento
     * 
     * @param deliveryId ID da entrega
     * @param amountCents Valor em centavos
     * @param clientEmail Email do cliente
     * @param courierRecipientId Recipient ID do courier
     * @param managerRecipientId Recipient ID do manager
     * @return OrderResponse com QR Code PIX
     */
    public OrderResponse createOrderWithSplit(
            String deliveryId,
            int amountCents,
            String clientEmail,
            String courierRecipientId,
            String managerRecipientId
    ) {
        log.info("💰 Criando order com split - Delivery: {}, Valor: R$ {}", 
            deliveryId, amountCents / 100.0);
        
        // Monta splits (87% courier, 5% manager, 8% fica com platform)
        List<SplitRequest> splits = List.of(
            SplitRequest.builder()
                .amount(87)
                .type("percentage")
                .recipientId(courierRecipientId)
                .options(SplitRequest.SplitOptions.builder()
                    .liable(false)
                    .chargeProcessingFee(false)
                    .chargeRemainderFee(false)
                    .build())
                .build(),
            SplitRequest.builder()
                .amount(5)
                .type("percentage")
                .recipientId(managerRecipientId)
                .options(SplitRequest.SplitOptions.builder()
                    .liable(false)
                    .chargeProcessingFee(false)
                    .chargeRemainderFee(false)
                    .build())
                .build()
            // Platform (8%) fica automaticamente na conta principal
        );
        
        // Monta order request
        OrderRequest request = OrderRequest.builder()
            .customer(OrderRequest.CustomerRequest.builder()
                .email(clientEmail)
                .name("Cliente " + clientEmail)
                .build())
            .items(List.of(OrderRequest.ItemRequest.builder()
                .amount(amountCents)
                .description("Entrega " + deliveryId)
                .quantity(1)
                .code(deliveryId)
                .build()))
            .payments(List.of(OrderRequest.PaymentRequest.builder()
                .paymentMethod("pix")
                .pix(OrderRequest.PixRequest.builder()
                    .expiresIn(86400) // 24 horas
                    .additionalInformation(List.of(
                        OrderRequest.AdditionalInfo.builder()
                            .name("Delivery ID")
                            .value(deliveryId)
                            .build()
                    ))
                    .build())
                .split(splits)
                .build()))
            .build();
        
        try {
            String url = pagarMeConfig.getApi().getUrl() + "/orders";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<OrderRequest> entity = new HttpEntity<>(request, headers);
            
            log.debug("POST {} - Criando order", url);
            ResponseEntity<OrderResponse> response = pagarMeRestTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                OrderResponse.class
            );
            
            OrderResponse order = response.getBody();
            if (order == null || order.getId() == null) {
                throw new PagarMeApiException("Resposta inválida: " + response);
            }
            
            log.info("✅ Order criada: {}", order.getId());
            return order;
            
        } catch (Exception e) {
            log.error("❌ Erro ao criar order", e);
            throw new PagarMeApiException("Erro ao criar order: " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida assinatura de webhook usando HMAC SHA256
     * 
     * @param signature Header x-hub-signature
     * @param payload Body do webhook
     * @return true se válido
     */
    public boolean validateWebhookSignature(String signature, String payload) {
        try {
            String secret = pagarMeConfig.getWebhook().getSecret();
            
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            hmac.init(secretKey);
            
            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(hash);
            
            boolean isValid = expectedSignature.equals(signature);
            
            if (isValid) {
                log.debug("✅ Webhook signature válida");
            } else {
                log.warn("❌ Webhook signature inválida");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("❌ Erro ao validar webhook signature", e);
            return false;
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String apiKey = pagarMeConfig.getApi().getKey();
        String auth = Base64.getEncoder().encodeToString(
            (apiKey + ":").getBytes(StandardCharsets.UTF_8)
        );
        
        headers.set("Authorization", "Basic " + auth);
        return headers;
    }
    
    private String extractBranchCheckDigit(String agency) {
        if (agency.contains("-")) {
            return agency.split("-")[1];
        }
        return "0";
    }
    
    private String extractAccountNumber(String account) {
        if (account.contains("-")) {
            return account.split("-")[0];
        }
        return account;
    }
    
    private String extractAccountCheckDigit(String account) {
        if (account.contains("-")) {
            return account.split("-")[1];
        }
        return "0";
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Exception para erros da API Pagar.me
     */
    public static class PagarMeApiException extends RuntimeException {
        public PagarMeApiException(String message) {
            super(message);
        }
        
        public PagarMeApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

---

## 🧪 Exemplo de Uso

```java
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PagarMeService pagarMeService;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    
    @PostMapping("/create-pix-split")
    public ResponseEntity<PaymentResponse> createPixWithSplit(
            @RequestBody PaymentRequest request
    ) {
        // Busca entrega
        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
            .orElseThrow(() -> new IllegalArgumentException("Delivery não encontrada"));
        
        // Busca courier e manager
        User courier = delivery.getCourier();
        User manager = delivery.getOrganization().getOwner();
        
        // Valida que têm recipient_id
        if (courier.getRecipientId() == null) {
            throw new IllegalStateException("Courier não tem recipient cadastrado");
        }
        if (manager.getRecipientId() == null) {
            throw new IllegalStateException("Manager não tem recipient cadastrado");
        }
        
        // Cria order no Pagar.me
        OrderResponse order = pagarMeService.createOrderWithSplit(
            delivery.getId().toString(),
            delivery.getShippingFee().multiply(new BigDecimal(100)).intValue(),
            delivery.getClient().getUsername(),
            courier.getRecipientId(),
            manager.getRecipientId()
        );
        
        // Salva payment local
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmount(delivery.getShippingFee());
        payment.setPixQrCode(order.getPixQrCode());
        payment.setPixQrCodeUrl(order.getPixQrCodeUrl());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setDelivery(delivery);
        paymentRepository.save(payment);
        
        // Retorna response
        return ResponseEntity.ok(PaymentResponse.fromPayment(payment));
    }
}
```

---

## 📊 Comparação Final

| Feature | Iugu | Pagar.me |
|---------|------|----------|
| Split percentual | ✅ 87/5/8 | ✅ 87/5 (8% automático) |
| PIX QR Code | ✅ | ✅ |
| Transfer D+1 | ✅ | ✅ |
| Webhook | ✅ Custom token | ✅ HMAC SHA256 |
| Validação bancária | ✅ Automática | ✅ Automática |
| Taxa Pagar.me | ~R$ 0,59 | ~2% do valor |

---

**Próximo passo:** Implementar `PagarMeConfig.java` e criar migrations.
