package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import com.mvt.mvt_events.validation.CPF;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @DisplayLabel
    @Column(nullable = false, length = 150)
    private String name;

    @Email(message = "Username must be a valid email")
    @Column(unique = true, nullable = false)
    private String username;

    @Visible(table = false, form = false, filter = false)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Visible(readonly = false, table = false, form = true, filter = true)
    private Role role = Role.ORGANIZER;

    @Visible(table = false, form = true, filter = false)
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    @Visible(table = true, form = true, filter = true)
    private City city;

    @Visible(table = false, form = false, filter = false)
    @Column(length = 100)
    private String country;

    @Column(name = "date_of_birth")
    @Visible(table = false, form = true, filter = false)
    private LocalDate dateOfBirth;

    @CPF(message = "CPF inválido")
    @Column(name = "document_number", unique = true, nullable = false, length = 14)
    private String cpf; // CPF do usuário (obrigatório e único)

    @Visible(table = false, form = true, filter = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Visible(table = true, form = true, filter = false)
    @Column(length = 20)
    private String phone;

    @Visible(table = false, form = false, filter = false)
    @Column(length = 100)
    private String state;

    // ============================================================================
    // ZAPI10 GEOLOCATION FIELDS
    // ============================================================================

    @Visible(table = false, form = true, filter = false, readonly = true)
    @Column(name = "gps_latitude")
    private Double gpsLatitude;

    @Visible(table = false, form = true, filter = false, readonly = true)
    @Column(name = "gps_longitude")
    private Double gpsLongitude;

    @Visible(table = false, form = true, filter = false, readonly = true)
    @Column(name = "latitude")
    private Double latitude;

    @Visible(table = false, form = true, filter = false, readonly = true)
    @Column(name = "longitude")
    private Double longitude;

    @Column(nullable = false)
    private boolean enabled = true;

    // ============================================================================
    // IUGU INTEGRATION FIELDS
    // ============================================================================

    /**
     * ID da subconta Iugu criada para este usuário (courier ou organizer).
     * Usado para transferências automáticas via split de pagamento.
     */
    @Column(name = "iugu_account_id", length = 100)
    @Visible(table = false, form = false, filter = false)
    private String iuguAccountId;

    /**
     * Indica se o usuário completou o cadastro de dados bancários.
     * true = dados bancários salvos e validados pelo Iugu
     * false = ainda não cadastrou ou dados pendentes de validação
     */
    @Column(name = "bank_data_complete", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private Boolean bankDataComplete = false;

    /**
     * Indica se o auto_withdraw está ativo no Iugu.
     * true = transferências automáticas (D+1) estão ativas
     * false = precisa ativar ou dados bancários inválidos
     */
    @Column(name = "auto_withdraw_enabled", nullable = false)
    @Visible(table = true, form = false, filter = true)
    private Boolean autoWithdrawEnabled = false;

    /**
     * Relacionamento 1:1 com BankAccount (OPCIONAL).
     * 
     * DESIGN ATUAL (v1.0):
     * - OBRIGATÓRIO para COURIER/ORGANIZER (recebem pagamentos via Iugu)
     * - OPCIONAL para CLIENT (pagam com cartão, não recebem)
     * - OPCIONAL para USER/ADMIN
     * 
     * FUTURO (v2.0):
     * - Criar entidade PaymentMethod (abstract/interface)
     * - BankAccount extends PaymentMethod (receber via Iugu PIX)
     * - CreditCard extends PaymentMethod (pagar via Stripe/Cielo)
     * - User terá List<PaymentMethod> (múltiplas formas de pagamento)
     * 
     * REGRAS:
     * - Se bankAccount == null → usuário não pode receber pagamentos
     * - Se bankAccount != null && status == ACTIVE → pode receber via Iugu
     * - Validar com: user.canReceivePayments()
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Visible(table = false, form = true, filter = false)
    private BankAccount bankAccount;

    // ============================================================================
    // N:M RELATIONSHIPS
    // ============================================================================

    // Para COURIER - contratos de trabalho (empregado-empresa)
    @OneToMany(mappedBy = "courier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Visible(table = false, form = true, filter = false)
    private Set<EmploymentContract> employmentContracts = new HashSet<>();

    // Para CLIENT - contratos de serviço (cliente-fornecedor)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Visible(table = false, form = true, filter = false)
    private Set<ClientContract> clientContracts = new HashSet<>();

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum Role {
        USER, // Mantido para compatibilidade
        ORGANIZER, // Gerente ADM - Dono da Organization (tenant)
        ADMIN, // Admin do sistema
        CLIENT, // Cliente que solicita entregas
        COURIER // Motoboy que realiza entregas
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    // ============================================================================
    // USERDETAILS IMPLEMENTATION
    // ============================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isOrganizer() {
        return role == Role.ORGANIZER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isUser() {
        return role == Role.USER;
    }

    public boolean canCreateEvents() {
        return role == Role.ORGANIZER || role == Role.ADMIN;
    }

    public boolean canRegisterForEvents() {
        return true; // All users can register for events
    }

    /**
     * Retorna o CPF sem formatação (apenas números)
     */
    public String getCpfClean() {
        return cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }

    /**
     * Retorna o CPF formatado (XXX.XXX.XXX-XX)
     */
    public String getCpfFormatted() {
        String clean = getCpfClean();
        if (clean == null || clean.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
                clean.substring(0, 3),
                clean.substring(3, 6),
                clean.substring(6, 9),
                clean.substring(9, 11));
    }

    /**
     * Método de compatibilidade - retorna CPF
     * 
     * @deprecated Use getCpf() ao invés
     */
    @Deprecated
    public String getDocumentNumber() {
        return cpf;
    }

    /**
     * Método de compatibilidade - define CPF
     * 
     * @deprecated Use setCpf() ao invés
     */
    @Deprecated
    public void setDocumentNumber(String documentNumber) {
        this.cpf = documentNumber;
    }

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = true;
    }

    // ============================================================================
    // USERDETAILS IMPLEMENTATION
    // ============================================================================

    @Override
    public String getUsername() {
        return username;
    }

    // ============================================================================
    // LIFECYCLE METHODS
    // ============================================================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================================================
    // RELATIONSHIP HELPER METHODS
    // ============================================================================

    /**
     * Retorna lista de organizações onde o usuário trabalha como COURIER
     */
    public Set<Organization> getEmployerOrganizations() {
        return employmentContracts.stream()
                .filter(EmploymentContract::isActive)
                .map(EmploymentContract::getOrganization)
                .collect(Collectors.toSet());
    }

    /**
     * Retorna lista de organizações onde o usuário é CLIENT
     */
    public Set<Organization> getClientOrganizationsList() {
        return clientContracts.stream()
                .filter(ClientContract::isActive)
                .map(ClientContract::getOrganization)
                .collect(Collectors.toSet());
    }

    /**
     * Retorna o contrato de serviço titular do cliente
     */
    public ClientContract getPrimaryContract() {
        return clientContracts.stream()
                .filter(ClientContract::isPrimary)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retorna a organização do contrato titular (se CLIENT)
     */
    public Organization getPrimaryOrganization() {
        ClientContract primary = getPrimaryContract();
        return primary != null ? primary.getOrganization() : null;
    }

    /**
     * Verifica se o usuário tem contratos de trabalho ativos como COURIER
     */
    public boolean hasActiveEmployment() {
        return !employmentContracts.isEmpty() &&
                employmentContracts.stream().anyMatch(EmploymentContract::isActive);
    }

    /**
     * Verifica se o usuário tem contratos de serviço ativos como CLIENT
     */
    public boolean hasActiveContracts() {
        return !clientContracts.isEmpty() &&
                clientContracts.stream().anyMatch(ClientContract::isActive);
    }

    // ============================================================================
    // IUGU/PAYMENT HELPER METHODS
    // ============================================================================

    /**
     * Verifica se o usuário pode receber pagamentos via Iugu.
     * Requer: dados bancários completos + auto_withdraw ativo + conta Iugu criada
     */
    public boolean canReceivePayments() {
        return iuguAccountId != null &&
                Boolean.TRUE.equals(bankDataComplete) &&
                Boolean.TRUE.equals(autoWithdrawEnabled);
    }

    /**
     * Verifica se o usuário tem dados bancários cadastrados
     */
    public boolean hasBankAccount() {
        return bankAccount != null &&
                Boolean.TRUE.equals(bankDataComplete);
    }

    /**
     * Marca que os dados bancários foram completados com sucesso
     */
    public void markBankAccountAsCompleted(String iuguAccountId) {
        this.iuguAccountId = iuguAccountId;
        this.bankDataComplete = true;
        this.autoWithdrawEnabled = true;
    }

    /**
     * Marca que os dados bancários precisam ser atualizados
     */
    public void markBankAccountAsPending() {
        this.bankDataComplete = false;
        this.autoWithdrawEnabled = false;
    }

    /**
     * Desativa o auto_withdraw (ex: dados bancários inválidos)
     */
    public void deactivateAutoWithdraw(String reason) {
        this.autoWithdrawEnabled = false;
        if (bankAccount != null) {
            bankAccount.markAsBlocked(reason);
        }
    }
}
