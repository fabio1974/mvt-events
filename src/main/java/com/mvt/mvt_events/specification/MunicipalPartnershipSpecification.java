package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.MunicipalPartnership;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Specifications para MunicipalPartnership
 */
public class MunicipalPartnershipSpecification {

    // Filtros de identificação
    public static Specification<MunicipalPartnership> hasId(Long id) {
        return (root, query, cb) -> id == null ? cb.conjunction()
                : cb.equal(root.get("id"), id);
    }

    public static Specification<MunicipalPartnership> hasCnpj(String cnpj) {
        return (root, query, cb) -> cnpj == null ? cb.conjunction()
                : cb.equal(root.get("cnpj"), cnpj);
    }

    // Filtros de localização
    public static Specification<MunicipalPartnership> hasCity(String city) {
        return (root, query, cb) -> city == null ? cb.conjunction()
                : cb.equal(root.get("city"), city);
    }

    public static Specification<MunicipalPartnership> hasState(String state) {
        return (root, query, cb) -> state == null ? cb.conjunction()
                : cb.equal(root.get("state"), state);
    }

    public static Specification<MunicipalPartnership> hasCityAndState(String city, String state) {
        return hasCity(city).and(hasState(state));
    }

    // Filtros de status
    public static Specification<MunicipalPartnership> hasStatus(MunicipalPartnership.PartnershipStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<MunicipalPartnership> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"),
                MunicipalPartnership.PartnershipStatus.ACTIVE);
    }

    // Filtros de vigência
    public static Specification<MunicipalPartnership> isValidOn(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null)
                return cb.conjunction();

            return cb.and(
                    cb.lessThanOrEqualTo(root.get("startDate"), date),
                    cb.or(
                            cb.isNull(root.get("endDate")),
                            cb.greaterThanOrEqualTo(root.get("endDate"), date)));
        };
    }

    public static Specification<MunicipalPartnership> isCurrentlyValid() {
        return isValidOn(LocalDate.now());
    }

    public static Specification<MunicipalPartnership> hasExpired() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("endDate")),
                cb.lessThan(root.get("endDate"), LocalDate.now()));
    }

    // Busca por texto
    public static Specification<MunicipalPartnership> searchByText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("city")), pattern),
                    cb.like(cb.lower(root.get("agreementNumber")), pattern));
        };
    }

    /**
     * Filtro para parcerias ativas e válidas
     */
    public static Specification<MunicipalPartnership> activeAndValid() {
        return isActive().and(isCurrentlyValid());
    }

    /**
     * Filtro para parcerias de uma cidade específica que estejam ativas
     */
    public static Specification<MunicipalPartnership> activeByCityAndState(String city, String state) {
        return isActive()
                .and(hasCity(city))
                .and(hasState(state));
    }
}
