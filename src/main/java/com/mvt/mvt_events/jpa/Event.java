package com.mvt.mvt_events.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mvt.mvt_events.metadata.DisplayLabel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@EqualsAndHashCode(callSuper = true)
@FilterDef(name = "organizationFilter", parameters = @ParamDef(name = "organizationId", type = Long.class))
@Filter(name = "organizationFilter", condition = "organization_id = :organizationId")
public class Event extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @DisplayLabel
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "registration_open")
    private Boolean registrationOpen = true;

    @Column(name = "registration_start_date")
    private LocalDateTime registrationStartDate;

    @Column(name = "registration_end_date")
    private LocalDateTime registrationEndDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    private String currency = "BRL";

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EventStatus status = EventStatus.DRAFT;

    // Financial settings
    @Column(name = "platform_fee_percentage", precision = 5, scale = 4)
    private BigDecimal platformFeePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_frequency", length = 20)
    private TransferFrequency transferFrequency = TransferFrequency.WEEKLY;

    // Relationships
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

        LocalDateTime now = LocalDateTime.now();

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