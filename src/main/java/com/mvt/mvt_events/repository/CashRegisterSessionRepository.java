package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CashRegisterSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSession, Long> {

    Optional<CashRegisterSession> findByClientIdAndStatus(UUID clientId, CashRegisterSession.Status status);

    @Query("SELECT s FROM CashRegisterSession s LEFT JOIN FETCH s.movements " +
           "WHERE s.client.id = :clientId AND s.status = 'OPEN'")
    Optional<CashRegisterSession> findOpenWithMovements(@Param("clientId") UUID clientId);

    @Query("SELECT s FROM CashRegisterSession s LEFT JOIN FETCH s.movements " +
           "WHERE s.client.id = :clientId " +
           "AND ((s.openedAt < :end) AND (s.closedAt IS NULL OR s.closedAt >= :start)) " +
           "ORDER BY s.openedAt DESC")
    List<CashRegisterSession> findOverlapping(
            @Param("clientId") UUID clientId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    List<CashRegisterSession> findByClientIdOrderByOpenedAtDesc(UUID clientId);
}
