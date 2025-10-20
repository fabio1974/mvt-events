package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para criação de UnifiedPayout
 */
@Data
public class UnifiedPayoutCreateRequest {

    @NotNull(message = "Beneficiário é obrigatório")
    private String beneficiaryId; // UUID como String

    @NotNull(message = "Tipo de beneficiário é obrigatório")
    private String beneficiaryType; // COURIER, ADM

    @NotBlank(message = "Período é obrigatório")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Período deve estar no formato YYYY-MM")
    private String period;

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor deve ser maior ou igual a 0")
    private BigDecimal totalAmount;

    private String paymentMethod; // PIX, BANK_TRANSFER, CASH, CHECK
    private String notes;
}
