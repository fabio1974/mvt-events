package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByEventId(Long eventId);

    List<Transfer> findByOrganizationId(Long organizationId);

    List<Transfer> findByStatus(Transfer.TransferStatus status);

    @Query("SELECT t FROM Transfer t WHERE t.status = :status AND t.requestedAt <= :beforeDate")
    List<Transfer> findByStatusAndRequestedBefore(@Param("status") Transfer.TransferStatus status,
            @Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT t FROM Transfer t WHERE t.status = 'FAILED' AND t.retryCount < 3")
    List<Transfer> findFailedTransfersForRetry();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.event.id = :eventId AND t.status = 'COMPLETED'")
    BigDecimal getTotalTransferredByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.organization.id = :organizationId AND t.status = 'COMPLETED'")
    BigDecimal getTotalTransferredByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT COALESCE(SUM(t.gatewayFee), 0) FROM Transfer t WHERE t.organization.id = :organizationId AND t.status = 'COMPLETED'")
    BigDecimal getTotalGatewayFeesByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT t FROM Transfer t WHERE t.organization.id = :organizationId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<Transfer> findByOrganizationIdSince(@Param("organizationId") Long organizationId,
            @Param("since") LocalDateTime since);

    Optional<Transfer> findByGatewayTransferId(String gatewayTransferId);

    @Query("SELECT COUNT(t) FROM Transfer t WHERE t.status = 'PENDING' AND t.transferType = 'AUTOMATIC'")
    long countPendingAutomaticTransfers();
}