package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByEventId(Long eventId);

    List<PaymentEvent> findByPaymentId(Long paymentId);

    List<PaymentEvent> findByTransferId(Long transferId);

    List<PaymentEvent> findByEventType(PaymentEvent.PaymentEventType eventType);

    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.event.id = :eventId AND pe.eventType = :eventType ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findByEventIdAndEventType(@Param("eventId") Long eventId,
            @Param("eventType") PaymentEvent.PaymentEventType eventType);

    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.event.id = :eventId AND pe.createdAt >= :since ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findByEventIdSince(@Param("eventId") Long eventId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(pe.amount), 0) FROM PaymentEvent pe WHERE pe.event.id = :eventId AND pe.eventType = :eventType")
    BigDecimal getTotalAmountByEventAndType(@Param("eventId") Long eventId,
            @Param("eventType") PaymentEvent.PaymentEventType eventType);

    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.event.organization.id = :organizationId AND pe.createdAt >= :since ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findByOrganizationIdSince(@Param("organizationId") Long organizationId,
            @Param("since") LocalDateTime since);

    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.eventType IN :eventTypes ORDER BY pe.createdAt DESC")
    List<PaymentEvent> findByEventTypes(@Param("eventTypes") List<PaymentEvent.PaymentEventType> eventTypes);
}