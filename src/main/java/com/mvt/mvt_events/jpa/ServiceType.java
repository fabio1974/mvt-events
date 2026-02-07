package com.mvt.mvt_events.jpa;

/**
 * Tipo de serviço prestado por um motorista
 */
public enum ServiceType {
    DELIVERY("Entrega"),
    PASSENGER_TRANSPORT("Transporte de Passageiro"),
    BOTH("Ambos");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
