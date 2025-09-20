package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.EventFinancials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventFinancialsRepository extends JpaRepository<EventFinancials, Long> {

    Optional<EventFinancials> findByEventId(Long eventId);

    @Query("SELECT ef FROM EventFinancials ef WHERE ef.event.organization.id = :organizationId")
    List<EventFinancials> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT ef FROM EventFinancials ef WHERE ef.pendingTransferAmount > 0")
    List<EventFinancials> findWithPendingTransfers();

    @Query("SELECT ef FROM EventFinancials ef WHERE ef.nextTransferDate <= :date AND ef.pendingTransferAmount > 0")
    List<EventFinancials> findDueForTransfer(@Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(ef.totalRevenue), 0) FROM EventFinancials ef WHERE ef.event.organization.id = :organizationId")
    BigDecimal getTotalRevenueByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT COALESCE(SUM(ef.platformFees), 0) FROM EventFinancials ef WHERE ef.event.organization.id = :organizationId")
    BigDecimal getTotalPlatformFeesByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT COALESCE(SUM(ef.transferredAmount), 0) FROM EventFinancials ef WHERE ef.event.organization.id = :organizationId")
    BigDecimal getTotalTransferredByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT ef FROM EventFinancials ef WHERE ef.event.organization.id = :organizationId AND ef.updatedAt >= :since")
    List<EventFinancials> findByOrganizationIdSince(@Param("organizationId") Long organizationId,
            @Param("since") LocalDateTime since);
}