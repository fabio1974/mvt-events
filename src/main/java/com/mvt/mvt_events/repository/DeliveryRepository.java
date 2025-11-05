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
 * Filtragem por tenant agora via client.organization.id
 */
@Repository
public interface DeliveryRepository
                extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {

        /**
         * Busca deliveries por cliente
         */
        List<Delivery> findByClientId(UUID clientId);

        /**
         * Busca deliveries por courier
         */
        List<Delivery> findByCourierId(UUID courierId);

        /**
         * Busca deliveries por organização (novo tenant)
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.organization.id = :organizationId")
        List<Delivery> findByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries por status em uma organização específica
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.organization.id = :organizationId AND d.status = :status")
        List<Delivery> findByOrganizationIdAndStatus(@Param("organizationId") Long organizationId,
                        @Param("status") String status);

        /**
         * Conta deliveries por status em uma organização
         */
        @Query("SELECT COUNT(d) FROM Delivery d WHERE d.client.organization.id = :organizationId AND d.status = :status")
        Long countByOrganizationIdAndStatus(@Param("organizationId") Long organizationId,
                        @Param("status") String status);

        /**
         * Busca deliveries de uma parceria específica
         */
        @Query("SELECT d FROM Delivery d WHERE d.partnership.id = :partnershipId")
        List<Delivery> findByPartnershipId(@Param("partnershipId") Long partnershipId);

        /**
         * Busca deliveries pendentes de atribuição em uma organização
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.client.organization.id = :organizationId AND d.status = 'PENDING' AND d.courier IS NULL "
                        +
                        "ORDER BY d.createdAt ASC")
        List<Delivery> findPendingAssignmentByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries ativas de um courier
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT') " +
                        "ORDER BY d.scheduledPickupAt ASC")
        List<Delivery> findActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries em um período para uma organização
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.client.organization.id = :organizationId " +
                        "AND d.createdAt BETWEEN :startDate AND :endDate " +
                        "ORDER BY d.createdAt DESC")
        List<Delivery> findByOrganizationIdAndDateRange(
                        @Param("organizationId") Long organizationId,
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

        /**
         * Busca entregas disponíveis para um motoboy (PENDING na organização)
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.client.organization.id = :organizationId AND d.courier IS NULL ORDER BY d.scheduledPickupAt ASC")
        List<Delivery> findAvailableForCourier(@Param("organizationId") Long organizationId);

        /**
         * Conta entregas ativas de um motoboy
         */
        @Query("SELECT COUNT(d) FROM Delivery d WHERE d.courier.id = :courierId AND d.status IN ('ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')")
        long countActiveDeliveriesByCourier(@Param("courierId") UUID courierId);

        /**
         * Verifica se cliente tem entregas ativas
         */
        @Query("SELECT COUNT(d) > 0 FROM Delivery d WHERE d.client.id = :clientId AND d.status IN ('PENDING', 'ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')")
        boolean hasActiveDeliveries(@Param("clientId") UUID clientId);

        /**
         * Busca entregas com pagamento pendente
         */
        @Query("SELECT d FROM Delivery d LEFT JOIN d.payment p WHERE d.status = 'COMPLETED' AND (p IS NULL OR p.status = 'PENDING')")
        List<Delivery> findWithPendingPayment();

        /**
         * Busca deliveries com fetch joins por organização do cliente
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH c.organization " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.partnership " +
                        "WHERE c.organization.id = :organizationId " +
                        "ORDER BY d.createdAt DESC")
        List<Delivery> findAllWithJoinsByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries com fetch joins usando contratos ativos (nova arquitetura)
         * Para COURIERs que acessam organizações via employment_contracts
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH c.organization " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.partnership " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "ORDER BY d.createdAt DESC")
        List<Delivery> findAllWithJoinsByClientContracts(@Param("organizationIds") List<Long> organizationIds);

        /**
         * Busca deliveries com fetch joins usando contratos ativos + status filter
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH c.organization " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.partnership " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "AND d.status = :status " +
                        "ORDER BY d.createdAt DESC")
        List<Delivery> findAllWithJoinsByClientContractsAndStatus(@Param("organizationIds") List<Long> organizationIds,
                        @Param("status") Delivery.DeliveryStatus status);
}
