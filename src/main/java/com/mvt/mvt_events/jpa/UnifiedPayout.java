package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.metadata.Visible;
import com.mvt.mvt_events.metadata.Computed;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repasses periódicos unificados para Couriers e ADMs.
 * Agrupa múltiplos pagamentos (PayoutItems) em um único repasse.
 * Simplifica a gestão financeira e contabilidade.
 */
@Entity
@Table(name = "unified_payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UnifiedPayout extends BaseEntity {

    // ============================================================================
    // BENEFICIARY
    // ============================================================================

    
    @NotNull(message = "Beneficiário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    @Visible(table = true, form = true, filter = true)
    private User beneficiary; // COURIER ou ADM

    
    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_type", nullable = false, length = 20)
    @Visible(table = true, form = false, filter = true)
    private BeneficiaryType beneficiaryType;

    // ============================================================================
    // PERIOD
    // ============================================================================

    ")
    @NotBlank(message = "Período é obrigatório")
    @Size(max = 7, message = "Período deve ter formato YYYY-MM")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Período deve ter formato YYYY-MM")
    @Column(nullable = false, length = 7)
    @Visible(table = true, form = true, filter = true)
    private String period; // Formato: YYYY-MM (ex: 2025-10)

    
    @Column(name = "start_date")
    @Visible(table = false, form = false, filter = false)
    private LocalDate startDate;

    
    @Column(name = "end_date")
    @Visible(table = false, form = false, filter = false)
    private LocalDate endDate;

    // ============================================================================
    // FINANCIAL
    // ============================================================================

    
    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor não pode ser negativo")
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    @Visible(table = true, form = false, filter = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    
    @Min(value = 0, message = "Total de itens não pode ser negativo")
    @Column(name = "item_count")
    @Visible(table = true, form = false, filter = false)
    private Integer itemCount = 0;

    // ============================================================================
    // STATUS & PAYMENT
    // ============================================================================

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private PayoutStatus status = PayoutStatus.PENDING;

    
    @Column(name = "paid_at")
    @Visible(table = true, form = false, filter = true)
    private LocalDateTime paidAt;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Visible(table = false, form = true, filter = true)
    private PayoutMethod paymentMethod;

    
    @Size(max = 100, message = "Referência deve ter no máximo 100 caracteres")
    @Column(name = "payment_reference", length = 100)
    @Visible(table = false, form = true, filter = false)
    private String paymentReference;

    
    @Column(columnDefinition = "TEXT")
    @Visible(table = false, form = true, filter = false)
    private String notes;

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @OneToMany(mappedBy = "payout", cascade = CascadeType.ALL, orphanRemoval = true)
    @Visible(table = false, form = false, filter = false)
    private List<PayoutItem> items = new ArrayList<>();

    // ============================================================================
    // COMPUTED FIELDS
    // ============================================================================

    
    @Visible(table = true, form = false, filter = false)
    public String getBeneficiaryName() {
        return beneficiary != null ? beneficiary.getName() : "N/A";
    }

    
    @Visible(table = true, form = false, filter = false)
    public BigDecimal getAverageItemValue() {
        if (itemCount == null || itemCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(itemCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum BeneficiaryType {
        COURIER,  // Repasse para motoboy
        ADM       // Repasse para gerente (comissão)
    }

    public enum PayoutStatus {
        PENDING,    // Aguardando processamento
        PROCESSING, // Em processamento
        COMPLETED,  // Pago
        FAILED,     // Falha no pagamento
        CANCELLED   // Cancelado
    }

    public enum PayoutMethod {
        PIX,          // PIX
        BANK_TRANSFER, // Transferência bancária (TED/DOC)
        CASH,         // Dinheiro
        CHECK         // Cheque
    }
}
