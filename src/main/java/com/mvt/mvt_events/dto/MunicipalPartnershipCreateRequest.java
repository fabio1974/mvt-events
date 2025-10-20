package com.mvt.mvt_events.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para criação de MunicipalPartnership
 */
@Data
public class MunicipalPartnershipCreateRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Cidade é obrigatória")
    private String city;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    private String state;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    private String cnpj;

    @NotBlank(message = "Número do convênio é obrigatório")
    private String agreementNumber;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate startDate;

    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "Percentual de desconto deve ser maior ou igual a 0")
    @DecimalMax(value = "100.0", message = "Percentual de desconto deve ser menor ou igual a 100")
    private BigDecimal discountPercentage;

    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String notes;
}
