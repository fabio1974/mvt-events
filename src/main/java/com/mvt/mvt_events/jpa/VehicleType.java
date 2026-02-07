package com.mvt.mvt_events.jpa;

/**
 * Tipo de veículo
 */
public enum VehicleType {
    MOTORCYCLE("Moto"),
    CAR("Automóvel");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
