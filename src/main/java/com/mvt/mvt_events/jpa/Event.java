package com.mvt.mvt_events.jpa;

import com.mvt.mvt_events.entity.City;
import com.mvt.mvt_events.metadata.DisplayLabel;
import com.mvt.mvt_events.metadata.Visible;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @Visible(form = false)
    private Organization organization;

    @DisplayLabel
    @Column(nullable = false)
    private String name;

    @Visible(table = false, form = false, filter = false)
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Visible(filter = false, table = false, form = true)
    @Size(max = 500)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType = EventType.RUNNING;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Visible(filter = false, table = false, form = true)
    @Column(length = 150, nullable = false)
    private String location;

    @Visible(filter = false, table = false, form = true)
    @Column(name = "max_participants")
    @Max(1000) // valor máximo permitido
    private Integer maxParticipants;

    @Visible(table = false, form = false, filter = false)
    @Column(name = "registration_open")
    private Boolean registrationOpen = true;

    @Visible(filter = false, table = false, form = true)
    @Column(name = "registration_start_date", nullable = false)
    private LocalDate registrationStartDate;

    @Visible(filter = false, table = false, form = true)
    @Column(name = "registration_end_date", nullable = false)
    private LocalDate registrationEndDate;

    @Visible(filter = false, table = false, form = true)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Visible(filter = false, table = false, form = true)
    @Column(length = 3)
    private String currency = "BRL";

    @Visible(table = false, form = false, filter = false)
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EventStatus status = EventStatus.DRAFT;

    // Financial settings
    @Visible(table = false, form = false, filter = false)
    @Column(name = "platform_fee_percentage", precision = 5, scale = 4)
    private BigDecimal platformFeePercentage;

    @Visible(filter = false, table = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_frequency", length = 20)
    private TransferFrequency transferFrequency = TransferFrequency.WEEKLY;

    // Relationships
    @Visible(form = true)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT) // Evita MultipleBagFetchException
    private List<EventCategory> categories = new ArrayList<>();

    // Enums
    public enum EventType {
        RUNNING("Running"),
        CYCLING("Cycling"),
        TRIATHLON("Triathlon"),
        SWIMMING("Swimming"),
        WALKING("Walking"),
        TRAIL_RUNNING("Trail Running"),
        MOUNTAIN_BIKING("Mountain Biking"),
        ROAD_CYCLING("Road Cycling"),
        MARATHON("Marathon"),
        HALF_MARATHON("Half Marathon"),
        ULTRA_MARATHON("Ultra Marathon"),
        OBSTACLE_RACE("Obstacle Race"),
        DUATHLON("Duathlon"),
        OTHER("Other");

        private final String displayName;

        EventType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum EventStatus {
        DRAFT("Rascunho"),
        PUBLISHED("Publicado"),
        CANCELLED("Cancelado"),
        COMPLETED("Concluído");

        private final String displayName;

        EventStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public boolean isRegistrationOpen() {
        if (!registrationOpen) {
            return false;
        }

        LocalDate now = LocalDate.now();

        // Check if registration start date has passed
        if (registrationStartDate != null && now.isBefore(registrationStartDate)) {
            return false;
        }

        // Check if registration end date has not passed
        if (registrationEndDate != null && now.isAfter(registrationEndDate)) {
            return false;
        }

        return true;
    }
}