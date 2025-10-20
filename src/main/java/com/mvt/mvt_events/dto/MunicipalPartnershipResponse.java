package com.mvt.mvt_events.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta para MunicipalPartnership
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MunicipalPartnershipResponse {

    private Long id;
    private LocalDateTime createdAt;

    private String name;
    private String city;
    private String state;
    private String cnpj;
    private String agreementNumber;

    private LocalDate startDate;
    private LocalDate endDate;

    private String status;
    private Boolean isValid; // válida hoje

    private BigDecimal discountPercentage;

    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    private String notes;
}
