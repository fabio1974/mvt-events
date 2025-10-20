package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Parceria com prefeituras municipais para entregas institucionais.
 * Permite que ADMs sejam vinculados a convênios com órgãos públicos.
 */
@Entity
@Table(name = "municipal_partnerships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MunicipalPartnership extends BaseEntity {

    // ============================================================================
    // IDENTIFICATION
    // ============================================================================

    @DisplayLabel // Campo principal para exibição
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    @Visible(table = true, form = true, filter = true)
    private String name;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    @Visible(table = true, form = true, filter = true)
    private String city;

    @NotBlank(message = "Estado é obrigatório")
    @Size(max = 2, message = "Estado deve ter 2 caracteres (UF)")
    @Column(nullable = false, length = 2)
    @Visible(table = true, form = true, filter = true)
    private String state;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}", message = "CNPJ inválido")
    @Column(unique = true, nullable = false, length = 18)
    @Visible(table = true, form = true, filter = false)
    private String cnpj;

    // ============================================================================
    // CONTACT INFORMATION
    // ============================================================================

    @Size(max = 150, message = "Nome do contato deve ter no máximo 150 caracteres")
    @Column(name = "contact_name", length = 150)
    @Visible(table = false, form = true, filter = false)
    private String contactName;

    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    @Column(name = "contact_email", length = 150)
    @Visible(table = false, form = true, filter = false)
    private String contactEmail;

    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "contact_phone", length = 20)
    @Visible(table = false, form = true, filter = false)
    private String contactPhone;

    // ============================================================================
    // AGREEMENT DETAILS
    // ============================================================================

    @Size(max = 50, message = "Número do convênio deve ter no máximo 50 caracteres")
    @Column(name = "agreement_number", length = 50)
    @Visible(table = true, form = true, filter = false)
    private String agreementNumber;

    @Column(name = "start_date")
    @Visible(table = true, form = true, filter = true)
    private LocalDate startDate;

    @Column(name = "end_date")
    @Visible(table = true, form = true, filter = true)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private PartnershipStatus status = PartnershipStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum PartnershipStatus {
        PENDING, // Pendente de aprovação
        ACTIVE, // Parceria ativa
        SUSPENDED, // Temporariamente suspensa
        EXPIRED // Convênio expirado
    }
}
