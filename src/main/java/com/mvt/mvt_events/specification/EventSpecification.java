package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasStatus(Event.EventStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasOrganizationId(Long organizationId) {
        return (root, query, cb) -> organizationId == null ? cb.conjunction()
                : cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Event> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? cb.conjunction()
                : cb.equal(root.join("categories").get("id"), categoryId);
    }

    public static Specification<Event> hasCity(String city) {
        return (root, query, cb) -> (city == null || city.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<Event> hasState(String state) {
        return (root, query, cb) -> (state == null || state.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("state")), state.toUpperCase());
    }

    public static Specification<Event> withFilters(
            Event.EventStatus status,
            Long organizationId,
            Long categoryId,
            String city,
            String state) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (organizationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), organizationId));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.join("categories").get("id"), categoryId));
            }

            if (city != null && !city.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("city")),
                        "%" + city.toLowerCase() + "%"));
            }

            if (state != null && !state.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("state")),
                        state.toUpperCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
