package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.CustomerCard;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.payment.dto.BillingAddressDTO;
import com.mvt.mvt_events.service.CustomerCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar cartões de crédito tokenizados dos clientes.
 * 
 * SEGURANÇA:
 * - Apenas o próprio cliente ou admin pode gerenciar seus cartões
 * - NUNCA recebe número completo do cartão (apenas token do Pagar.me)
 * - Tokenização feita no frontend com Pagar.me JS
 */
@RestController
@RequestMapping("/api/customer-cards")
@CrossOrigin(origins = "*")
@Tag(name = "Cartões de Crédito", description = "Gerenciamento de cartões tokenizados dos clientes")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class CustomerCardController {

    private final CustomerCardService cardService;

    @PostMapping
    @Operation(summary = "Adicionar cartão de crédito", 
               description = "Adiciona um novo cartão. O frontend deve criar o token no Pagar.me primeiro e enviar apenas o token.")
    public ResponseEntity<CardResponse> addCard(@RequestBody @Valid AddCardRequest request, Authentication authentication) {
        try {
            // Extrair customerId do token JWT
            UUID customerId = extractCustomerId(authentication);

            // Adicionar cartão com billing address (se fornecido)
            CustomerCard card = cardService.addCard(
                customerId, 
                request.getCardToken(), 
                request.getSetAsDefault(),
                request.getBillingAddress()
            );

            return ResponseEntity.ok(new CardResponse(card));
        } catch (RuntimeException e) {
            log.error("❌ Erro ao adicionar cartão: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping
    @Operation(summary = "Listar meus cartões", 
               description = "Lista todos os cartões ativos do cliente logado. Ordenados por: padrão primeiro, depois último uso.")
    public ResponseEntity<List<CardResponse>> listMyCards(Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        List<CustomerCard> cards = cardService.listCustomerCards(customerId);
        List<CardResponse> response = cards.stream()
                .map(CardResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/default")
    @Operation(summary = "Buscar cartão padrão", 
               description = "Retorna o cartão padrão do cliente (usado por default em pagamentos).")
    public ResponseEntity<CardResponse> getDefaultCard(Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            CustomerCard card = cardService.getDefaultCard(customerId);
            return ResponseEntity.ok(new CardResponse(card));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{cardId}/set-default")
    @Operation(summary = "Definir cartão como padrão", 
               description = "Define um cartão como padrão. Remove flag de todos os outros cartões do cliente.")
    public ResponseEntity<CardResponse> setDefaultCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            CustomerCard card = cardService.setDefaultCard(customerId, cardId, authentication);
            return ResponseEntity.ok(new CardResponse(card));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Deletar cartão", 
               description = "Remove um cartão (soft delete). Mantém no banco para auditoria.")
    public ResponseEntity<?> deleteCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            cardService.deleteCard(customerId, cardId, authentication);
            return ResponseEntity.ok(Map.of("message", "Cartão removido com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/has-cards")
    @Operation(summary = "Verificar se tem cartões", 
               description = "Verifica se o cliente possui cartões cadastrados.")
    public ResponseEntity<Map<String, Boolean>> hasCards(Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        boolean hasCards = cardService.hasActiveCards(customerId);
        return ResponseEntity.ok(Map.of("hasCards", hasCards));
    }

    @PostMapping("/retry-unpaid-deliveries")
    @Operation(summary = "Retry pagamento de entregas não pagas", 
               description = "Busca todas as entregas do cliente logado com status IN_TRANSIT ou COMPLETED " +
                             "que ainda não foram pagas, e cria um pagamento para cada uma usando o cartão padrão atual. " +
                             "Evita duplicatas verificando pagamentos já existentes.")
    public ResponseEntity<Map<String, Object>> retryUnpaidDeliveries(Authentication authentication) {
        try {
            UUID customerId = extractCustomerId(authentication);
            Map<String, Object> result = cardService.retryUnpaidDeliveries(customerId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // DTOs
    // ============================================================================

    @Data
    public static class AddCardRequest {
        @NotBlank(message = "Token do cartão é obrigatório")
        private String cardToken;

        private Boolean setAsDefault = false;
        
        /**
         * Endereço de cobrança (opcional).
         * Se fornecido, será enviado ao Pagar.me ao criar o cartão.
         * NÃO é persistido no banco - apenas passthrough para o Pagar.me.
         */
        @Valid
        private BillingAddressDTO billingAddress;
    }

    @Data
    public static class CardResponse {
        private Long id;
        private String lastFourDigits;
        private String brand;
        private String holderName;
        private String expiration; // Formato: MM/YY
        private Boolean isDefault;
        private Boolean isActive;
        private Boolean isExpired;
        private String maskedNumber; // Ex: "Visa **** 4242"
        private String createdAt;
        private String lastUsedAt;

        public CardResponse(CustomerCard card) {
            this.id = card.getId();
            this.lastFourDigits = card.getLastFourDigits();
            this.brand = card.getBrand().getDisplayName();
            this.holderName = card.getHolderName();
            this.expiration = card.getExpirationDisplay();
            this.isDefault = card.getIsDefault();
            this.isActive = card.getIsActive();
            this.isExpired = card.isExpired();
            this.maskedNumber = card.getMaskedNumber();
            this.createdAt = card.getCreatedAt() != null ? card.getCreatedAt().toString() : null;
            this.lastUsedAt = card.getLastUsedAt() != null ? card.getLastUsedAt().toString() : null;
        }
    }

    // ============================================================================
    // HELPER
    // ============================================================================

    /**
     * Extrai customerId do token JWT.
     * No JWT está como "userId" no claim.
     */
    private UUID extractCustomerId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
}
