package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
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
         * Busca deliveries por cliente (ordenado por id DESC)
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id = :clientId ORDER BY d.id DESC")
        List<Delivery> findByClientId(@Param("clientId") UUID clientId);

        /**
         * Busca deliveries por courier (ordenado por id DESC)
         */
        @Query("SELECT d FROM Delivery d WHERE d.courier.id = :courierId ORDER BY d.id DESC")
        List<Delivery> findByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries por organização (novo tenant, ordenado por id DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") ORDER BY d.id DESC")
        List<Delivery> findByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca TODAS as deliveries com fetch joins (para ADMIN)
         * Ordenado por id DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query(value = "SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "ORDER BY d.id DESC",
               countQuery = "SELECT COUNT(DISTINCT d) FROM Delivery d")
        org.springframework.data.domain.Page<Delivery> findAllWithJoins(org.springframework.data.domain.Pageable pageable);

        /**
         * Busca deliveries por status em uma organização específica (ordenado por id DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") AND d.status = :status ORDER BY d.id DESC")
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
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client " +
                        "WHERE (:organizationId IS NULL) AND d.status = 'PENDING' AND d.courier IS NULL " +
                        "ORDER BY d.id DESC")
        List<Delivery> findPendingAssignmentByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries ativas de um courier
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status IN ('ACCEPTED', 'IN_TRANSIT') " +
                        "ORDER BY d.id DESC")
        List<Delivery> findActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries concluídas de um courier
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status = 'COMPLETED' " +
                        "ORDER BY d.id DESC")
        List<Delivery> findCompletedByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries concluídas de um courier que NÃO possuem nenhum pagamento PAID
         * Útil para listar entregas pendentes de pagamento
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.courier.id = :courierId " +
                        "AND d.status = 'COMPLETED' " +
                        "AND NOT EXISTS (" +
                        "  SELECT p FROM Payment p JOIN p.deliveries pd " +
                        "  WHERE pd.id = d.id AND p.status = 'PAID'" +
                        ") " +
                        "ORDER BY d.id DESC")
        List<Delivery> findCompletedUnpaidByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries ativas de um organizer (ACCEPTED, IN_TRANSIT)
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.organizer.id = :organizerId " +
                        "AND d.status IN ('ACCEPTED', 'IN_TRANSIT') " +
                        "ORDER BY d.id DESC")
        List<Delivery> findActiveByOrganizerId(@Param("organizerId") UUID organizerId);

        /**
         * Busca deliveries concluídas de um organizer
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.organizer.id = :organizerId " +
                        "AND d.status = 'COMPLETED' " +
                        "ORDER BY d.id DESC")
        List<Delivery> findCompletedByOrganizerId(@Param("organizerId") UUID organizerId);

        /**
         * Busca deliveries em um período para uma organização (ordenado por id DESC)
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
                        ") " +
                        "AND d.createdAt BETWEEN :startDate AND :endDate " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByOrganizationIdAndDateRange(
                        @Param("organizationId") Long organizationId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);

        /**
         * Busca deliveries completadas sem avaliação (ordenado por id DESC)
         */
        @Query("SELECT d FROM Delivery d " +
                        "WHERE d.status = 'COMPLETED' " +
                        "AND NOT EXISTS (SELECT e FROM Evaluation e WHERE e.delivery.id = d.id) " +
                        "AND d.completedAt > :sinceDate " +
                        "ORDER BY d.id DESC")
        List<Delivery> findCompletedWithoutEvaluation(@Param("sinceDate") OffsetDateTime sinceDate);

        /**
         * Busca entregas disponíveis para um motoboy (PENDING na organização)
         * Ordenadas por id DESC para mostrar as mais recentes primeiro
         * Usa ClientContract para determinar organização do cliente
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id = :organizationId AND cc.status = 'ACTIVE'" +
               ") AND d.courier IS NULL ORDER BY d.id DESC")
        List<Delivery> findAvailableForCourier(@Param("organizationId") Long organizationId);

        /**
         * Conta entregas ativas de um motoboy
         */
        @Query("SELECT COUNT(d) FROM Delivery d WHERE d.courier.id = :courierId AND d.status IN ('ACCEPTED', 'IN_TRANSIT')")
        long countActiveDeliveriesByCourier(@Param("courierId") UUID courierId);

        /**
         * Verifica se cliente tem entregas ativas
         */
        @Query("SELECT COUNT(d) > 0 FROM Delivery d WHERE d.client.id = :clientId AND d.status IN ('PENDING', 'ACCEPTED', 'IN_TRANSIT')")
        boolean hasActiveDeliveries(@Param("clientId") UUID clientId);

        /**
         * Busca entregas com pagamento pendente (ordenado por id DESC)
         * Note: Agora usa payments (N:M) ao invés de payment (1:1)
         */
        @Query("SELECT d FROM Delivery d LEFT JOIN d.payments p WHERE d.status = 'COMPLETED' AND (p IS NULL OR p.status = 'PENDING') ORDER BY d.id DESC")
        List<Delivery> findWithPendingPayment();

        /**
         * Busca deliveries com fetch joins por organização do cliente (ordenado por id DESC)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE c.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id = :organizationId " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "ORDER BY d.id DESC")
        List<Delivery> findAllWithJoinsByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca deliveries PENDING e sem courier em organizações PRIMÁRIAS (isPrimary=true)
         * onde o courier possui contratos ativos. Ordenadas por id DESC.
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.courier IS NULL AND d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.organization.id IN :organizationIds " +
               "    AND cc.status = 'ACTIVE' " +
               "    AND cc.isPrimary = true" +
               ") ORDER BY d.id DESC")
        List<Delivery> findPendingInPrimaryOrganizations(@Param("organizationIds") List<Long> organizationIds);

        /**
         * Busca deliveries PENDING (sem courier) e WAITING_PAYMENT (aguardando PIX) de clientes CUSTOMER.
         * PENDING: sem courier atribuído (disponíveis para aceite)
         * WAITING_PAYMENT: aguardando confirmação de pagamento PIX (já podem ter courier)
         * Ordenadas por id DESC.
         */
        @Query("SELECT d FROM Delivery d WHERE " +
               "((d.status = 'PENDING' AND d.courier IS NULL) OR d.status = 'WAITING_PAYMENT') " +
               "AND d.client.role = 'CUSTOMER' ORDER BY d.id DESC")
        List<Delivery> findPendingForCustomerClients();

        /**
         * Busca deliveries PENDING sem courier há mais de X minutos (para expiração automática).
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.courier IS NULL " +
               "AND d.createdAt < :expirationCutoff")
        List<Delivery> findStalePendingDeliveries(@Param("expirationCutoff") OffsetDateTime expirationCutoff);

        /**
         * NÍVEL 1 — Busca deliveries PENDING de clientes vinculados por contrato ativo ao courier.
         * Cadeia: courier → employment_contracts (isActive=true) → organization
         *                 → client_contracts (status=ACTIVE) → client → deliveries PENDING
         * Ordenadas por id DESC.
         */
        @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' AND d.courier IS NULL " +
               "AND d.client.id IN (" +
               "  SELECT cc.client.id FROM ClientContract cc " +
               "  WHERE cc.status = 'ACTIVE' " +
               "  AND cc.organization.id IN (" +
               "    SELECT ec.organization.id FROM EmploymentContract ec " +
               "    WHERE ec.courier.id = :courierId AND ec.isActive = true" +
               "  )" +
               ") ORDER BY d.id DESC")
        List<Delivery> findPendingByContractCourier(@Param("courierId") UUID courierId);

        /**
         * Busca deliveries com fetch joins usando contratos ativos (nova arquitetura)
         * Para COURIERs que acessam organizações via employment_contracts
         * Ordenado por id DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "ORDER BY d.id DESC")
        List<Delivery> findAllWithJoinsByClientContracts(@Param("organizationIds") List<Long> organizationIds);

        /**
         * Busca deliveries com fetch joins usando contratos ativos + status filter
         * Ordenado por id DESC
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE d.client.id IN (" +
                        "  SELECT cc.client.id FROM ClientContract cc " +
                        "  WHERE cc.organization.id IN :organizationIds " +
                        "  AND cc.status = 'ACTIVE'" +
                        ") " +
                        "AND d.status = :status " +
                        "ORDER BY d.id DESC")
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
                        "LEFT JOIN FETCH d.vehicle " +
                        "LEFT JOIN FETCH d.stops " +
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
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE c.id = :clientId " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByClientIdWithJoins(@Param("clientId") UUID clientId);

        /**
         * Busca deliveries por clientId e status com todos os relacionamentos carregados (fetch join)
         * Note: organization removed from User, access via Organization.owner if needed
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE c.id = :clientId AND d.status = :status " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByClientIdAndStatusWithJoins(@Param("clientId") UUID clientId, 
                        @Param("status") Delivery.DeliveryStatus status);

        /**
         * Busca deliveries por clientId e lista de statuses com fetch joins.
         * Usado quando PENDING é expandido para incluir WAITING_PAYMENT (PIX).
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE c.id = :clientId AND d.status IN :statuses " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByClientIdAndStatusesWithJoins(@Param("clientId") UUID clientId,
                        @Param("statuses") List<Delivery.DeliveryStatus> statuses);

        /**
         * Busca deliveries por organizerId com todos os relacionamentos carregados (fetch join)
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer o " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE o.id = :organizerId " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByOrganizerIdWithJoins(@Param("organizerId") UUID organizerId);

        /**
         * Busca deliveries por organizerId e status com todos os relacionamentos carregados (fetch join)
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer o " +
                        "LEFT JOIN FETCH d.vehicle " +
                        "WHERE o.id = :organizerId AND d.status = :status " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByOrganizerIdAndStatusWithJoins(@Param("organizerId") UUID organizerId,
                        @Param("status") Delivery.DeliveryStatus status);

        /**
         * Busca deliveries por organizerId e lista de statuses com fetch joins.
         * Usado quando PENDING é expandido para incluir WAITING_PAYMENT (PIX).
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
                        "LEFT JOIN FETCH d.client c " +
                        "LEFT JOIN FETCH d.courier " +
                        "LEFT JOIN FETCH d.organizer o " +
                        "WHERE o.id = :organizerId AND d.status IN :statuses " +
                        "ORDER BY d.id DESC")
        List<Delivery> findByOrganizerIdAndStatusesWithJoins(@Param("organizerId") UUID organizerId,
                        @Param("statuses") List<Delivery.DeliveryStatus> statuses);

        /**
         * Busca payments (id e status) de múltiplas deliveries
         * Retorna como DTO projection para evitar carregar relacionamento completo
         * CAST do enum para String para evitar ClassCastException
         */
        @Query("SELECT new map(d.id as deliveryId, p.id as paymentId, CAST(p.status AS string) as paymentStatus, CAST(p.paymentMethod AS string) as paymentMethod) " +
               "FROM Payment p " +
               "JOIN p.deliveries d " +
               "WHERE d.id IN :deliveryIds")
        List<java.util.Map<String, Object>> findPaymentsByDeliveryIds(@Param("deliveryIds") List<Long> deliveryIds);

        /**
         * Busca deliveries por IDs com fetch joins para evitar lazy loading
         * Usado para criação de invoices consolidadas
         * Usa query nativa para evitar ConcurrentModificationException com coleções
         */
        @Query(value = "SELECT d.* FROM deliveries d WHERE d.id IN :deliveryIds", nativeQuery = true)
        List<Delivery> findAllByIdNative(@Param("deliveryIds") List<Long> deliveryIds);

        /**
         * Busca todos os clientes (UUIDs únicos) que têm deliveries COMPLETED
         * Usado pelo cronjob de consolidação de pagamentos
         * 
         * @return Lista de client IDs com deliveries completadas
         */
        @Query("SELECT DISTINCT d.client.id FROM Delivery d WHERE d.status = 'COMPLETED' ORDER BY d.client.id")
        List<UUID> findClientsWithCompletedDeliveries();

        /**
         * Busca deliveries COMPLETED que têm payments com status específico (ou NULL)
         * Usado para consolidação: encontrar deliveries que ainda não foram pagas
         * 
         * @param clientId UUID do cliente
         * @param statuses Lista de PaymentStatus a filtrar (ex: NULL, FAILED, EXPIRED)
         * @return Lista de deliveries que necessitam pagamento
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
               "LEFT JOIN FETCH d.payments p " +
               "WHERE d.client.id = :clientId " +
               "AND d.status = 'COMPLETED' " +
               "AND (p IS NULL OR CAST(p.status AS string) IN :statuses) " +
               "ORDER BY d.id DESC")
        List<Delivery> findByClientIdAndPaymentStatusesWithJoins(@Param("clientId") UUID clientId,
                                                                  @Param("statuses") List<String> statuses);

        /**
         * Busca deliveries por courier e status com payments carregados (fetch join)
         * Útil para listar recebimentos do courier com detalhamento de pagamento
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
               "LEFT JOIN FETCH d.payments p " +
               "LEFT JOIN FETCH d.client c " +
               "LEFT JOIN FETCH d.organizer o " +
               "WHERE d.courier.id = :courierId " +
               "AND d.status = :status " +
               "ORDER BY d.id DESC")
        List<Delivery> findByCourierIdAndStatus(@Param("courierId") UUID courierId,
                                                 @Param("status") Delivery.DeliveryStatus status);

        /**
         * Busca deliveries por organizer e status com payments carregados (fetch join)
         * Útil para listar recebimentos do organizer com detalhamento de pagamento
         */
        @Query("SELECT DISTINCT d FROM Delivery d " +
               "LEFT JOIN FETCH d.payments p " +
               "LEFT JOIN FETCH d.client c " +
               "LEFT JOIN FETCH d.courier co " +
               "LEFT JOIN FETCH d.organizer o " +
               "WHERE o.id = :organizerId " +
               "AND d.status = :status " +
               "ORDER BY d.id DESC")
        List<Delivery> findByOrganizerIdAndStatus(@Param("organizerId") UUID organizerId,
                                                   @Param("status") Delivery.DeliveryStatus status);

        // ============================================================================
        // ROUTE TRACKING (PostGIS)
        // ============================================================================

        /**
         * Initialize the route with the first GPS point (called on pickup/IN_TRANSIT)
         */
        @Modifying
        @Query(value = "UPDATE deliveries SET actual_route = ST_MakeLine(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) WHERE id = :deliveryId", nativeQuery = true)
        void initializeRoute(@Param("deliveryId") Long deliveryId, @Param("lat") double lat, @Param("lng") double lng);

        /**
         * Append a GPS point to the existing route
         */
        @Modifying
        @Query(value = "UPDATE deliveries SET actual_route = ST_AddPoint(actual_route, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) WHERE id = :deliveryId AND actual_route IS NOT NULL", nativeQuery = true)
        void appendRoutePoint(@Param("deliveryId") Long deliveryId, @Param("lat") double lat, @Param("lng") double lng);

        /**
         * Get the route as a JSON array of [lng, lat] coordinates
         */
        @Query(value = "SELECT ST_AsGeoJSON(actual_route) FROM deliveries WHERE id = :deliveryId AND actual_route IS NOT NULL", nativeQuery = true)
        String getRouteAsGeoJson(@Param("deliveryId") Long deliveryId);

        @Query(value = "SELECT ST_AsGeoJSON(planned_route) FROM deliveries WHERE id = :deliveryId AND planned_route IS NOT NULL", nativeQuery = true)
        String getPlannedRouteAsGeoJson(@Param("deliveryId") Long deliveryId);

        /**
         * Comprimento da {@code actual_route} em metros (geodésico).
         * Após {@code complete()}, a geometria é a rota real de billing (origem, pós-pickup, trilha,
         * último GPS e fechamento ao destino); o valor alimenta o recálculo de frete.
         */
        @Query(value = "SELECT ST_Length(actual_route::geography) FROM deliveries WHERE id = :deliveryId AND actual_route IS NOT NULL", nativeQuery = true)
        Double getRouteDistanceMeters(@Param("deliveryId") Long deliveryId);

        /**
         * Get the number of points in the route
         */
        @Query(value = "SELECT ST_NPoints(actual_route) FROM deliveries WHERE id = :deliveryId AND actual_route IS NOT NULL", nativeQuery = true)
        Integer getRoutePointCount(@Param("deliveryId") Long deliveryId);

        /**
         * Substitui {@code actual_route} pela rota real de billing antes do recálculo de frete na conclusão.
         */
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "UPDATE deliveries SET actual_route = ST_GeomFromText(:wkt, 4326) WHERE id = :deliveryId", nativeQuery = true)
        void updateActualRouteFromWkt(@Param("deliveryId") Long deliveryId, @Param("wkt") String wkt);
}

