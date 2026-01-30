package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Organization;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> withFilters(
            User.Role role,
            Long organizationId,
            Boolean enabled,
            String search) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (organizationId != null) {
                // User não tem mais organization, busca através de Organization.owner
                Subquery<Long> subquery = query.subquery(Long.class);
                var orgRoot = subquery.from(Organization.class);
                subquery.select(orgRoot.get("owner").get("id"))
                        .where(criteriaBuilder.equal(orgRoot.get("id"), organizationId));
                predicates.add(root.get("id").in(subquery));
            }

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            // Busca por nome, email ou username (case-insensitive e accent-insensitive)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                // Usa immutable_unaccent para busca insensível a acentos
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("name"))),
                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern)));
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.lower(root.get("username"))),
                        criteriaBuilder.function("immutable_unaccent", String.class, criteriaBuilder.literal(searchPattern)));

                predicates.add(criteriaBuilder.or(namePredicate, emailPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
