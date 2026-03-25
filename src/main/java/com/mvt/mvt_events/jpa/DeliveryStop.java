package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Representa uma parada (destino) de uma entrega.
 * Relação: Delivery (1) → DeliveryStop (N).
 * 
 * Para entregas single-stop (CUSTOMER), haverá exatamente 1 stop.
 * Para entregas multi-stop (CLIENT), haverá N stops ordenados por stopOrder.
 */
@Entity
@Table(name = "delivery_stops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class DeliveryStop extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Delivery delivery;

    @NotNull
    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @NotBlank
    @Column(name = "address", columnDefinition = "TEXT", nullable = false)
    @Size(max = 500)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Size(max = 150)
    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Size(max = 20)
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Size(max = 500)
    @Column(name = "item_description", length = 500)
    private String itemDescription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StopStatus status = StopStatus.PENDING;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /**
     * Ordem de visita desta parada.
     * COMPLETED: valor incremental (1, 2, 3…) atribuído na conclusão. Imutável após set.
     * SKIPPED: 0. Imutável após set.
     * PENDING: sequência planejada pelo nearest-neighbor; atualizada a cada recálculo de rota.
     */
    @Column(name = "completion_order")
    private Integer completionOrder;

    public enum StopStatus {
        PENDING,
        COMPLETED,
        SKIPPED
    }
}
