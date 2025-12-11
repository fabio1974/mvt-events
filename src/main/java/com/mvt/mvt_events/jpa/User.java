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

    // ============================================================================
    // ADDRESS RELATIONSHIP (1:1)
    // ============================================================================

    /**
     * Relacionamento 1:1 com Address.
     * Cada usuário possui um endereço completo.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private Address address;

    // ============================================================================
    // GPS TRACKING FIELDS (Real-time location)
    // ============================================================================
    
    /**
     * Latitude GPS em tempo real (rastreamento de entregador/cliente).
     * Diferente de Address.latitude (endereço fixo).
     */
    @Column(name = "gps_latitude")
    @Visible(table = false, form = false, filter = false)
    private Double gpsLatitude;

    /**
     * Longitude GPS em tempo real (rastreamento de entregador/cliente).
     * Diferente de Address.longitude (endereço fixo).
     */
    @Column(name = "gps_longitude")
    @Visible(table = false, form = false, filter = false)
    private Double gpsLongitude;

    // ============================================================================
    // PERSONAL DATA
    // ============================================================================

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

    // ============================================================================
    // PHONE FIELDS (for Pagar.me KYC)
    // ============================================================================

    /**
     * DDD do telefone (2 dígitos)
     * Exemplo: "11" para São Paulo
     */
    @Column(name = "phone_ddd", length = 2)
    @Visible(table = false, form = true, filter = false)
    private String phoneDdd;

    /**
     * Número do telefone sem DDD (8 ou 9 dígitos)
     * Exemplo: "987654321"
     */
    @Column(name = "phone_number", length = 9)
    @Visible(table = true, form = true, filter = false)
    private String phoneNumber;

    /**
     * Telefone completo formatado (somente para compatibilidade)
     * Deprecated: Use phoneDdd + phoneNumber instead
     */
    @Deprecated
    @Visible(table = false, form = false, filter = false)
    @Column(length = 100)
    private String state;

    @Column(nullable = false)
    private boolean enabled = true;

    // ============================================================================
    // PAGAR.ME INTEGRATION FIELDS
    // ============================================================================

    /**
     * ID do recipient Pagar.me criado para este usuário (courier ou organizer).
     * Usado para transferências automáticas via split de pagamento.
     */
    @Column(name = "pagarme_recipient_id", length = 100)
    @Visible(table = false, form = false, filter = false)
    private String pagarmeRecipientId;

    /**
     * Status da conta no Pagar.me
     * Valores: "pending", "active", "inactive"
     */
    @Column(name = "pagarme_status", length = 20)
    @Visible(table = true, form = false, filter = true)
    private String pagarmeStatus;

    /**
     * Relacionamento 1:1 com BankAccount (OPCIONAL).
     * 
     * DESIGN ATUAL (v1.0):
     * - OBRIGATÓRIO para COURIER/ORGANIZER (recebem pagamentos via Pagar.me)
     * - OPCIONAL para CLIENT (pagam com cartão, não recebem)
     * - OPCIONAL para USER/ADMIN
     * 
     * FUTURO (v2.0):
     * - Criar entidade PaymentMethod (abstract/interface)
     * - BankAccount extends PaymentMethod (receber via Pagar.me PIX)
     * - CreditCard extends PaymentMethod (pagar via Stripe/Cielo)
     * - User terá List<PaymentMethod> (múltiplas formas de pagamento)
     * 
     * REGRAS:
     * - Se bankAccount == null → usuário não pode receber pagamentos
     * - Se bankAccount != null && pagarmeStatus == "active" → pode receber via Pagar.me
     * - Validar com: user.canReceivePayments()
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Visible(table = false, form = false, filter = false)
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

    /**
     * Retorna telefone completo formatado (DDD + número)
     * Formato: (11) 98765-4321
     */
    public String getPhone() {
        if (phoneDdd == null || phoneNumber == null) {
            return null;
        }
        
        // Formatar: (11) 98765-4321 ou (11) 8765-4321
        if (phoneNumber.length() == 9) {
            return String.format("(%s) %s-%s", 
                phoneDdd, 
                phoneNumber.substring(0, 5), 
                phoneNumber.substring(5));
        } else if (phoneNumber.length() == 8) {
            return String.format("(%s) %s-%s", 
                phoneDdd, 
                phoneNumber.substring(0, 4), 
                phoneNumber.substring(4));
        }
        
        return String.format("(%s) %s", phoneDdd, phoneNumber);
    }

    /**
     * Define telefone a partir de string formatada
     * Aceita formatos: (11) 98765-4321, 11987654321, etc.
     */
    public void setPhone(String phoneFormatted) {
        if (phoneFormatted == null || phoneFormatted.trim().isEmpty()) {
            this.phoneDdd = null;
            this.phoneNumber = null;
            return;
        }
        
        // Extrair apenas números
        String clean = phoneFormatted.replaceAll("[^0-9]", "");
        
        if (clean.length() >= 10) {
            this.phoneDdd = clean.substring(0, 2);
            this.phoneNumber = clean.substring(2);
        }
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
    // PAGAR.ME/PAYMENT HELPER METHODS
    // ============================================================================

    /**
     * Verifica se o usuário pode receber pagamentos via Pagar.me.
     * Requer: recipient criado + status active + dados bancários
     */
    public boolean canReceivePayments() {
        return pagarmeRecipientId != null &&
                "active".equals(pagarmeStatus) &&
                bankAccount != null;
    }

    /**
     * Verifica se o usuário tem dados bancários cadastrados
     */
    public boolean hasBankAccount() {
        return bankAccount != null;
    }

    /**
     * Marca que o recipient foi criado com sucesso no Pagar.me
     */
    public void markRecipientAsActive(String recipientId) {
        this.pagarmeRecipientId = recipientId;
        this.pagarmeStatus = "active";
    }

    /**
     * Marca que o recipient precisa ser recriado
     */
    public void markRecipientAsPending() {
        this.pagarmeStatus = "pending";
    }

    /**
     * Desativa o recipient (ex: dados bancários inválidos)
     */
    public void deactivateRecipient(String reason) {
        this.pagarmeStatus = "inactive";
        if (bankAccount != null) {
            bankAccount.markAsBlocked(reason);
        }
    }
}
