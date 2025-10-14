package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.Computed;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Event Category - Represents different categories within an event
 * (e.g., age groups, gender divisions, distances, etc.)
 */
@Entity
@Table(name = "event_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    @Visible(form = false, filter = true, table = true)
    private Event event;

    // Distance
    @Column(precision = 10, scale = 2)
    private BigDecimal distance;

    // Gender
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    // Age range
    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private DistanceUnit distanceUnit = DistanceUnit.KM;

    @DisplayLabel
    @Computed(function = "categoryName", dependencies = { "distance", "gender", "minAge", "maxAge", "distanceUnit" })
    @Column(nullable = false, length = 100)
    private String name;

    // Registration
    @Visible(filter = false, table = false, form = true)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Visible(form = false, table = false, filter = false)
    @Column(name = "current_participants", nullable = true)
    private Integer currentParticipants = 0;

    // Additional info
    @Column(columnDefinition = "TEXT")
    @Visible(filter = false, table = false, form = true)
    private String observations;

    // ============================================================================
    // ENUMS
    // ============================================================================

    public enum Gender {
        MALE("Masculino"),
        FEMALE("Feminino"),
        MIXED("Misto"),
        OTHER("Outro");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DistanceUnit {
        KM("Quilômetros"),
        MI("Milhas"),
        METERS("Metros");

        private final String displayName;

        DistanceUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Check if category is full
     */
    public boolean isFull() {
        return maxParticipants != null && currentParticipants >= maxParticipants;
    }

    /**
     * Check if category is available for registration
     */
    public boolean isAvailableForRegistration() {
        return !isFull();
    }

    /**
     * Check if a person with given age and gender can register
     */
    public boolean isEligible(Integer age, Gender personGender) {
        // Check age eligibility
        if (age != null) {
            if (minAge != null && age < minAge) {
                return false;
            }
            if (maxAge != null && age > maxAge) {
                return false;
            }
        }

        // Check gender eligibility
        if (gender != null && gender != Gender.MIXED && personGender != null) {
            return gender == personGender;
        }

        return true;
    }

    /**
     * Increment participant count
     */
    public void incrementParticipants() {
        if (isFull()) {
            throw new IllegalStateException("Category is full");
        }
        this.currentParticipants++;
    }

    /**
     * Decrement participant count
     */
    public void decrementParticipants() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
    }

    /**
     * Get available spots
     */
    public Integer getAvailableSpots() {
        if (maxParticipants == null) {
            return null; // Unlimited
        }
        return maxParticipants - currentParticipants;
    }

    /**
     * Get formatted age range
     */
    public String getAgeRangeFormatted() {
        if (minAge == null && maxAge == null) {
            return "Todas as idades";
        }
        if (minAge != null && maxAge != null) {
            return minAge + " - " + maxAge + " anos";
        }
        if (minAge != null) {
            return minAge + "+ anos";
        }
        return "Até " + maxAge + " anos";
    }

    /**
     * Get formatted distance
     */
    public String getDistanceFormatted() {
        if (distance == null) {
            return "";
        }
        DistanceUnit unit = distanceUnit != null ? distanceUnit : DistanceUnit.KM;
        return distance + " " + unit.getDisplayName();
    }
}
