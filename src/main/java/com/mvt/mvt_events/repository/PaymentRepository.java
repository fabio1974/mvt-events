package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByRegistrationId(Long registrationId);

    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.registration.event.id = :eventId")
    List<Payment> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT p FROM Payment p WHERE p.registration.event.organization.id = :organizationId")
    List<Payment> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.registration.event.id = :eventId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.registration.event.organization.id = :organizationId AND p.status = 'COMPLETED'")
    BigDecimal getTotalPaidByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT p FROM Payment p WHERE p.registration.event.id = :eventId AND p.status = 'COMPLETED' AND p.processedAt >= :since")
    List<Payment> findCompletedPaymentsByEventSince(@Param("eventId") Long eventId,
            @Param("since") LocalDateTime since);

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING'")
    long countPendingPayments();

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt <= :beforeDate")
    List<Payment> findPendingPaymentsBefore(@Param("beforeDate") LocalDateTime beforeDate);
}