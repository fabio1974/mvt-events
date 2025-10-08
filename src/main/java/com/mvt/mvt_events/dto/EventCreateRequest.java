package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EventCreateRequest {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @NotBlank(message = "Name is required")
    private String name;

    private String slug;

    private String description;

    @NotNull(message = "Event type is required")
    private Event.EventType eventType;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    private LocalTime eventTime;

    private String location;

    private String address;

    @Positive(message = "Max participants must be positive")
    private Integer maxParticipants;

    private Boolean registrationOpen = true;

    private LocalDate registrationStartDate;

    private LocalDate registrationEndDate;

    private BigDecimal price;

    private String currency = "BRL";

    private String bannerUrl;

    private BigDecimal platformFeePercentage;

    private String termsAndConditions;

    private String transferFrequency;

    // Categories
    private List<CategoryRequest> categories;

    @Data
    public static class CategoryRequest {
        private String name;
        private Integer minAge;
        private Integer maxAge;
        private EventCategory.Gender gender;
        private BigDecimal distance;
        private String distanceUnit;
        private BigDecimal price;
        private Integer maxParticipants;
        private Boolean isActive = true;
        private String observations;
    }
}