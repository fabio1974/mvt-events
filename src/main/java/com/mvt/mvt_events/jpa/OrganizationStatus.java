package com.mvt.mvt_events.jpa;

public enum OrganizationStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED;

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Ativa";
            case INACTIVE -> "Inativa";
            case SUSPENDED -> "Suspensa";
        };
    }
}
