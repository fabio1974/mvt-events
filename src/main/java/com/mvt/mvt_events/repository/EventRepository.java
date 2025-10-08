package com.mvt.mvt_events.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mvt.mvt_events.jpa.Event;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Métodos de busca única e validação - mantidos
    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Métodos com FETCH JOIN otimizados - mantidos por performance
    // Use @EntityGraph instead of JOIN FETCH for pagination to avoid in-memory
    // pagination
    @EntityGraph(attributePaths = { "categories" })
    @Query("SELECT DISTINCT e FROM Event e")
    Page<Event> findAllWithCategories(Pageable pageable);
}