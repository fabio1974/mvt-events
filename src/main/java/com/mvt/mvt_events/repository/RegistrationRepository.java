package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository
                extends JpaRepository<Registration, Long>, JpaSpecificationExecutor<Registration> {

        // Métodos de contagem e existência - mantidos por serem específicos
        @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId")
        Long countByEventId(@Param("eventId") Long eventId);

        boolean existsByEventIdAndUserId(Long eventId, UUID userId);

        boolean existsByUserIdAndEventId(UUID userId, Long eventId);

        // Métodos com FETCH JOIN otimizados - mantidos por performance
        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event e " +
                        "LEFT JOIN FETCH e.categories " +
                        "LEFT JOIN FETCH r.payments " +
                        "WHERE r.id = :id")
        Optional<Registration> findByIdWithUserEventAndPayments(@Param("id") Long id);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event e " +
                        "LEFT JOIN FETCH e.categories " +
                        "LEFT JOIN FETCH r.payments " +
                        "WHERE r.user.id = :userId")
        List<Registration> findByUserIdWithUserEventAndPayments(@Param("userId") UUID userId);

        @Query("SELECT DISTINCT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event e " +
                        "LEFT JOIN FETCH e.categories")
        List<Registration> findAllWithUserAndEvent();

        // Métodos simples podem ser substituídos por Specifications, mas mantidos para
        // compatibilidade
        @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId")
        List<Registration> findByEventId(@Param("eventId") Long eventId);

        // Override findAll with Specification to include EntityGraph
        @Override
        @EntityGraph(attributePaths = { "user", "event", "payments" })
        @NonNull
        Page<Registration> findAll(@Nullable Specification<Registration> spec, @NonNull Pageable pageable);

        // Override findById to include EntityGraph
        @Override
        @EntityGraph(attributePaths = { "user", "event", "payments" })
        @NonNull
        Optional<Registration> findById(@NonNull Long id);
}