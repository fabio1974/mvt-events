package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.EventCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventCategorySpecification {

    public static Specification<EventCategory> belongsToEvent(Long eventId) {
        return (root, query, cb) -> eventId == null ? cb.conjunction()
                : cb.equal(root.get("event").get("id"), eventId);
    }

    public static Specification<EventCategory> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? cb.conjunction()
                : cb.equal(root.get("isActive"), active);
    }

    public static Specification<EventCategory> buildSpecification(Long eventId) {
        return belongsToEvent(eventId);
    }

    public static Specification<EventCategory> withFilters(
            Long eventId,
            Boolean active) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventId != null) {
                predicates.add(criteriaBuilder.equal(root.get("event").get("id"), eventId));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
