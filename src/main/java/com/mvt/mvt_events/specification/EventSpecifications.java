package com.mvt.mvt_events.specification;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.Organization;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para filtragem de Events
 */
public class EventSpecifications {

    /**
     * Filtra eventos pela organização
     * 
     * @param organizationId ID da organização
     * @return Specification que filtra por organization_id
     */
    public static Specification<Event> hasOrganization(Long organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction(); // Retorna todos se null
            }

            Join<Event, Organization> organizationJoin = root.join("organization");
            return criteriaBuilder.equal(organizationJoin.get("id"), organizationId);
        };
    }

    /**
     * Filtro principal para organização (alias para hasOrganization)
     * 
     * @param organizationId ID da organização
     * @return Specification de organização
     */
    public static Specification<Event> forOrganization(Long organizationId) {
        return hasOrganization(organizationId);
    }
}
