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
 * Filtragem por tenant através de ClientContract (organization removida de User)
 */
@Repository
public interface DeliveryRepository
                extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {

        /**
         * Busca deliveries por cliente (ordenado por updatedAt DESC)
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id = :clientId ORDER BY d.updatedAt DESC")
        List<Delivery> findByClientId(@Param("clientId") UUID clientId);

        /**
         * Busca deliveries por courier (ordenado por updatedAt DESC)
         */
        @Query("SELECT d FROM Delivery d WHERE d.courier.id = :courierId ORDER BY d.updatedAt DESC")
        List<Delivery> findByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries por organização (novo tenant, ordenado por updatedAt DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") ORDER BY d.updatedAt DESC")
        List<Delivery> findByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca TODAS as deliveries com fetch joins (para ADMIN)
         * Ordenado por updatedAt DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query(value = "SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "ORDER BY d.updatedAt DESC",
               countQuery = "SELECT COUNT(DISTINCT d) FROM Delivery d")
        org.springframework.data.domain.Page<Delivery> findAllWithJoins(org.springframework.data.domain.Pageable pageable);

        /**
         * Busca deliveries por status em uma organização específica (ordenado por updatedAt DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") AND d.status = :status ORDER BY d.updatedAt DESC")
        List<Delivery> findByOrganizationIdAndStatus(@Param("organizationId") Long organizationId,
                        @Param("status") String status);

        /**
         * Conta deliveries por status em uma organização
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT COUNT(d) FROM Delivery d WHERE d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") AND d.status = :status")
        Long countByOrganizationIdAndStatus(@Param("organizationId") Long organizationId,
                        @Param("status") String status);

        // REMOVIDO: findByPartnershipId() - Municipal Partnerships foi removido do sistema

        /**
         * Busca deliveries pendentes de atribuição
         * Se organizationId for null, retorna todas as deliveries pendentes (para ADMIN)
         * Se organizationId for informado, filtrar seria incorreto pois organization não tem relação com deliveries
         * Ordenadas por updatedAt DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client " +
                        "WHERE (:organizationId IS NULL) AND d.status = 'PENDING' AND d.courier IS NULL " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findPendingAssignmentByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries ativas de um courier
         * Ordenadas por updatedAt DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT') " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries em um período para uma organização (ordenado por updatedAt DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
                        ") " +
                        "AND d.createdAt BETWEEN :startDate AND :endDate " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findByOrganizationIdAndDateRange(
                        @Param("organizationId") Long organizationId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Busca deliveries completadas sem avaliação (ordenado por updatedAt DESC)
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.status = 'COMPLETED' " +
                        "AND NOT EXISTS (SELECT e FROM Evaluation e WHERE e.delivery.id = d.id) " +
                        "AND d.completedAt > :sinceDate " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findCompletedWithoutEvaluation(@Param("sinceDate") LocalDateTime sinceDate);

        /**
         * Busca entregas disponíveis para um motoboy (PENDING na organização)
         * Ordenadas por updatedAt DESC para mostrar as mais recentes primeiro
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") AND d.courier IS NULL ORDER BY d.updatedAt DESC")
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
         * Busca entregas com pagamento pendente (ordenado por updatedAt DESC)
         */
        @Query("SELECT d FROM Delivery d LEFT JOIN d.payment p WHERE d.status = 'COMPLETED' AND (p IS NULL OR p.status = 'PENDING') ORDER BY d.updatedAt DESC")
        List<Delivery> findWithPendingPayment();

        /**
         * Busca deliveries com fetch joins por organização do cliente (ordenado por updatedAt DESC)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "WHERE c.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id = :organizationId " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findAllWithJoinsByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries com fetch joins usando contratos ativos (nova arquitetura)
         * Para COURIERs que acessam organizações via employment_contracts
         * Ordenado por updatedAt DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findAllWithJoinsByClientContracts(@Param("organizationIds") List<Long> organizationIds);

        /**
         * Busca deliveries com fetch joins usando contratos ativos + status filter
         * Ordenado por updatedAt DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "AND d.status = :status " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findAllWithJoinsByClientContractsAndStatus(@Param("organizationIds") List<Long> organizationIds,
                        @Param("status") Delivery.DeliveryStatus status);

        /**
         * Busca delivery por ID com todos os relacionamentos carregados (fetch join)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "WHERE d.id = :id")
        java.util.Optional<Delivery> findByIdWithJoins(@Param("id") Long id);

        /**
         * Busca deliveries por clientId com todos os relacionamentos carregados (fetch join)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "WHERE c.id = :clientId " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findByClientIdWithJoins(@Param("clientId") UUID clientId);

        /**
         * Busca deliveries por clientId e status com todos os relacionamentos carregados (fetch join)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "WHERE c.id = :clientId AND d.status = :status " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findByClientIdAndStatusWithJoins(@Param("clientId") UUID clientId, 
                        @Param("status") Delivery.DeliveryStatus status);

        /**
         * Busca deliveries por organizerId com todos os relacionamentos carregados (fetch join)
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer o " +
                        "WHERE o.id = :organizerId " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findByOrganizerIdWithJoins(@Param("organizerId") UUID organizerId);

        /**
         * Busca deliveries por organizerId e status com todos os relacionamentos carregados (fetch join)
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer o " +
                        "WHERE o.id = :organizerId AND d.status = :status " +
                        "ORDER BY d.updatedAt DESC")
        List<Delivery> findByOrganizerIdAndStatusWithJoins(@Param("organizerId") UUID organizerId,
                        @Param("status") Delivery.DeliveryStatus status);
}

