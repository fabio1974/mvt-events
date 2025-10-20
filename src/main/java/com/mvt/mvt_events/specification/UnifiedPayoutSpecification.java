package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.UnifiedPayout;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications para UnifiedPayout
 */
public class UnifiedPayoutSpecification {

    // Filtros de identificação
    public static Specification<UnifiedPayout> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<UnifiedPayout> hasBeneficiaryId(UUID beneficiaryId) {
        return (root, query, cb) -> beneficiaryId == null ? cb.conjunction()
                : cb.equal(root.get("beneficiary").get("id"), beneficiaryId);
    }

    // Filtros de tipo
    public static Specification<UnifiedPayout> hasBeneficiaryType(UnifiedPayout.BeneficiaryType beneficiaryType) {
        return (root, query, cb) -> beneficiaryType == null ? cb.conjunction()
                : cb.equal(root.get("beneficiaryType"), beneficiaryType);
    }

    public static Specification<UnifiedPayout> isCourierPayout() {
        return (root, query, cb) -> cb.equal(root.get("beneficiaryType"),
                UnifiedPayout.BeneficiaryType.COURIER);
    }

    public static Specification<UnifiedPayout> isAdmPayout() {
        return (root, query, cb) -> cb.equal(root.get("beneficiaryType"),
                UnifiedPayout.BeneficiaryType.ADM);
    }

    // Filtros de período
    public static Specification<UnifiedPayout> hasPeriod(String period) {
        return (root, query, cb) -> period == null ? cb.conjunction()
                : cb.equal(root.get("period"), period);
    }

    public static Specification<UnifiedPayout> hasPeriodBetween(String startPeriod, String endPeriod) {
        return (root, query, cb) -> {
            if (startPeriod == null && endPeriod == null)
                return cb.conjunction();
            if (startPeriod != null && endPeriod != null)
                return cb.between(root.get("period"), startPeriod, endPeriod);
            if (startPeriod != null)
                return cb.greaterThanOrEqualTo(root.get("period"), startPeriod);
            return cb.lessThanOrEqualTo(root.get("period"), endPeriod);
        };
    }

    // Filtros de status
    public static Specification<UnifiedPayout> hasStatus(UnifiedPayout.PayoutStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<UnifiedPayout> isPending() {
        return (root, query, cb) -> cb.equal(root.get("status"), UnifiedPayout.PayoutStatus.PENDING);
    }

    public static Specification<UnifiedPayout> isCompleted() {
        return (root, query, cb) -> cb.equal(root.get("status"), UnifiedPayout.PayoutStatus.COMPLETED);
    }

    // Filtros de valores
    public static Specification<UnifiedPayout> hasTotalAmountGreaterThan(BigDecimal minAmount) {
        return (root, query, cb) -> minAmount == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
    }

    public static Specification<UnifiedPayout> hasTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Filtros de método de pagamento
    public static Specification<UnifiedPayout> hasPaymentMethod(UnifiedPayout.PayoutMethod paymentMethod) {
        return (root, query, cb) -> paymentMethod == null ? cb.conjunction()
                : cb.equal(root.get("paymentMethod"), paymentMethod);
    }

    // Filtros de datas
    public static Specification<UnifiedPayout> createdBetween(LocalDateTime start, LocalDateTime end) {
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

    public static Specification<UnifiedPayout> paidBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("paidAt")));
            if (start != null && end != null) {
                predicates.add(cb.between(root.get("paidAt"), start, end));
            } else if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paidAt"), start));
            } else {
                predicates.add(cb.lessThanOrEqualTo(root.get("paidAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filtro para payouts pendentes de um período específico
     */
    public static Specification<UnifiedPayout> pendingForPeriod(String period) {
        return hasPeriod(period).and(isPending());
    }

    /**
     * Filtro para payouts de courier em um período
     */
    public static Specification<UnifiedPayout> courierPayoutsForPeriod(String period) {
        return hasPeriod(period).and(isCourierPayout());
    }

    /**
     * Filtro para payouts de ADM em um período
     */
    public static Specification<UnifiedPayout> admPayoutsForPeriod(String period) {
        return hasPeriod(period).and(isAdmPayout());
    }
}
