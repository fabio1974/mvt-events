package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "organizations")
@Data
@EqualsAndHashCode(callSuper = true)
public class Organization extends BaseEntity {

    @DisplayLabel
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    @Visible(filter = false, table = false, form = false)
    private String slug;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(length = 255)
    @Visible(filter = false, table = false, form = false)
    private String website;

    @Column(columnDefinition = "TEXT")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    @Visible(filter = false, table = false, form = true)
    private String description;

    @Column(name = "logo_url")
    @Visible(filter = false, table = false, form = false)
    private String logoUrl;

    // Owner (ORGANIZER da organização)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;

    // Commission
    @Column(name = "commission_percentage", precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 5.00")
    @Visible(filter = false, table = true, form = true, readonly = true)
    private BigDecimal commissionPercentage = BigDecimal.valueOf(5.00);

    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    // N:M Relationships
    // Contratos de trabalho (empregado-empresa) - Couriers que trabalham para esta
    // organização
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<EmploymentContract> employmentContracts;

    // Contratos de serviço (cliente-fornecedor) - Clientes que contratam serviços
    // desta organização
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ClientContract> clientContracts;

    // ============================================================================
    // RELATIONSHIP HELPER METHODS
    // ============================================================================

    /**
     * Retorna lista de couriers (funcionários) ativos desta organização
     */
    public List<User> getEmployees() {
        return employmentContracts.stream()
                .filter(EmploymentContract::isActive)
                .map(EmploymentContract::getCourier)
                .collect(Collectors.toList());
    }

    /**
     * Retorna lista de clientes com contratos de serviço ativos nesta organização
     */
    public List<User> getClients() {
        return clientContracts.stream()
                .filter(ClientContract::isActive)
                .map(ClientContract::getClient)
                .collect(Collectors.toList());
    }

    /**
     * Retorna contagem de contratos de serviço ativos
     */
    public long getActiveServiceContractsCount() {
        return clientContracts.stream()
                .filter(ClientContract::isActive)
                .count();
    }

    /**
     * Retorna contagem de funcionários (couriers) ativos
     */
    public long getActiveEmployeesCount() {
        return employmentContracts.stream()
                .filter(EmploymentContract::isActive)
                .count();
    }
}