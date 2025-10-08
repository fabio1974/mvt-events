package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.EventCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EventUpdateRequest {

    private Long organizationId;
    private String name;
    private String slug;
    private String description;
    private Event.EventType eventType;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String location;
    private String address;
    private Integer maxParticipants;
    private Boolean registrationOpen;
    private LocalDate registrationStartDate;
    private LocalDate registrationEndDate;
    private BigDecimal price;
    private String currency;
    private String bannerUrl;
    private BigDecimal platformFeePercentage;
    private String termsAndConditions;
    private String transferFrequency;
    private Event.EventStatus status;

    // Categories to update/create/delete
    private List<CategoryUpdateRequest> categories;

    @Data
    public static class CategoryUpdateRequest {
        private Long id; // null for new categories
        private String name;
        private Integer minAge;
        private Integer maxAge;
        private EventCategory.Gender gender;
        private BigDecimal distance;
        private String distanceUnit;
        private BigDecimal price;
        private Integer maxParticipants;
        private Boolean isActive;
        private String observations;
        private Boolean _delete = false; // flag to delete category
    }
}
