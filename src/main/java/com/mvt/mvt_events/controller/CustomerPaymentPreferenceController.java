package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.CustomerPaymentPreference;
import com.mvt.mvt_events.jpa.CustomerPaymentPreference.PreferredPaymentType;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.CustomerPaymentPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller para gerenciar preferências de pagamento dos clientes.
 * 
 * Permite que o cliente escolha seu método de pagamento preferido:
 * - PIX: Pagamento via QR Code PIX
 * - CREDIT_CARD: Pagamento via cartão de crédito tokenizado
 */
@RestController
@RequestMapping("/api/customers/me/payment-preference")
@CrossOrigin(origins = "*")
@Tag(name = "Preferências de Pagamento", description = "Gerenciamento de preferências de pagamento do cliente")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentPreferenceController {

    private final CustomerPaymentPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Buscar preferência de pagamento", 
               description = "Retorna a preferência de pagamento atual do cliente. Se não existir, retorna preferênciaPaymentType null.")
    public ResponseEntity<?> getPreference(Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            log.info("🔍 Buscando preferência de pagamento para customerId: {}", customerId);
            
            CustomerPaymentPreference preference = preferenceService.getPreference(customerId);
            
            if (preference == null) {
                log.info("ℹ️ Nenhuma preferência salva para customerId: {}", customerId);
                return ResponseEntity.ok(new PaymentPreferenceResponse());
            }
            
            log.info("✅ Preferência encontrada: type={}, cardId={}", 
                    preference.getPreferredPaymentType(), 
                    preference.getDefaultCard() != null ? preference.getDefaultCard().getId() : null);
            
            return ResponseEntity.ok(new PaymentPreferenceResponse(preference));
        } catch (Exception e) {
            log.error("❌ Erro ao buscar preferência de pagamento: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "FETCH_FAILED",
                "message", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"
            ));
        }
    }

    @PutMapping
    @Operation(summary = "Atualizar preferência de pagamento", 
               description = "Atualiza a preferência de pagamento do cliente. Se escolher CREDIT_CARD, deve informar o cardId.")
    public ResponseEntity<?> updatePreference(
            @RequestBody @Valid UpdatePaymentPreferenceRequest request,
            Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            
            String paymentTypeStr = request.getPaymentType();
            if (paymentTypeStr == null || paymentTypeStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "MISSING_PAYMENT_TYPE",
                    "message", "Tipo de pagamento é obrigatório (preferredPaymentType ou preferredPaymentMethod)"
                ));
            }
            
            PreferredPaymentType paymentType = PreferredPaymentType.valueOf(paymentTypeStr.toUpperCase());
            
            CustomerPaymentPreference preference = preferenceService.savePreference(
                    customerId, 
                    paymentType, 
                    request.getDefaultCardId()
            );
            
            return ResponseEntity.ok(new PaymentPreferenceResponse(preference));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_PAYMENT_TYPE",
                "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "UPDATE_FAILED",
                "message", e.getMessage()
            ));
        }
    }

    @PutMapping("/pix")
    @Operation(summary = "Definir PIX como preferência", 
               description = "Atalho para definir PIX como método de pagamento preferido.")
    public ResponseEntity<PaymentPreferenceResponse> setPixAsPreferred(Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            CustomerPaymentPreference preference = preferenceService.setPixAsPreferred(customerId);
            return ResponseEntity.ok(new PaymentPreferenceResponse(preference));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/credit-card/{cardId}")
    @Operation(summary = "Definir cartão como preferência", 
               description = "Atalho para definir um cartão de crédito específico como método de pagamento preferido.")
    public ResponseEntity<?> setCreditCardAsPreferred(
            @PathVariable Long cardId,
            Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            CustomerPaymentPreference preference = preferenceService.setCreditCardAsPreferred(customerId, cardId);
            return ResponseEntity.ok(new PaymentPreferenceResponse(preference));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_CARD",
                "message", e.getMessage()
            ));
        }
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private UUID extractCustomerId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        throw new RuntimeException("Não foi possível extrair o ID do usuário");
    }

    // ============================================================================
    // DTOs
    // ============================================================================

    @Data
    public static class UpdatePaymentPreferenceRequest {
        // Aceita ambos os nomes: preferredPaymentType ou preferredPaymentMethod
        private String preferredPaymentType; // "PIX" ou "CREDIT_CARD"
        private String preferredPaymentMethod; // Alias para compatibilidade com frontend
        
        private Long defaultCardId; // Obrigatório se preferredPaymentType = CREDIT_CARD

        /**
         * Retorna o tipo de pagamento, aceitando ambos os campos.
         */
        public String getPaymentType() {
            if (preferredPaymentType != null && !preferredPaymentType.isBlank()) {
                return preferredPaymentType;
            }
            return preferredPaymentMethod;
        }
    }

    @Data
    public static class PaymentPreferenceResponse {
        private String preferredPaymentType;
        private Long defaultCardId;
        private String defaultCardLastFour;
        private String defaultCardBrand;
        private boolean hasDefaultCard;

        /**
         * Construtor vazio — sem preferência salva.
         * Retorna todos os campos null/false.
         */
        public PaymentPreferenceResponse() {
            this.preferredPaymentType = null;
            this.hasDefaultCard = false;
        }

        public PaymentPreferenceResponse(CustomerPaymentPreference preference) {
            this.preferredPaymentType = preference.getPreferredPaymentType() != null 
                    ? preference.getPreferredPaymentType().name() 
                    : null;
            
            if (preference.getDefaultCard() != null) {
                this.hasDefaultCard = true;
                this.defaultCardId = preference.getDefaultCard().getId();
                this.defaultCardLastFour = preference.getDefaultCard().getLastFourDigits();
                this.defaultCardBrand = preference.getDefaultCard().getBrand() != null 
                        ? preference.getDefaultCard().getBrand().name() 
                        : null;
            } else {
                this.hasDefaultCard = false;
                this.defaultCardId = null;
                this.defaultCardLastFour = null;
                this.defaultCardBrand = null;
            }
        }
    }
}
