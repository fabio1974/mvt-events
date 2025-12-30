package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository
        extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    Optional<Organization> findBySlug(String slug);
    
    Optional<Organization> findByOwner(User owner);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Find organization by ID with contracts loaded (but NOT the users)
     * Users will be loaded separately to avoid circular reference
     */
    @Query("SELECT DISTINCT o FROM Organization o " +
            "LEFT JOIN FETCH o.employmentContracts " +
            "LEFT JOIN FETCH o.clientContracts " +
            "WHERE o.id = :id")
    Optional<Organization> findByIdWithRelationships(Long id);

    /**
     * Get courier IDs for organization's employment contracts
     */
    @Query("SELECT ec.courier.id FROM EmploymentContract ec WHERE ec.organization.id = :organizationId")
    java.util.List<java.util.UUID> findCourierIdsByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Get client IDs for organization's service contracts
     */
    @Query("SELECT c.client.id FROM ClientContract c WHERE c.organization.id = :organizationId")
    java.util.List<java.util.UUID> findClientIdsByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Override findAll with pagination to eagerly load owner
     */
    @EntityGraph(attributePaths = { "owner" })
    @NonNull
    Page<Organization> findAll(@NonNull Pageable pageable);

    /**
     * Override findAll with Specification to eagerly load owner
     */
    @EntityGraph(attributePaths = { "owner" })
    @NonNull
    Page<Organization> findAll(org.springframework.data.jpa.domain.Specification<Organization> spec, @NonNull Pageable pageable);
}