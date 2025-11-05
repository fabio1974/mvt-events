package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.CourierProfile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Specifications para CourierProfile
 */
public class CourierProfileSpecification {

    // Filtros de identificação
    public static Specification<CourierProfile> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<CourierProfile> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction()
                : cb.equal(root.get("user").get("id"), userId);
    }

    // Filtros de status
    public static Specification<CourierProfile> hasStatus(CourierProfile.CourierStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<CourierProfile> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), CourierProfile.CourierStatus.AVAILABLE);
    }

    // Filtros de veículo
    public static Specification<CourierProfile> hasVehicleType(CourierProfile.VehicleType vehicleType) {
        return (root, query, cb) -> vehicleType == null ? cb.conjunction()
                : cb.equal(root.get("vehicleType"), vehicleType);
    }

    public static Specification<CourierProfile> hasVehiclePlate(String vehiclePlate) {
        return (root, query, cb) -> vehiclePlate == null ? cb.conjunction()
                : cb.equal(root.get("vehiclePlate"), vehiclePlate);
    }

    // Filtros de rating
    public static Specification<CourierProfile> hasRatingGreaterThan(BigDecimal minRating) {
        return (root, query, cb) -> minRating == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<CourierProfile> hasRatingBetween(BigDecimal minRating, BigDecimal maxRating) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (maxRating != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), maxRating));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Filtros de métricas
    public static Specification<CourierProfile> hasTotalDeliveriesGreaterThan(Integer minDeliveries) {
        return (root, query, cb) -> minDeliveries == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("totalDeliveries"), minDeliveries);
    }

    public static Specification<CourierProfile> hasSuccessDeliveriesGreaterThan(Integer minSuccess) {
        return (root, query, cb) -> minSuccess == null ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("successfulDeliveries"), minSuccess);
    }

    // Filtro por ADM (via CourierADMLink)
    public static Specification<CourierProfile> hasAdmId(UUID admId) {
        return (root, query, cb) -> {
            if (admId == null)
                return cb.conjunction();

            // TODO: CourierADMLink removido - implementar filtro via EmploymentContract
            // var subquery = query.subquery(Long.class);
            // var linkRoot = subquery.from(com.mvt.mvt_events.jpa.CourierADMLink.class);
            // subquery.select(linkRoot.get("courierProfile").get("id"))
            // .where(cb.and(
            // cb.equal(linkRoot.get("admProfile").get("user").get("id"), admId),
            // cb.equal(linkRoot.get("isActive"), true)));
            //
            // return root.get("id").in(subquery);

            // Temporariamente retorna todos os couriers (sem filtro por ADM)
            return cb.conjunction();
        };
    }

    // Busca por texto
    public static Specification<CourierProfile> searchByText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("user").get("name")), pattern),
                    cb.like(cb.lower(root.get("vehiclePlate")), pattern));
        };
    }

    /**
     * Filtro combinado para couriers disponíveis
     */
    public static Specification<CourierProfile> availableForDelivery(UUID admId, BigDecimal minRating) {
        return isActive()
                .and(hasAdmId(admId))
                .and(hasRatingGreaterThan(minRating));
    }
}
