package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Registration;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RegistrationSpecification {

    // Filtros individuais
    public static Specification<Registration> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<Registration> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Registration> hasEventId(Long eventId) {
        return (root, query, cb) -> eventId == null ? cb.conjunction()
                : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<Registration> hasRegistrationDate(LocalDateTime registrationDate) {
        return (root, query, cb) -> registrationDate == null ? cb.conjunction()
                : cb.equal(root.get("registrationDate"), registrationDate);
    }

    public static Specification<Registration> hasRegistrationDateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            if (start != null && end != null)
                return cb.between(root.get("registrationDate"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("registrationDate"), start);
            return cb.lessThanOrEqualTo(root.get("registrationDate"), end);
        };
    }

    public static Specification<Registration> hasStatus(Registration.RegistrationStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Registration> hasNotes(String notes) {
        return (root, query, cb) -> (notes == null || notes.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("notes")), "%" + notes.toLowerCase() + "%");
    }

    public static Specification<Registration> hasTenantId(Long tenantId) {
        return (root, query, cb) -> tenantId == null ? cb.conjunction()
                : cb.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Método principal de filtros combinados
     */
    public static Specification<Registration> withFilters(
            Registration.RegistrationStatus status,
            Long eventId,
            UUID userId) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (eventId != null) {
                predicates.add(criteriaBuilder.equal(root.get("event").get("id"), eventId));
            }

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
