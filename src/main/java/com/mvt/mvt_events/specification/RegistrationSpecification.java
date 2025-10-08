package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Registration;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class RegistrationSpecification {

    public static Specification<Registration> hasStatus(Registration.RegistrationStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Registration> hasEventId(Long eventId) {
        return (root, query, criteriaBuilder) -> {
            if (eventId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("event").get("id"), eventId);
        };
    }

    public static Specification<Registration> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    /**
     * Combina todos os filtros
     */
    public static Specification<Registration> withFilters(
            Registration.RegistrationStatus status,
            Long eventId,
            UUID userId) {

        Specification<Registration> spec = hasStatus(status);
        if (eventId != null) {
            spec = spec.and(hasEventId(eventId));
        }
        if (userId != null) {
            spec = spec.and(hasUserId(userId));
        }
        return spec;
    }
}
