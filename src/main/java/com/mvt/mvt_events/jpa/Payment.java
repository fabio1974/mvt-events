package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um pagamento de múltiplas entregas.
 * 
 * Relacionamentos:
 * - N:M com Delivery (um pagamento pode cobrir várias entregas, e uma entrega pode ter múltiplos payments históricos)
 * - N:1 com User (payer - quem está pagando)
 * 
 * IMPORTANTE: Embora seja N:M, na prática uma delivery deve ter apenas 1 payment PAID.
 * Múltiplos payments para mesma delivery representam histórico de tentativas (EXPIRED, CANCELLED, etc).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Payment extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Ao menos uma entrega é obrigatória")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "payment_deliveries",
        joinColumns = @JoinColumn(name = "payment_id"),
        inverseJoinColumns = @JoinColumn(name = "delivery_id")
    )
    @JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private List<Delivery> deliveries = new ArrayList<>();

    @NotNull(message = "Pagador é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private Organization organization;

    // ============================================================================
    // PAYMENT INFO
    // ============================================================================

    @DisplayLabel
    @Column(name = "transaction_id", length = 100, unique = true)
    @Visible(table = true, form = false, filter = true)
    private String transactionId;

    @DisplayLabel
    @NotNull(message = "Valor é obrigatório")
    @Column(nullable = false, precision = 10, scale = 2)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal amount;

    @NotNull(message = "Moeda é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3, nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Currency currency = Currency.BRL; // Moeda padrão: Real Brasileiro

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Visible(table = true, form = true, filter = true)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Visible(table = true, form = true, filter = true)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_date")
    @Visible(table = true, form = false, filter = true)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 50)
    @Visible(table = false, form = false, filter = false)
    private PaymentProvider provider; // Gateway de pagamento (Pagar.me, Stripe, etc)

    @Column(name = "provider_payment_id", length = 100)
    @Visible(table = false, form = false, filter = false)
    private String providerPaymentId;

    // ============================================================================
    // METADATA
    // ============================================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String metadata;

    @Column(name = "expires_at")
    @Visible(table = true, form = false, filter = true)
    private LocalDateTime expiresAt;

    // ============================================================================
    // PAGAR.ME PIX FIELDS
    // ============================================================================

    /**
     * PIX QR Code (copia e cola)
     */
    @Column(name = "pix_qr_code", columnDefinition = "TEXT")
    @Visible(table = false, form = false, filter = false)
    private String pixQrCode;

    /**
     * URL da imagem do PIX QR Code
     */
    @Column(name = "pix_qr_code_url", columnDefinition = "TEXT")
    @Visible(table = false, form = false, filter = false)
    private String pixQrCodeUrl;

    /**
     * QR Code extraído de charges[0].last_transaction.qr_code do response Pagar.me
     */
    @Column(name = "qr_code", columnDefinition = "TEXT")
    @Visible(table = false, form = false, filter = false)
    private String qrCode;

    /**
     * URL do QR Code extraído de charges[0].last_transaction.qr_code_url do response Pagar.me
     */
    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    @Visible(table = false, form = false, filter = false)
    private String qrCodeUrl;

    /**
     * Regras de split em formato JSON
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "split_rules", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String splitRules;

    /**
     * Extrato específico do gateway_response retornado pelo Pagar.me
     * Contém apenas a parte "gateway_response" do response completo,
     * que inclui códigos de erro e mensagens de validação.
     * Exemplo: {"code": "400", "errors": [{"message": "At least one customer phone is required."}]}
     * Deixar vazio se a chave "gateway_response" não existir no response.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String gatewayResponse;

    /**
     * Request completo enviado para criar o pagamento no gateway (Pagar.me, Iugu, etc.)
     * Armazena o payload completo da requisição enviada ao gateway de pagamento
     * para fins de auditoria e debugging.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "request", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String request;

    /**
     * Response completo retornado pelo gateway de pagamento (Pagar.me, Iugu, etc.)
     * Armazena o payload completo da resposta recebida do gateway após criar o pagamento.
     * Inclui todos os dados: order ID, status, charges, PIX data, timestamps, etc.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "response", columnDefinition = "JSONB")
    @Visible(table = false, form = false, filter = false)
    private String response;

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Adiciona uma delivery ao pagamento
     */
    public void addDelivery(Delivery delivery) {
        if (this.deliveries == null) {
            this.deliveries = new ArrayList<>();
        }
        this.deliveries.add(delivery);
    }

    /**
     * Remove uma delivery do pagamento
     */
    public void removeDelivery(Delivery delivery) {
        if (this.deliveries != null) {
            this.deliveries.remove(delivery);
        }
    }

    /**
     * Retorna o número de deliveries neste pagamento
     */
    public int getDeliveriesCount() {
        return deliveries != null ? deliveries.size() : 0;
    }

    /**
     * Calcula o valor total das deliveries (deve ser igual ao amount)
     */
    public BigDecimal calculateTotalFromDeliveries() {
        if (deliveries == null || deliveries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return deliveries.stream()
            .map(Delivery::getTotalAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED;
    }

    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Pagamento não pode ser reembolsado no status: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public void markAsCancelled() {
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Pagamento concluído não pode ser cancelado. Use refund.");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    // ============================================================================
    // PAGAR.ME PIX HELPER METHODS
    // ============================================================================

    /**
     * Verifica se o pagamento PIX expirou
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica se é um pagamento via Pagar.me
     */
    public boolean isPagarmePayment() {
        return providerPaymentId != null && !providerPaymentId.isBlank() && provider == PaymentProvider.PAGARME;
    }

    /**
     * Verifica se tem PIX QR Code disponível
     */
    public boolean hasPixQrCode() {
        return pixQrCode != null && !pixQrCode.isBlank();
    }

    /**
     * Calcula o valor que o motoboy deve receber (87% do total)
     */
    public BigDecimal getMotoboyShare() {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(new BigDecimal("0.87")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor que o gestor deve receber (5% do total)
     */
    public BigDecimal getManagerShare() {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o valor que a plataforma recebe (8% do total)
     */
    public BigDecimal getPlatformShare() {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
    }
}
