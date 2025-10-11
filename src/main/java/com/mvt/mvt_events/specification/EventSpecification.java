package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.TransferFrequency;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    // Filtros individuais
    public static Specification<Event> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<Event> hasOrganizationId(Long organizationId) {
        return (root, query, cb) -> organizationId == null ? cb.conjunction()
                : cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Event> hasName(String name) {
        return (root, query, cb) -> (name == null || name.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Event> hasSlug(String slug) {
        return (root, query, cb) -> (slug == null || slug.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.lower(root.get("slug")), slug.toLowerCase());
    }

    public static Specification<Event> hasDescription(String description) {
        return (root, query, cb) -> (description == null || description.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }

    public static Specification<Event> hasEventType(Event.EventType eventType) {
        return (root, query, cb) -> eventType == null ? cb.conjunction()
                : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<Event> hasEventDate(LocalDateTime eventDate) {
        return (root, query, cb) -> eventDate == null ? cb.conjunction()
                : cb.equal(root.get("eventDate"), eventDate);
    }

    public static Specification<Event> hasEventDateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            if (start != null && end != null)
                return cb.between(root.get("eventDate"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("eventDate"), start);
            return cb.lessThanOrEqualTo(root.get("eventDate"), end);
        };
    }

    public static Specification<Event> hasCityId(Long cityId) {
        return (root, query, cb) -> cityId == null ? cb.conjunction()
                : cb.equal(root.get("city").get("id"), cityId);
    }

    public static Specification<Event> hasCity(String city) {
        return (root, query, cb) -> (city == null || city.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("city").get("name")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<Event> hasLocation(String location) {
        return (root, query, cb) -> (location == null || location.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Event> hasMaxParticipants(Integer maxParticipants) {
        return (root, query, cb) -> maxParticipants == null ? cb.conjunction()
                : cb.equal(root.get("maxParticipants"), maxParticipants);
    }

    public static Specification<Event> hasMaxParticipantsGreaterThan(Integer min) {
        return (root, query, cb) -> min == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("maxParticipants"), min);
    }

    public static Specification<Event> hasRegistrationOpen(Boolean registrationOpen) {
        return (root, query, cb) -> registrationOpen == null ? cb.conjunction()
                : cb.equal(root.get("registrationOpen"), registrationOpen);
    }

    public static Specification<Event> hasRegistrationStartDate(LocalDate registrationStartDate) {
        return (root, query, cb) -> registrationStartDate == null ? cb.conjunction()
                : cb.equal(root.get("registrationStartDate"), registrationStartDate);
    }

    public static Specification<Event> hasRegistrationEndDate(LocalDate registrationEndDate) {
        return (root, query, cb) -> registrationEndDate == null ? cb.conjunction()
                : cb.equal(root.get("registrationEndDate"), registrationEndDate);
    }

    public static Specification<Event> hasPrice(BigDecimal price) {
        return (root, query, cb) -> price == null ? cb.conjunction()
                : cb.equal(root.get("price"), price);
    }

    public static Specification<Event> hasPriceBetween(BigDecimal min, BigDecimal max) {
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

    public static Specification<Event> hasCurrency(String currency) {
        return (root, query, cb) -> (currency == null || currency.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("currency")), currency.toUpperCase());
    }

    public static Specification<Event> hasStatus(Event.EventStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasTransferFrequency(TransferFrequency transferFrequency) {
        return (root, query, cb) -> transferFrequency == null ? cb.conjunction()
                : cb.equal(root.get("transferFrequency"), transferFrequency);
    }

    public static Specification<Event> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? cb.conjunction()
                : cb.equal(root.join("categories").get("id"), categoryId);
    }

    // Filtro legado para state (removido da entidade, mas mantido para
    // compatibilidade)
    public static Specification<Event> hasState(String state) {
        return (root, query, cb) -> cb.conjunction(); // Não faz nada, campo removido
    }

    /**
     * Método principal de filtros combinados.
     * Aceita todos os campos mais comuns para filtragem.
     */
    public static Specification<Event> withFilters(
            Event.EventStatus status,
            Long organizationId,
            Long categoryId,
            String city,
            String state,
            Event.EventType eventType,
            String name) {

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
                        criteriaBuilder.lower(root.get("city").get("name")),
                        "%" + city.toLowerCase() + "%"));
            }

            if (eventType != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
            }

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
