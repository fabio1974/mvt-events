package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.UnifiedPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para UnifiedPayout
 * Repasses periódicos consolidados para Couriers e ADMs
 */
@Repository
public interface UnifiedPayoutRepository
        extends JpaRepository<UnifiedPayout, Long>, JpaSpecificationExecutor<UnifiedPayout> {

    /**
     * Busca payout por beneficiário e período
     */
    Optional<UnifiedPayout> findByBeneficiaryIdAndPeriod(UUID beneficiaryId, String period);

    /**
     * Verifica se existe payout para beneficiário em um período
     */
    boolean existsByBeneficiaryIdAndPeriod(UUID beneficiaryId, String period);

    /**
     * Busca payouts por beneficiário
     */
    List<UnifiedPayout> findByBeneficiaryIdOrderByPeriodDesc(UUID beneficiaryId);

    /**
     * Busca payouts por tipo de beneficiário
     */
    @Query("SELECT p FROM UnifiedPayout p WHERE p.beneficiaryType = :type ORDER BY p.period DESC")
    List<UnifiedPayout> findByBeneficiaryType(@Param("type") String type);

    /**
     * Busca payouts por status
     */
    List<UnifiedPayout> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Busca payouts pendentes de um período
     */
    @Query("SELECT p FROM UnifiedPayout p " +
            "WHERE p.period = :period AND p.status = 'PENDING' " +
            "ORDER BY p.totalAmount DESC")
    List<UnifiedPayout> findPendingByPeriod(@Param("period") String period);

    /**
     * Calcula total de payouts pagos em um período
     */
    @Query("SELECT SUM(p.totalAmount) FROM UnifiedPayout p " +
            "WHERE p.period = :period AND p.status = 'COMPLETED'")
    Double sumPaidAmountByPeriod(@Param("period") String period);

    /**
     * Busca payouts por período
     */
    @Query("SELECT p FROM UnifiedPayout p WHERE p.period = :period ORDER BY p.createdAt DESC")
    List<UnifiedPayout> findByPeriod(@Param("period") String period);

    /**
     * Busca payouts de couriers em um período
     */
    @Query("SELECT p FROM UnifiedPayout p " +
            "WHERE p.period = :period AND p.beneficiaryType = 'COURIER' " +
            "ORDER BY p.totalAmount DESC")
    List<UnifiedPayout> findCourierPayoutsByPeriod(@Param("period") String period);

    /**
     * Busca payouts de ADMs em um período
     */
    @Query("SELECT p FROM UnifiedPayout p " +
            "WHERE p.period = :period AND p.beneficiaryType = 'ADM' " +
            "ORDER BY p.totalAmount DESC")
    List<UnifiedPayout> findAdmPayoutsByPeriod(@Param("period") String period);
}
