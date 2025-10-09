package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.User;
import jakarta.persistence.criteria.Predicate;
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
                predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), organizationId));
            }

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            // Busca por nome, email ou username (case-insensitive)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")), searchPattern);

                predicates.add(criteriaBuilder.or(namePredicate, emailPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
