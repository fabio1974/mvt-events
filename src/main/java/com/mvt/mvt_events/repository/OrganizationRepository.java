package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlug(String slug);

    Optional<Organization> findByContactEmail(String contactEmail);

    boolean existsBySlug(String slug);

    boolean existsByContactEmail(String contactEmail);

    boolean existsBySlugAndIdNot(String slug, Long id);
}