package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.Registration;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class MyRegistrationResponse {
    private Long id;
    private LocalDateTime registrationDate;
    private Registration.RegistrationStatus status;
    private EventSummary event;
    private UserSummary user;
    private List<PaymentSummary> payments;

    @Data
    public static class EventSummary {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime eventDate;
        private String location;
        private java.math.BigDecimal price;
    }

    @Data
    public static class UserSummary {
        private UUID id;
        private String name;
    }

    @Data
    public static class PaymentSummary {
        private Long id;
        private java.math.BigDecimal amount;
        private String paymentMethod;
        private String status;
        private LocalDateTime createdAt;
    }
}