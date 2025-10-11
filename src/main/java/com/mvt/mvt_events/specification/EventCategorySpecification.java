package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.EventCategory;
import com.mvt.mvt_events.jpa.EventCategory.Gender;
import com.mvt.mvt_events.jpa.EventCategory.DistanceUnit;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EventCategorySpecification {

    // Filtros individuais
    public static Specification<EventCategory> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<EventCategory> hasEventId(Long eventId) {
        return (root, query, cb) -> eventId == null ? cb.conjunction()
                : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<EventCategory> hasName(String name) {
        return (root, query, cb) -> (name == null || name.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<EventCategory> hasMinAge(Integer minAge) {
        return (root, query, cb) -> minAge == null ? cb.conjunction()
                : cb.equal(root.get("minAge"), minAge);
    }

    public static Specification<EventCategory> hasMaxAge(Integer maxAge) {
        return (root, query, cb) -> maxAge == null ? cb.conjunction()
                : cb.equal(root.get("maxAge"), maxAge);
    }

    public static Specification<EventCategory> hasGender(Gender gender) {
        return (root, query, cb) -> gender == null ? cb.conjunction()
                : cb.equal(root.get("gender"), gender);
    }

    public static Specification<EventCategory> hasDistance(BigDecimal distance) {
        return (root, query, cb) -> distance == null ? cb.conjunction()
                : cb.equal(root.get("distance"), distance);
    }

    public static Specification<EventCategory> hasDistanceUnit(DistanceUnit distanceUnit) {
        return (root, query, cb) -> distanceUnit == null ? cb.conjunction()
                : cb.equal(root.get("distanceUnit"), distanceUnit);
    }

    public static Specification<EventCategory> hasPrice(BigDecimal price) {
        return (root, query, cb) -> price == null ? cb.conjunction()
                : cb.equal(root.get("price"), price);
    }

    public static Specification<EventCategory> hasPriceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return cb.conjunction();
            if (min != null && max != null)
                return cb.between(root.get("price"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<EventCategory> hasMaxParticipants(Integer maxParticipants) {
        return (root, query, cb) -> maxParticipants == null ? cb.conjunction()
                : cb.equal(root.get("maxParticipants"), maxParticipants);
    }

    public static Specification<EventCategory> hasCurrentParticipants(Integer currentParticipants) {
        return (root, query, cb) -> currentParticipants == null ? cb.conjunction()
                : cb.equal(root.get("currentParticipants"), currentParticipants);
    }

    public static Specification<EventCategory> hasAvailableSpots() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("maxParticipants")),
                cb.lessThan(root.get("currentParticipants"), root.get("maxParticipants")));
    }

    public static Specification<EventCategory> hasTenantId(Long tenantId) {
        return (root, query, cb) -> tenantId == null ? cb.conjunction()
                : cb.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Método principal de filtros combinados
     */
    public static Specification<EventCategory> withFilters(
            Long eventId,
            Gender gender,
            DistanceUnit distanceUnit) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventId != null) {
                predicates.add(criteriaBuilder.equal(root.get("event").get("id"), eventId));
            }

            if (gender != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            if (distanceUnit != null) {
                predicates.add(criteriaBuilder.equal(root.get("distanceUnit"), distanceUnit));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Alias para compatibilidade
    public static Specification<EventCategory> belongsToEvent(Long eventId) {
        return hasEventId(eventId);
    }

    public static Specification<EventCategory> buildSpecification(Long eventId) {
        return hasEventId(eventId);
    }
}
