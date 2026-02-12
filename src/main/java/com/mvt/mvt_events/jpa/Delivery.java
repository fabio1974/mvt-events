package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade CORE do Zapi10 - representa uma entrega/delivery.
 * Substitui a entidade Registration do MVT Events.
 * 
 * TENANT: ADM (gerente local) - todas as queries devem filtrar por adm_id via
 * Specifications.
 * O adm_id é denormalizado do ADM principal do courier para performance.
 */
@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Delivery extends BaseEntity {

    // ============================================================================
    // ACTORS (TENANT: ADM)
    // ============================================================================

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User client;



    /**
     * Gerente: dono da organização comum entre courier e client.
     * Setado automaticamente quando courier aceita a entrega.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = false, filter = true)
    private User organizer;

    // ============================================================================
    // ORIGIN (FROM)
    // ============================================================================

    @NotBlank(message = "Endereço de origem é obrigatório")
    @Column(name = "from_address", columnDefinition = "TEXT", nullable = false)
    @Size(max = 500, message = "Endereço de origem deve ter no máximo 500 caracteres")
    @Visible(table = false, form = true, filter = false)
    private String fromAddress;

    @Column(name = "from_lat")
    @Visible(table = false, form = true, filter = false, readonly = true)
    private Double fromLatitude;

    @Column(name = "from_lng")
    @Visible(table = false, form = true, filter = false, readonly = true)
    private Double fromLongitude;

    // ============================================================================
    // DESTINATION (TO)
    // ============================================================================

    @NotBlank(message = "Endereço de destino é obrigatório")
    @Column(name = "to_address", columnDefinition = "TEXT", nullable = false)
    @Size(max = 500, message = "Endereço de destino deve ter no máximo 500 caracteres")
    @Visible(table = true, form = true, filter = false)
    private String toAddress;

    @Column(name = "to_lat")
    @Visible(table = false, form = true, filter = false, readonly = true)
    private Double toLatitude;

    @Column(name = "to_lng")
    @Visible(table = false, form = true, filter = false, readonly = true)
    private Double toLongitude;

    // ============================================================================
    // DELIVERY DETAILS
    // ============================================================================


    @Min(value = 0, message = "Tempo estimado não pode ser negativo")
    @Column(name = "estimated_time_minutes")
    @Visible(table = false, form = false, filter = false)
    private Integer estimatedTimeMinutes;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Column(name = "item_description", length = 500)
    @Visible(table = false, form = true, filter = false)
    private String itemDescription;

    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    @Column(name = "recipient_name", length = 150)
    @Visible(table = false, form = true, filter = false)
    private String recipientName;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "recipient_phone", length = 20)
    @Visible(table = false, form = true, filter = false)
    private String recipientPhone;

    @Column(name = "scheduled_pickup_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime scheduledPickupAt;

    // ============================================================================
    // PRICING
    // ============================================================================

    @DecimalMin(value = "0.0", message = "Valor total não pode ser negativo")
    @Column(name = "total_amount", precision = 10, scale = 2)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal totalAmount;
    
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    @Visible(table = true, form = true, filter = false, readonly = true)
    private BigDecimal shippingFee;

    @DecimalMin(value = "0.0", message = "Distância não pode ser negativa")
    @Column(name = "distance_km", precision = 6, scale = 2)
    @Visible(table = true, form = true, filter = false, readonly = true)
    private BigDecimal distanceKm;


    // ============================================================================
    // STATUS & TIMESTAMPS
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true, readonly = true)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    /**
     * Tipo de entrega: DELIVERY (objeto) ou RIDE (passageiro)
     */
    @NotNull(message = "Tipo de entrega é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private DeliveryType deliveryType = DeliveryType.DELIVERY;

    /**
     * Preferência de veículo do cliente:
     * MOTORCYCLE (moto), CAR (automóvel) ou ANY (sem preferência)
     */
    @NotNull(message = "Preferência de veículo é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_vehicle_type", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private PreferredVehicleType preferredVehicleType = PreferredVehicleType.ANY;

    /**
     * Indica se o pagamento já foi realizado.
     * - Para DELIVERY: true após pagamento quando motoboy aceita
     * - Para RIDE: true após pagamento quando inicia viagem
     */
    @NotNull(message = "Status de pagamento é obrigatório")
    @Column(name = "payment_completed", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private Boolean paymentCompleted = false;

    /**
     * Indica se o pagamento foi capturado/confirmado pelo gateway.
     * Para cartão: muda de false para true quando captura é confirmada.
     */
    @NotNull(message = "Status de captura é obrigatório")
    @Column(name = "payment_captured", nullable = false)
    @Visible(table = false, form = false, filter = true)
    private Boolean paymentCaptured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = true, filter = true, readonly = true)
    private User courier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = true, filter = true, readonly = true)
    private Vehicle vehicle;

    @Column(name = "accepted_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime pickedUpAt;

    @Column(name = "in_transit_at")
    @Visible(table = true, form = false, filter = false)
    private LocalDateTime inTransitAt;

    @Column(name = "completed_at")
    @Visible(table = true, form = false, filter = true)
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    @Size(max = 200, message = "Motivo de cancelamento deve ter no máximo 200 caracteres")
    private String cancellationReason;

    // ============================================================================
    // PAYMENT & EVALUATION
    // ============================================================================

    @ManyToMany(mappedBy = "deliveries", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = false, filter = false)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "delivery", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private Evaluation evaluation;

    // ============================================================================
    // COMPUTED FIELDS
    // ============================================================================
    // ============================================================================

    public Long getActualDeliveryTimeMinutes() {
        if (pickedUpAt == null || completedAt == null) {
            return null;
        }
        return Duration.between(pickedUpAt, completedAt).toMinutes();
    }

    public String getClientName() {
        return client != null ? client.getName() : "N/A";
    }

    public String getCourierName() {
        return courier != null ? courier.getName() : "Não atribuído";
    }

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum DeliveryStatus {
        PENDING, // Aguardando aceitação
        ACCEPTED, // Aceita pelo motoboy
        IN_TRANSIT, // Em trânsito (coletou e está transportando)
        COMPLETED, // Entregue com sucesso
        CANCELLED // Cancelada
    }

    /**
     * Tipo de entrega:
     * - DELIVERY: Entrega de objeto (ex: comida, pacote) - paga quando motoboy aceita
     * - RIDE: Viagem de passageiro (ex: Uber) - paga quando motoboy inicia viagem
     */
    public enum DeliveryType {
        DELIVERY, // Entrega de objeto
        RIDE // Viagem de passageiro
    }

    /**
     * Preferência de veículo para a entrega/viagem:
     * - MOTORCYCLE: Apenas moto
     * - CAR: Apenas automóvel
     * - ANY: Qualquer veículo (sem preferência)
     */
    public enum PreferredVehicleType {
        MOTORCYCLE, // Moto
        CAR,        // Automóvel
        ANY         // Sem preferência
    }

    /**
     * Define quando o pagamento deve ser cobrado
     */
    public enum PaymentTiming {
        ON_ACCEPT, // Paga quando motoboy aceita (DELIVERY)
        ON_TRANSIT_START // Paga quando motoboy inicia viagem (RIDE)
    }

    /**
     * Retorna quando o pagamento deve ser feito baseado no tipo
     */
    public PaymentTiming getPaymentTiming() {
        return deliveryType == DeliveryType.DELIVERY 
            ? PaymentTiming.ON_ACCEPT 
            : PaymentTiming.ON_TRANSIT_START;
    }

    /**
     * Verifica se a delivery foi criada por um CLIENT confiável (estabelecimento).
     * CLIENTs são estabelecimentos com contratos ativos e podem ter regras de pagamento mais flexíveis.
     * 
     * @return true se o cliente é um CLIENT (Role.CLIENT), false caso contrário (CUSTOMER)
     */
    public boolean isFromTrustedClient() {
        if (client == null) {
            return false;
        }
        return client.getRole() == User.Role.CLIENT;
    }
}
