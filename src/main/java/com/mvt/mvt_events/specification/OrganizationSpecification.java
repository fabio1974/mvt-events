package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Organization;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrganizationSpecification {

    /**
     * Busca organizações por nome ou slug (accent-insensitive)
     */
    public static Specification<Organization> withSearch(String search) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + search.toLowerCase() + "%";

            // Usa immutable_unaccent para busca insensível a acentos
            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("name"))),
                            criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern))),
                    criteriaBuilder.like(
                            criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("slug"))),
                            criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern))));
        };
    }

    /**
     * Filtro por status ativo/inativo
     */
    public static Specification<Organization> hasActive(Boolean active) {
        return (root, query, cb) -> active == null ? cb.conjunction()
                : cb.equal(root.get("active"), active);
    }

    /**
     * Combinação de filtros (accent-insensitive)
     */
    public static Specification<Organization> withFilters(String search, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                // Usa immutable_unaccent para busca insensível a acentos
                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("name"))),
                                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern))),
                                criteriaBuilder.like(
                                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("slug"))),
                                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern)))));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filtro por ID da organização (tenant filter)
     */
    public static Specification<Organization> byOwnerId(Long organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.disjunction(); // Retorna falso se organizationId for null
            }
            return criteriaBuilder.equal(root.get("id"), organizationId);
        };
    }
}
