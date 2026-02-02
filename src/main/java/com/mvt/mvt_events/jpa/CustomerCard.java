package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um cartão de crédito tokenizado do cliente.
 * 
 * SEGURANÇA (PCI Compliance):
 * - NUNCA armazena número completo do cartão
 * - NUNCA armazena CVV
 * - Armazena apenas token do Pagar.me + últimos 4 dígitos + bandeira
 * 
 * Relacionamentos:
 * - N:1 com User (customer que possui o cartão)
 * 
 * Funcionalidades:
 * - Múltiplos cartões por cliente
 * - Um cartão marcado como "padrão" (default)
 * - Soft delete (não remove fisicamente por auditoria)
 */
@Entity
@Table(name = "customer_cards", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "pagarme_card_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerCard extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Cliente é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User customer;

    // ============================================================================
    // PAGAR.ME TOKEN (PCI Compliance)
    // ============================================================================

    /**
     * ID do cartão tokenizado no Pagar.me (card_xxxxx).
     * Este é o único identificador que usamos para processar pagamentos.
     * NUNCA armazenamos o número real do cartão.
     */
    @NotNull(message = "Token do Pagar.me é obrigatório")
    @Column(name = "pagarme_card_id", nullable = false, length = 100, unique = true)
    @DisplayLabel
    @Visible(table = false, form = false, filter = true)
    private String pagarmeCardId;

    // ============================================================================
    // INFORMAÇÕES PARA EXIBIÇÃO (não sensíveis)
    // ============================================================================

    /**
     * Últimos 4 dígitos do cartão (para exibição ao usuário).
     * Exemplo: "4242"
     */
    @NotNull(message = "Últimos 4 dígitos são obrigatórios")
    @Column(name = "last_four_digits", nullable = false, length = 4)
    @Visible(table = true, form = false, filter = false)
    private String lastFourDigits;

    /**
     * Bandeira do cartão (Visa, Mastercard, Elo, etc).
     */
    @NotNull(message = "Bandeira é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "brand", nullable = false, length = 20)
    @Visible(table = true, form = false, filter = true)
    private CardBrand brand;

    /**
     * Nome do titular conforme impresso no cartão.
     * Exemplo: "JOAO DA SILVA"
     */
    @Column(name = "holder_name", length = 100)
    @Visible(table = true, form = false, filter = false)
    private String holderName;

    /**
     * Mês de expiração (1-12).
     */
    @Column(name = "exp_month")
    @Visible(table = false, form = false, filter = false)
    private Integer expMonth;

    /**
     * Ano de expiração (formato: 2026).
     */
    @Column(name = "exp_year")
    @Visible(table = false, form = false, filter = false)
    private Integer expYear;

    // ============================================================================
    // CONTROLE E STATUS
    // ============================================================================

    /**
     * Se este é o cartão padrão do cliente (usado por default nos pagamentos).
     * Apenas um cartão pode ser padrão por cliente.
     */
    @Column(name = "is_default", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isDefault = false;

    /**
     * Se o cartão está ativo (pode ser usado em pagamentos).
     * Cartões podem ser desativados pelo cliente ou por falhas de verificação.
     */
    @Column(name = "is_active", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private Boolean isActive = true;

    /**
     * Se o cartão foi verificado pelo Pagar.me (verificação de titular, etc).
     */
    @Column(name = "is_verified", nullable = false)
    @Visible(table = false, form = false, filter = true)
    private Boolean isVerified = false;

    /**
     * Data em que o cartão foi verificado.
     */
    @Column(name = "verified_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime verifiedAt;

    /**
     * Último uso do cartão (para ordenar por recência).
     */
    @Column(name = "last_used_at")
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime lastUsedAt;

    /**
     * Soft delete: marca cartão como deletado (não remove fisicamente).
     * Mantemos por auditoria e histórico de transações.
     */
    @Column(name = "deleted_at")
    @JsonIgnore
    @Visible(table = false, form = false, filter = false)
    private LocalDateTime deletedAt;

    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================

    /**
     * Verifica se o cartão está expirado.
     */
    public boolean isExpired() {
        if (expMonth == null || expYear == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        return expYear < currentYear || (expYear == currentYear && expMonth < currentMonth);
    }

    /**
     * Máscara do cartão para exibição segura.
     * Exemplo: "Visa **** 4242"
     */
    public String getMaskedNumber() {
        return brand.getDisplayName() + " **** " + lastFourDigits;
    }

    /**
     * Validade no formato MM/YY para exibição.
     * Exemplo: "12/26"
     */
    public String getExpirationDisplay() {
        if (expMonth == null || expYear == null) {
            return "N/A";
        }
        return String.format("%02d/%02d", expMonth, expYear % 100);
    }

    /**
     * Soft delete: marca como deletado sem remover do banco.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
        this.isDefault = false;
    }

    /**
     * Restaura cartão deletado.
     */
    public void restore() {
        this.deletedAt = null;
        this.isActive = true;
    }

    /**
     * Verifica se o cartão foi deletado.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum CardBrand {
        VISA("Visa"),
        MASTERCARD("Mastercard"),
        AMEX("American Express"),
        ELO("Elo"),
        HIPERCARD("Hipercard"),
        DINERS("Diners Club"),
        DISCOVER("Discover"),
        JCB("JCB"),
        OTHER("Outro");

        private final String displayName;

        CardBrand(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
