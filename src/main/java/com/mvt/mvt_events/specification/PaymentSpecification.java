package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Payment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    // Filtros individuais
    public static Specification<Payment> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<Payment> hasTenantId(Long tenantId) {
        return (root, query, cb) -> tenantId == null ? cb.conjunction()
                : cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Payment> hasRegistrationId(Long registrationId) {
        return (root, query, cb) -> registrationId == null ? cb.conjunction()
                : cb.equal(root.get("registration").get("id"), registrationId);
    }

    public static Specification<Payment> hasAmount(BigDecimal amount) {
        return (root, query, cb) -> amount == null ? cb.conjunction()
                : cb.equal(root.get("amount"), amount);
    }

    public static Specification<Payment> hasAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return cb.conjunction();
            if (min != null && max != null)
                return cb.between(root.get("amount"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("amount"), min);
            return cb.lessThanOrEqualTo(root.get("amount"), max);
        };
    }

    public static Specification<Payment> hasCurrency(String currency) {
        return (root, query, cb) -> (currency == null || currency.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("currency")), currency.toUpperCase());
    }

    public static Specification<Payment> hasStatus(Payment.PaymentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Payment> hasPaymentMethod(String paymentMethod) {
        return (root, query, cb) -> (paymentMethod == null || paymentMethod.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("paymentMethod")), paymentMethod.toUpperCase());
    }

    public static Specification<Payment> hasGatewayProvider(String gatewayProvider) {
        return (root, query, cb) -> (gatewayProvider == null || gatewayProvider.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("gatewayProvider")), gatewayProvider.toUpperCase());
    }

    public static Specification<Payment> hasGatewayPaymentId(String gatewayPaymentId) {
        return (root, query, cb) -> (gatewayPaymentId == null || gatewayPaymentId.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(root.get("gatewayPaymentId"), gatewayPaymentId);
    }

    public static Specification<Payment> hasGatewayFee(BigDecimal gatewayFee) {
        return (root, query, cb) -> gatewayFee == null ? cb.conjunction()
                : cb.equal(root.get("gatewayFee"), gatewayFee);
    }

    public static Specification<Payment> hasProcessedAt(LocalDateTime processedAt) {
        return (root, query, cb) -> processedAt == null ? cb.conjunction()
                : cb.equal(root.get("processedAt"), processedAt);
    }

    public static Specification<Payment> hasProcessedAtBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null)
                return cb.conjunction();
            if (start != null && end != null)
                return cb.between(root.get("processedAt"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("processedAt"), start);
            return cb.lessThanOrEqualTo(root.get("processedAt"), end);
        };
    }

    public static Specification<Payment> hasRefundedAt(LocalDateTime refundedAt) {
        return (root, query, cb) -> refundedAt == null ? cb.conjunction()
                : cb.equal(root.get("refundedAt"), refundedAt);
    }

    public static Specification<Payment> hasRefundAmount(BigDecimal refundAmount) {
        return (root, query, cb) -> refundAmount == null ? cb.conjunction()
                : cb.equal(root.get("refundAmount"), refundAmount);
    }

    public static Specification<Payment> hasRefundReason(String refundReason) {
        return (root, query, cb) -> (refundReason == null || refundReason.trim().isEmpty()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("refundReason")), "%" + refundReason.toLowerCase() + "%");
    }

    /**
     * Método principal de filtros combinados
     */
    public static Specification<Payment> withFilters(
            Payment.PaymentStatus status,
            Long registrationId,
            String provider) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (registrationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("registration").get("id"), registrationId));
            }

            if (provider != null && !provider.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("gatewayProvider")),
                        provider.toUpperCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Alias para compatibilidade
    public static Specification<Payment> hasProvider(String provider) {
        return hasGatewayProvider(provider);
    }

    public static Specification<Payment> buildSpecification(
            Payment.PaymentStatus status,
            Long registrationId,
            String provider) {
        return withFilters(status, registrationId, provider);
    }
}
