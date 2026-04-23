package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Perfil da loja — módulo Zapi-Food.
 * Relação 1:1 com User (apenas CLIENTs com catálogo).
 */
@Entity
@Table(name = "store_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Builder.Default
    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private Map<String, Object> openingHours;

    @Column(name = "min_order", precision = 10, scale = 2)
    private BigDecimal minOrder;

    @Column(name = "avg_preparation_minutes")
    private Integer avgPreparationMinutes;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "table_orders_enabled", nullable = false)
    private Boolean tableOrdersEnabled = false;

    @Column(name = "table_orders_enabled_at")
    private OffsetDateTime tableOrdersEnabledAt;

    /**
     * Se false, o estabelecimento pula impressões automáticas (rodada de mesa, aceite de pedido food).
     * Impressões acionadas por botão explícito ("Imprimir", "Imprimir empacotados") ficam preservadas.
     */
    @Builder.Default
    @Column(name = "auto_print_enabled", nullable = false)
    private Boolean autoPrintEnabled = true;

    @Column(name = "total_tables")
    private Integer totalTables;

    @Column(name = "default_seats")
    private Integer defaultSeats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
