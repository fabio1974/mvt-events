package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Payment;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    // Métodos de busca única - mantidos
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    // Métodos de agregação e cálculos - mantidos (lógica de negócio)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.registration.event.id = :eventId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.registration.event.organization.id = :organizationId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidByOrganization(@Param("organizationId") Long organizationId);

    // Queries complexas com datas - mantidos (lógica de negócio específica)
    @Query("SELECT p FROM Payment p WHERE p.registration.event.id = :eventId AND p.status = 'COMPLETED' AND p.processedAt >= :since")
    List<Payment> findCompletedPaymentsByEventSince(@Param("eventId") Long eventId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING'")
    long countPendingPayments();

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt <= :beforeDate")
    List<Payment> findPendingPaymentsBefore(@Param("beforeDate") LocalDateTime beforeDate);

    // Override findAll with Specification to include EntityGraph
    @Override
    @EntityGraph(attributePaths = { "registration" })
    @NonNull
    Page<Payment> findAll(@Nullable Specification<Payment> spec, @NonNull Pageable pageable);

    // Override findById to include EntityGraph
    @Override
    @EntityGraph(attributePaths = { "registration" })
    @NonNull
    Optional<Payment> findById(@NonNull Long id);
}