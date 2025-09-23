package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
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

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // ============================================================================
    // ATHLETE FIELDS (from migration V4)
    // ============================================================================

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private String name;

    private String phone;

    @Column(length = 100)
    private String state;

    // Organization relationship for ORGANIZER role
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum Role {
        USER, ORGANIZER, ADMIN
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

    // ============================================================================
    // CONSTRUCTORS
    // ============================================================================

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.enabled = true;
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