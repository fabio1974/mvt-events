package com.mvt.mvt_events.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import com.mvt.mvt_events.jpa.Event;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Métodos de busca única e validação - mantidos
    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Query otimizada em 2 etapas para evitar paginação em memória:
    // 1. Busca IDs com paginação (rápido, sem JOINs)
    @Query("SELECT e.id FROM Event e")
    Page<Long> findAllEventIds(Pageable pageable);

    // 2. Busca eventos completos por IDs (com EntityGraph)
    @EntityGraph(attributePaths = { "categories", "organization", "city" })
    @Query("SELECT e FROM Event e WHERE e.id IN :ids")
    List<Event> findAllByIds(@org.springframework.data.repository.query.Param("ids") List<Long> ids);

    // Teste: busca sem @Query para ver se o problema é com JPQL custom
    @EntityGraph(attributePaths = { "categories", "organization", "city" })
    List<Event> findByIdIn(List<Long> ids);

    // Teste 2: SEM EntityGraph (override do método padrão)
    @Override
    @NonNull
    List<Event> findAllById(@NonNull Iterable<Long> ids);

    // Override findAll with Specification - SEM EntityGraph para evitar HHH90003004
    // Categories, organization e city serão carregados LAZY quando acessados
    @Override
    @NonNull
    Page<Event> findAll(@Nullable Specification<Event> spec, @NonNull Pageable pageable);

    // Override findAll with Pageable - SEM EntityGraph para evitar HHH90003004
    @Override
    @NonNull
    Page<Event> findAll(@NonNull Pageable pageable);

    // Override findById - SEM EntityGraph para evitar bug
    // Relações serão carregadas LAZY no Service
    @Override
    @NonNull
    Optional<Event> findById(@NonNull Long id);

    // DEBUG: Query nativa que ignora o filtro de tenant
    @Query(value = "SELECT COUNT(*) FROM events WHERE organization_id = :orgId", nativeQuery = true)
    Long countByOrganizationIdNative(Long orgId);

    @Query(value = "SELECT * FROM events WHERE organization_id = :orgId LIMIT 10", nativeQuery = true)
    java.util.List<Event> findTop10ByOrganizationIdNative(Long orgId);
}