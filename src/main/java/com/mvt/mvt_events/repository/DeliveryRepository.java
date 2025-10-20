package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para Delivery
 * ENTIDADE CORE do Zapi10 - substitui Registration
 * Todas as queries devem filtrar por ADM (tenant)
 */
@Repository
public interface DeliveryRepository
                extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {

        /**
         * Busca deliveries por cliente (filtrar por ADM via Specification)
         */
        List<Delivery> findByClientId(UUID clientId);

        /**
         * Busca deliveries por courier (filtrar por ADM via Specification)
         */
        List<Delivery> findByCourierId(UUID courierId);

        /**
         * Busca deliveries por ADM (TENANT)
         */
        List<Delivery> findByAdmId(UUID admId);

        /**
         * Busca deliveries por status em um ADM específico
         */
        @Query("SELECT d FROM Delivery d WHERE d.adm.id = :admId AND d.status = :status")
        List<Delivery> findByAdmIdAndStatus(@Param("admId") UUID admId, @Param("status") String status);

        /**
         * Conta deliveries por status em um ADM
         */
        @Query("SELECT COUNT(d) FROM Delivery d WHERE d.adm.id = :admId AND d.status = :status")
        Long countByAdmIdAndStatus(@Param("admId") UUID admId, @Param("status") String status);

        /**
         * Busca deliveries de uma parceria específica
         */
        @Query("SELECT d FROM Delivery d WHERE d.partnership.id = :partnershipId")
        List<Delivery> findByPartnershipId(@Param("partnershipId") Long partnershipId);

        /**
         * Busca deliveries pendentes de atribuição em um ADM
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.adm.id = :admId AND d.status = 'PENDING' AND d.courier IS NULL " +
                        "ORDER BY d.createdAt ASC")
        List<Delivery> findPendingAssignmentByAdmId(@Param("admId") UUID admId);

        /**
         * Busca deliveries ativas de um courier
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT') " +
                        "ORDER BY d.scheduledPickupAt ASC")
        List<Delivery> findActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries em um período para um ADM
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.adm.id = :admId " +
                        "AND d.createdAt BETWEEN :startDate AND :endDate " +
                        "ORDER BY d.createdAt DESC")
        List<Delivery> findByAdmIdAndDateRange(
                        @Param("admId") UUID admId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Busca deliveries completadas sem avaliação
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.status = 'COMPLETED' " +
                        "AND NOT EXISTS (SELECT e FROM Evaluation e WHERE e.delivery.id = d.id) " +
                        "AND d.completedAt > :sinceDate")
        List<Delivery> findCompletedWithoutEvaluation(@Param("sinceDate") LocalDateTime sinceDate);
}
