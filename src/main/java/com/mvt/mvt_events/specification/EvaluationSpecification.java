package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Evaluation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications para Evaluation
 */
public class EvaluationSpecification {

    // Filtros de identificação
    public static Specification<Evaluation> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<Evaluation> hasDeliveryId(Long deliveryId) {
        return (root, query, cb) -> deliveryId == null ? cb.conjunction()
                : cb.equal(root.get("delivery").get("id"), deliveryId);
    }

    public static Specification<Evaluation> hasEvaluatorId(UUID evaluatorId) {
        return (root, query, cb) -> evaluatorId == null ? cb.conjunction()
                : cb.equal(root.get("evaluator").get("id"), evaluatorId);
    }

    // Filtros de rating
    public static Specification<Evaluation> hasRating(Integer rating) {
        return (root, query, cb) -> rating == null ? cb.conjunction()
                : cb.equal(root.get("rating"), rating);
    }

    public static Specification<Evaluation> hasRatingGreaterThan(Integer minRating) {
        return (root, query, cb) -> minRating == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Evaluation> hasRatingLessThan(Integer maxRating) {
        return (root, query, cb) -> maxRating == null ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("rating"), maxRating);
    }

    // Filtros de tipo
    public static Specification<Evaluation> hasEvaluationType(Evaluation.EvaluationType evaluationType) {
        return (root, query, cb) -> evaluationType == null ? cb.conjunction()
                : cb.equal(root.get("evaluationType"), evaluationType);
    }

    public static Specification<Evaluation> isClientToCourier() {
        return (root, query, cb) -> cb.equal(root.get("evaluationType"),
                Evaluation.EvaluationType.CLIENT_TO_COURIER);
    }

    public static Specification<Evaluation> isCourierToClient() {
        return (root, query, cb) -> cb.equal(root.get("evaluationType"),
                Evaluation.EvaluationType.COURIER_TO_CLIENT);
    }

    // Filtros de courier (avaliações recebidas)
    public static Specification<Evaluation> hasCourierId(UUID courierId) {
        return (root, query, cb) -> courierId == null ? cb.conjunction()
                : cb.equal(root.get("delivery").get("courier").get("id"), courierId);
    }

    public static Specification<Evaluation> courierReceivedEvaluations(UUID courierId) {
        return hasCourierId(courierId).and(isClientToCourier());
    }

    // Filtros de data
    public static Specification<Evaluation> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            if (start != null && end != null)
                return cb.between(root.get("createdAt"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            return cb.lessThanOrEqualTo(root.get("createdAt"), end);
        };
    }

    // Filtro por ADM (via Delivery)
    public static Specification<Evaluation> hasAdmId(UUID admId) {
        return (root, query, cb) -> admId == null ? cb.conjunction()
                : cb.equal(root.get("delivery").get("adm").get("id"), admId);
    }

    // Busca por texto (accent-insensitive)
    public static Specification<Evaluation> searchByComments(String text) {
        return (root, query, cb) -> {
            if (text == null || text.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            // Usa immutable_unaccent para busca insensível a acentos
            return cb.like(
                    cb.function("immutable_unaccent", String.class, cb.lower(root.get("comments"))),
                    cb.function("immutable_unaccent", String.class, cb.literal(pattern)));
        };
    }

    /**
     * Filtro para avaliações ruins (rating <= 2)
     */
    public static Specification<Evaluation> isPoorRating() {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("rating"), 2);
    }

    /**
     * Filtro para avaliações excelentes (rating >= 4)
     */
    public static Specification<Evaluation> isExcellentRating() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), 4);
    }
}
