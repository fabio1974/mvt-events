package com.mvt.mvt_events.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * DTO para criar/atualizar endereço
 */
public record AddressRequest(
    
    @NotBlank(message = "Rua é obrigatória")
    String street,
    
    @NotBlank(message = "Número é obrigatório")
    String number,
    
    String complement, // Opcional
    
    @NotBlank(message = "Bairro é obrigatório")
    String neighborhood,
    
    String referencePoint, // Opcional
    
    Double latitude, // Opcional
    
    Double longitude, // Opcional
    
    @NotNull(message = "Cidade é obrigatória")
    UUID cityId // FK para City
) {
}
