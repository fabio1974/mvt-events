package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perfil especializado para usuários com role COURIER (motoboy).
 * Relacionamento 1:1 com User.
 * ADMs são gerenciados via CourierADMLink (N:M).
 */
@Entity
@Table(name = "courier_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CourierProfile extends BaseEntity {

    // ============================================================================
    // RELATIONSHIP WITH USER
    // ============================================================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Visible(table = true, form = false, filter = true)
    private User user;

    // ============================================================================
    // VEHICLE INFORMATION
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    @Visible(table = true, form = true, filter = true)
    private VehicleType vehicleType;

    @Pattern(regexp = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", message = "Placa inválida (formato: ABC1D23)")
    @Size(max = 10, message = "Placa deve ter no máximo 10 caracteres")
    @Column(name = "vehicle_plate", length = 10)
    @Visible(table = true, form = true, filter = false)
    private String vehiclePlate;

    @Size(max = 50, message = "Modelo deve ter no máximo 50 caracteres")
    @Column(name = "vehicle_model", length = 50)
    @Visible(table = false, form = true, filter = false)
    private String vehicleModel;

    @Size(max = 30, message = "Cor deve ter no máximo 30 caracteres")
    @Column(name = "vehicle_color", length = 30)
    @Visible(table = false, form = true, filter = false)
    private String vehicleColor;

    // ============================================================================
    // PERFORMANCE METRICS
    // ============================================================================

    @DecimalMin(value = "0.0", message = "Avaliação mínima é 0.0")
    @DecimalMax(value = "5.0", message = "Avaliação máxima é 5.0")
    @Column(precision = 3, scale = 2)
    @Visible(table = true, form = false, filter = true)
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(value = 0, message = "Total de entregas não pode ser negativo")
    @Column(name = "total_deliveries")
    @Visible(table = true, form = false, filter = false)
    private Integer totalDeliveries = 0;

    @Min(value = 0, message = "Entregas completadas não pode ser negativo")
    @Column(name = "completed_deliveries")
    @Visible(table = false, form = false, filter = false)
    private Integer completedDeliveries = 0;

    @Min(value = 0, message = "Entregas canceladas não pode ser negativo")
    @Column(name = "cancelled_deliveries")
    @Visible(table = false, form = false, filter = false)
    private Integer cancelledDeliveries = 0;

    // ============================================================================
    // STATUS
    // ============================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private CourierStatus status = CourierStatus.OFFLINE;

    @Column(name = "last_location_update")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime lastLocationUpdate;

    @Column(name = "current_latitude")
    @Visible(table = false, form = false, filter = false)
    private Double currentLatitude;

    @Column(name = "current_longitude")
    @Visible(table = false, form = false, filter = false)
    private Double currentLongitude;

    // ============================================================================
    // RELATIONSHIPS (N:M WITH ADM VIA COURIER_ADM_LINKS)
    // ============================================================================

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "courier_id", referencedColumnName = "user_id")
    // TODO: CourierADMLink removido - agora Courier se relaciona com Organization
    // via EmploymentContract
    // @Visible(table = false, form = false, filter = false)
    // private Set<CourierADMLink> admLinks = new HashSet<>();

    // ============================================================================
    // COMPUTED FIELDS (Helper methods for computed values)
    // ============================================================================

    /**
     * Calcula a taxa de sucesso do courier (% de entregas completadas)
     */
    public BigDecimal getSuccessRate() {
        if (totalDeliveries == null || totalDeliveries == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedDeliveries)
                .divide(BigDecimal.valueOf(totalDeliveries), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // TODO: Método removido - CourierADMLink não existe mais
    // /**
    // * Retorna o nome do ADM principal
    // */
    // public String getPrimaryADMName() {
    // User primaryADM = getPrimaryADM();
    // return primaryADM != null ? primaryADM.getName() : "Sem ADM";
    // }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Retorna o ADM principal (is_primary = true e is_active = true)
     */
    // TODO: Métodos removidos - CourierADMLink não existe mais
    // public User getPrimaryADM() {
    // return admLinks.stream()
    // .filter(link -> Boolean.TRUE.equals(link.getIsPrimary()) &&
    // Boolean.TRUE.equals(link.getIsActive()))
    // .map(link -> link.getAdm())
    // .findFirst()
    // .orElse(null);
    // }

    // /**
    // * Retorna todos os ADMs ativos
    // */
    // public Set<User> getAllActiveADMs() {
    // return admLinks.stream()
    // .filter(link -> Boolean.TRUE.equals(link.getIsActive()))
    // .map(link -> link.getAdm())
    // .collect(Collectors.toSet());
    // }

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum VehicleType {
        MOTORCYCLE, // Moto
        BICYCLE, // Bicicleta
        CAR, // Carro
        SCOOTER, // Patinete/Scooter elétrico
        ON_FOOT // A pé
    }

    public enum CourierStatus {
        AVAILABLE, // Disponível para entregas
        ON_DELIVERY, // Em entrega
        OFFLINE, // Offline/Indisponível
        SUSPENDED // Suspenso
    }
}
