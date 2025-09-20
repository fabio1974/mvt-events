package com.mvt.mvt_events.jpa;

public enum TransferFrequency {
    IMMEDIATE("Immediate"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ON_DEMAND("On Demand");

    private final String displayName;

    TransferFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}