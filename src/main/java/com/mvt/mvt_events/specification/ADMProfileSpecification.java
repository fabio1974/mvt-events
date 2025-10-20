package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.ADMProfile;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Specifications para ADMProfile
 * ADM é o TENANT da aplicação
 */
public class ADMProfileSpecification {

    // Filtros de identificação
    public static Specification<ADMProfile> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<ADMProfile> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("user").get("id"), userId);
    }

    // Filtros de região (TENANT)
    public static Specification<ADMProfile> hasRegion(String region) {
        return (root, query, cb) -> region == null ? cb.conjunction()
                : cb.equal(root.get("region"), region);
    }

    // Filtros de status
    public static Specification<ADMProfile> hasStatus(ADMProfile.ADMStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<ADMProfile> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ADMProfile.ADMStatus.ACTIVE);
    }

    // Filtros de parceria
    public static Specification<ADMProfile> hasPartnershipId(Long partnershipId) {
        return (root, query, cb) -> partnershipId == null ? cb.conjunction()
                : cb.equal(root.get("partnership").get("id"), partnershipId);
    }

    // Filtros de comissão
    public static Specification<ADMProfile> hasCommissionPercentageGreaterThan(BigDecimal minPercentage) {
        return (root, query, cb) -> minPercentage == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("commissionPercentage"), minPercentage);
    }

    // Filtros de métricas
    public static Specification<ADMProfile> hasManagedCouriersGreaterThan(Integer minCouriers) {
        return (root, query, cb) -> minCouriers == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("managedCouriers"), minCouriers);
    }

    public static Specification<ADMProfile> hasTotalDeliveriesGreaterThan(Integer minDeliveries) {
        return (root, query, cb) -> minDeliveries == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("totalDeliveries"), minDeliveries);
    }

    // Busca por texto
    public static Specification<ADMProfile> searchByText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("user").get("name")), pattern),
                    cb.like(cb.lower(root.get("region")), pattern));
        };
    }

    /**
     * Filtro para ADMs ativos de uma região específica
     */
    public static Specification<ADMProfile> activeInRegion(String region) {
        return isActive().and(hasRegion(region));
    }
}
