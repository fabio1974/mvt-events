package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import com.mvt.mvt_events.metadata.Computed;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User courier;

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

    @DecimalMin(value = "0.0", message = "Distância não pode ser negativa")
    @Column(name = "distance_km", precision = 6, scale = 2)
    @Visible(table = true, form = false, filter = false)
    private BigDecimal distanceKm;

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
    @Visible(table = true, form = true, filter = false)
    private String recipientName;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "recipient_phone", length = 20)
    @Visible(table = false, form = true, filter = false)
    private String recipientPhone;

    // ============================================================================
    // PRICING
    // ============================================================================

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal totalAmount;

    // ============================================================================
    // STATUS & TIMESTAMPS
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "scheduled_pickup_at")
    @Visible(table = true, form = true, filter = true)
    private LocalDateTime scheduledPickupAt;

    @Column(name = "accepted_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime pickedUpAt;

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
    // OPTIONAL: MUNICIPAL PARTNERSHIP
    // ============================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = false, form = true, filter = true)
    private MunicipalPartnership partnership;

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private Payment payment;

    @OneToOne(mappedBy = "delivery", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private Evaluation evaluation;

    // ============================================================================
    // COMPUTED FIELDS
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
        PICKED_UP, // Item coletado
        IN_TRANSIT, // Em trânsito
        COMPLETED, // Entregue com sucesso
        CANCELLED // Cancelada
    }
}
