package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CashRegisterMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashRegisterMovementRepository extends JpaRepository<CashRegisterMovement, Long> {
    List<CashRegisterMovement> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
