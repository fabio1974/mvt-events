package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;
import com.mvt.mvt_events.validation.ValidBankCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa dados bancários de um usuário (courier ou organizer).
 * Usada para transferências automáticas via Pagar.me.
 * 
 * Relacionamentos:
 * - 1:1 com User (um usuário tem uma conta bancária)
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BankAccount extends BaseEntity {

    // ============================================================================
    // RELATIONSHIPS
    // ============================================================================

    @NotNull(message = "Usuário é obrigatório")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    @Visible(table = true, form = true, filter = true)
    private User user;

    // ============================================================================
    // BANK DATA
    // ============================================================================

    @DisplayLabel
    @NotBlank(message = "Código do banco é obrigatório")
    @ValidBankCode(message = "Código do banco inválido ou não cadastrado")
    @Column(name = "bank_code", nullable = false, length = 3)
    @Visible(table = true, form = true, filter = true)
    private String bankCode; // Código do banco (ex: 260 = Nubank)

    @NotBlank(message = "Nome do banco é obrigatório")
    @Size(max = 100, message = "Nome do banco não pode exceder 100 caracteres")
    @Column(name = "bank_name", nullable = false, length = 100)
    @Visible(table = true, form = true, filter = false)
    private String bankName; // Nome do banco (ex: Nubank)

    @NotBlank(message = "Agência é obrigatória")
    @Size(min = 3, max = 10, message = "Agência deve ter entre 3 e 10 caracteres")
    @Pattern(regexp = "\\d+", message = "Agência deve conter apenas números (sem dígito verificador)")
    @Column(name = "agency", nullable = false, length = 10)
    @Visible(table = true, form = true, filter = false)
    private String agency; // Agência sem dígito verificador

    @Column(name = "agency_digit", length = 2)
    @Visible(table = true, form = true, filter = false)
    private String agencyDigit; // Dígito verificador da agência (opcional)

    @NotBlank(message = "Conta é obrigatória")
    @Size(min = 1, max = 20, message = "Conta deve ter entre 1 e 20 caracteres")
    @Pattern(regexp = "\\d+", message = "Número da conta deve conter apenas números")
    @Column(name = "account_number", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = false)
    private String accountNumber; // Número da conta (apenas dígitos)

    @Column(name = "account_digit", length = 2)
    @Visible(table = true, form = true, filter = false)
    private String accountDigit; // Dígito verificador da conta (extraído de accountNumber)

    @NotNull(message = "Tipo de conta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    @Visible(table = true, form = true, filter = true)
    private AccountType accountType = AccountType.CHECKING; // checking (corrente) ou savings (poupança)

    // ============================================================================
    // PAGAR.ME KYC FIELDS
    // ============================================================================

    /**
     * Nome da mãe (obrigatório para Pagar.me KYC)
     */
    @Column(name = "mother_name", length = 200)
    @Visible(table = false, form = false, filter = false)
    private String motherName;

    /**
     * Renda mensal estimada (obrigatório para Pagar.me KYC)
     */
    @Column(name = "monthly_income", length = 20)
    @Visible(table = false, form = false, filter = false)
    private String monthlyIncome;

    /**
     * Ocupação profissional (obrigatório para Pagar.me KYC)
     */
    @Column(name = "professional_occupation", length = 100)
    @Visible(table = false, form = false, filter = false)
    private String professionalOccupation;

    // NOTE: Os seguintes campos KYC são obtidos da entidade User:
    // - accountHolderName → user.getName()
    // - accountHolderDocument → user.getDocumentNumber()
    // - email → user.getUsername()
    // - birthdate → user.getDateOfBirth() (formatado DD/MM/YYYY)
    // - phoneDdd → user.getPhoneDdd()
    // - phoneNumber → user.getPhoneNumber()

    // ============================================================================
    // STATUS
    // ============================================================================

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Visible(table = true, form = true, filter = true)
    private BankAccountStatus status = BankAccountStatus.PENDING_VALIDATION;

    @Column(name = "validated_at")
    @Visible(table = true, form = false, filter = false)
    private LocalDateTime validatedAt;

    // ============================================================================
    // METADATA?
    // ============================================================================

    @Column(name = "notes", columnDefinition = "TEXT")
    @Visible(table = false, form = false, filter = false)
    private String notes; // Notas sobre a conta (ex: motivo de bloqueio)

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum AccountType {
        CHECKING("checking", "Conta Corrente"),
        SAVINGS("savings", "Conta Poupança");

        private final String pagarmeValue;
        private final String displayName;

        AccountType(String pagarmeValue, String displayName) {
            this.pagarmeValue = pagarmeValue;
            this.displayName = displayName;
        }

        public String getPagarmeValue() {
            return pagarmeValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static AccountType fromPagarmeValue(String value) {
            for (AccountType tipo : values()) {
                if (tipo.pagarmeValue.equals(value)) {
                    return tipo;
                }
            }
            throw new IllegalArgumentException("Tipo de conta inválido: " + value);
        }
    }

    public enum BankAccountStatus {
        PENDING_VALIDATION("Pendente de Validação"),
        ACTIVE("Ativa"),
        BLOCKED("Bloqueada"),
        CANCELLED("Cancelada");

        private final String displayName;

        BankAccountStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    public boolean isActive() {
        return status == BankAccountStatus.ACTIVE;
    }

    public boolean isPendingValidation() {
        return status == BankAccountStatus.PENDING_VALIDATION;
    }

    public boolean isBlocked() {
        return status == BankAccountStatus.BLOCKED;
    }

    public void markAsActive() {
        this.status = BankAccountStatus.ACTIVE;
        this.validatedAt = LocalDateTime.now();
    }

    public void markAsBlocked(String reason) {
        this.status = BankAccountStatus.BLOCKED;
        this.notes = reason;
    }

    public void markAsCancelled(String reason) {
        this.status = BankAccountStatus.CANCELLED;
        this.notes = reason;
    }

    /**
     * Retorna dados mascarados para exibição segura
     */
    public String getAccountNumberMasked() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "***";
        }
        return "***" + accountNumber.substring(accountNumber.length() - 4);
    }

    public String getAgencyMasked() {
        if (agency == null || agency.length() < 2) {
            return "***";
        }
        return "***" + agency.substring(agency.length() - 2);
    }
}
