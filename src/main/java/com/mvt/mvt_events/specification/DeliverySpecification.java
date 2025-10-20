package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Delivery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications para Delivery
 * IMPORTANTE: Todas as queries devem filtrar por ADM (tenant)
 */
public class DeliverySpecification {

    /**
     * TENANT FILTER - CRÍTICO
     * Todas as queries devem incluir este filtro
     */
    public static Specification<Delivery> hasAdmId(UUID admId) {
        return (root, query, cb) -> admId == null ? cb.conjunction()
                : cb.equal(root.get("adm").get("id"), admId);
    }

    // Filtros de identificação
    public static Specification<Delivery> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<Delivery> hasClientId(UUID clientId) {
        return (root, query, cb) -> clientId == null ? cb.conjunction()
                : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Delivery> hasCourierId(UUID courierId) {
        return (root, query, cb) -> courierId == null ? cb.conjunction()
                : cb.equal(root.get("courier").get("id"), courierId);
    }

    public static Specification<Delivery> hasPartnershipId(Long partnershipId) {
        return (root, query, cb) -> partnershipId == null ? cb.conjunction()
                : cb.equal(root.get("partnership").get("id"), partnershipId);
    }

    // Filtros de status
    public static Specification<Delivery> hasStatus(Delivery.DeliveryStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Delivery> hasStatusIn(List<Delivery.DeliveryStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty() ? cb.conjunction()
                : root.get("status").in(statuses);
    }

    // Filtros de valores
    public static Specification<Delivery> hasTotalAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), max));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Filtros de datas
    public static Specification<Delivery> createdBetween(LocalDateTime start, LocalDateTime end) {
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

    public static Specification<Delivery> scheduledPickupBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            if (start != null && end != null)
                return cb.between(root.get("scheduledPickupAt"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("scheduledPickupAt"), start);
            return cb.lessThanOrEqualTo(root.get("scheduledPickupAt"), end);
        };
    }

    public static Specification<Delivery> completedBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("completedAt")));
            if (start != null && end != null) {
                predicates.add(cb.between(root.get("completedAt"), start, end));
            } else if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), start));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("completedAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Filtros de localização
    public static Specification<Delivery> hasFromCity(String city) {
        return (root, query, cb) -> city == null ? cb.conjunction()
                : cb.equal(root.get("fromCity"), city);
    }

    public static Specification<Delivery> hasToCity(String city) {
        return (root, query, cb) -> city == null ? cb.conjunction()
                : cb.equal(root.get("toCity"), city);
    }

    // Filtros especiais
    public static Specification<Delivery> isPendingAssignment() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Delivery.DeliveryStatus.PENDING),
                cb.isNull(root.get("courier")));
    }

    public static Specification<Delivery> isActive() {
        return (root, query, cb) -> root.get("status").in(
                Delivery.DeliveryStatus.ASSIGNED,
                Delivery.DeliveryStatus.PICKED_UP,
                Delivery.DeliveryStatus.IN_TRANSIT);
    }

    public static Specification<Delivery> hasNoEvaluation() {
        return (root, query, cb) -> cb.isEmpty(root.get("evaluations"));
    }

    // Busca por texto
    public static Specification<Delivery> searchByText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fromAddress")), pattern),
                    cb.like(cb.lower(root.get("toAddress")), pattern),
                    cb.like(cb.lower(root.get("recipientName")), pattern),
                    cb.like(cb.lower(root.get("recipientPhone")), pattern));
        };
    }

    /**
     * Filtro combinado com TENANT obrigatório
     * Exemplo de uso:
     * Specification<Delivery> spec = DeliverySpecification.buildSpec(admId,
     * clientId, status, startDate, endDate);
     */
    public static Specification<Delivery> buildSpec(UUID admId, UUID clientId,
            Delivery.DeliveryStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        return hasAdmId(admId)
                .and(hasClientId(clientId))
                .and(hasStatus(status))
                .and(createdBetween(startDate, endDate));
    }
}
