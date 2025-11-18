package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Perfil especializado para usuários com role ADM (gerente local).
 * Relacionamento 1:1 com User.
 * ADM é o TENANT do sistema - todas as queries devem filtrar por ADM via
 * Specifications.
 */
@Entity
@Table(name = "adm_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ADMProfile extends BaseEntity {

    // ============================================================================
    // RELATIONSHIP WITH USER
    // ============================================================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Visible(table = true, form = false, filter = true)
    private User user;

    // ============================================================================
    // MANAGEMENT AREA (TENANT SCOPE)
    // ============================================================================

    @NotBlank(message = "Região é obrigatória")
    @Size(max = 100, message = "Região deve ter no máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    @Visible(table = true, form = true, filter = true)
    private String region;

    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres")
    @Column(name = "region_code", length = 20)
    @Visible(table = true, form = true, filter = true)
    private String regionCode;

    // ============================================================================
    // COMMISSION & FINANCIAL
    // ============================================================================

    @NotNull(message = "Comissão é obrigatória")
    @DecimalMin(value = "0.0", message = "Comissão mínima é 0%")
    @DecimalMax(value = "100.0", message = "Comissão máxima é 100%")
    @Column(name = "commission_percentage", precision = 5, scale = 2, nullable = false)
    @Visible(table = true, form = true, filter = false)
    private BigDecimal commissionPercentage = BigDecimal.valueOf(10.0);

    @Column(name = "total_commission", precision = 12, scale = 2)
    @Visible(table = true, form = false, filter = false)
    private BigDecimal totalCommission = BigDecimal.ZERO;

    // ============================================================================
    // PERFORMANCE METRICS
    // ============================================================================

    @Min(value = 0, message = "Total de clientes não pode ser negativo")
    @Column(name = "total_clients_managed")
    @Visible(table = true, form = false, filter = false)
    private Integer totalClientsManaged = 0;

    @Min(value = 0, message = "Total de motoboys não pode ser negativo")
    @Column(name = "total_couriers_managed")
    @Visible(table = true, form = false, filter = false)
    private Integer totalCouriersManaged = 0;

    @Min(value = 0, message = "Total de entregas não pode ser negativo")
    @Column(name = "total_deliveries_managed")
    @Visible(table = true, form = false, filter = false)
    private Integer totalDeliveriesManaged = 0;

    // ============================================================================
    // ============================================================================
    // STATUS
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private ADMStatus status = ADMStatus.ACTIVE;

    // ============================================================================
    // COMPUTED FIELDS (Helper methods for computed values)
    // ============================================================================

    /**
     * Calcula a comissão média por entrega gerenciada
     */
    public BigDecimal getAverageCommissionPerDelivery() {
        if (totalDeliveriesManaged == null || totalDeliveriesManaged == 0) {
            return BigDecimal.ZERO;
        }
        return totalCommission.divide(
                BigDecimal.valueOf(totalDeliveriesManaged),
                2,
                RoundingMode.HALF_UP);
    }

    // REMOVIDO: getPartnershipName() - Municipal Partnerships foi removido do sistema

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum ADMStatus {
        ACTIVE, // Ativo
        INACTIVE, // Inativo
        SUSPENDED // Suspenso
    }
}
