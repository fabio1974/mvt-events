package com.mvt.mvt_events.jpa;

import lombok.Getter;

/**
 * Enum para cores de veículos
 */
@Getter
public enum VehicleColor {
    BRANCO("Branco"),
    PRETO("Preto"),
    PRATA("Prata"),
    CINZA("Cinza"),
    VERMELHO("Vermelho"),
    AZUL("Azul"),
    VERDE("Verde"),
    AMARELO("Amarelo"),
    LARANJA("Laranja"),
    MARROM("Marrom"),
    BEGE("Bege"),
    DOURADO("Dourado"),
    ROSA("Rosa"),
    ROXO("Roxo"),
    VINHO("Vinho"),
    FANTASIA("Fantasia"),
    OUTROS("Outros");

    private final String displayName;

    VehicleColor(String displayName) {
        this.displayName = displayName;
    }
}
