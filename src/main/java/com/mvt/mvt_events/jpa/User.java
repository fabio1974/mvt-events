package com.mvt.mvt_events.jpa;

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
import java.util.List;
import java.util.UUID;

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
    @Visible(readonly = true)
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
    @Column(name = "emergency_contact")
    private String emergencyContact;

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

    @Visible(table = false, form = false, filter = false)
    @Column
    private Double latitude;

    @Visible(table = false, form = false, filter = false)
    @Column
    private Double longitude;

    // Organization relationship for ORGANIZER role
    @Visible(table = false, form = false, filter = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private boolean enabled = true;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum Role {
        USER, // Mantido para compatibilidade
        ORGANIZER, // Dono da Organization (tenant)
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
}