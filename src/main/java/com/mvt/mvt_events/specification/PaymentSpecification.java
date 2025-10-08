package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Payment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<Payment> hasStatus(Payment.PaymentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Payment> hasRegistrationId(Long registrationId) {
        return (root, query, cb) -> registrationId == null ? cb.conjunction()
                : cb.equal(root.get("registration").get("id"), registrationId);
    }

    public static Specification<Payment> hasProvider(String provider) {
        return (root, query, cb) -> (provider == null || provider.trim().isEmpty()) ? cb.conjunction()
                : cb.equal(cb.upper(root.get("gatewayProvider")), provider.toUpperCase());
    }

    public static Specification<Payment> buildSpecification(
            Payment.PaymentStatus status,
            Long registrationId,
            String provider) {

        Specification<Payment> spec = hasStatus(status);
        if (registrationId != null) {
            spec = spec.and(hasRegistrationId(registrationId));
        }
        if (provider != null) {
            spec = spec.and(hasProvider(provider));
        }
        return spec;
    }

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
                        criteriaBuilder.upper(root.get("provider")),
                        provider.toUpperCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
