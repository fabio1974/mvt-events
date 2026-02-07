package com.mvt.mvt_events.jpa;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Preferências de pagamento do cliente.
 * Armazena o tipo de pagamento preferido (PIX ou CREDIT_CARD) e o cartão padrão.
 */
@Entity
@Table(name = "customer_payment_preferences")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class CustomerPaymentPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário (cliente) dono desta preferência.
     * Relacionamento 1:1 - cada cliente tem apenas uma preferência.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Tipo de pagamento preferido.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_payment_type", nullable = false, length = 20)
    @Builder.Default
    private PreferredPaymentType preferredPaymentType = PreferredPaymentType.PIX;

    /**
     * Cartão padrão (usado quando preferredPaymentType = CREDIT_CARD).
     * Pode ser null se o cliente prefere PIX.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_card_id")
    private CustomerCard defaultCard;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum para tipos de pagamento preferidos.
     */
    public enum PreferredPaymentType {
        PIX,
        CREDIT_CARD
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Verifica se o cliente prefere PIX.
     */
    public boolean prefersPix() {
        return preferredPaymentType == PreferredPaymentType.PIX;
    }

    /**
     * Verifica se o cliente prefere cartão de crédito.
     */
    public boolean prefersCreditCard() {
        return preferredPaymentType == PreferredPaymentType.CREDIT_CARD;
    }

    /**
     * Verifica se a preferência está válida.
     * Se preferir cartão, deve ter um cartão padrão definido.
     */
    public boolean isValid() {
        if (prefersCreditCard()) {
            return defaultCard != null && defaultCard.getIsActive();
        }
        return true; // PIX não precisa de cartão
    }
}
