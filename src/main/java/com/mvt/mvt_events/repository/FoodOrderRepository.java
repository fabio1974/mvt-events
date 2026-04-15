package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    @Query("SELECT o FROM FoodOrder o LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.client LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<FoodOrder> findByIdWithItems(@Param("id") Long id);

    List<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<FoodOrder> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    org.springframework.data.domain.Page<FoodOrder> findByClientId(UUID clientId, org.springframework.data.domain.Pageable pageable);

    List<FoodOrder> findByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, FoodOrder.OrderStatus status);

    List<FoodOrder> findByClientIdAndStatusInOrderByCreatedAtDesc(UUID clientId, List<FoodOrder.OrderStatus> statuses);

    @Query("SELECT o FROM FoodOrder o WHERE o.id = (SELECT d.order.id FROM Delivery d WHERE d.id = :deliveryId)")
    Optional<FoodOrder> findByDeliveryId(@Param("deliveryId") Long deliveryId);

    /** Pedidos ativos (não finalizados) por mesa de um estabelecimento */
    @Query("SELECT o FROM FoodOrder o WHERE o.client.id = :clientId " +
           "AND o.table IS NOT NULL " +
           "AND o.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY o.createdAt DESC")
    List<FoodOrder> findActiveTableOrders(@Param("clientId") UUID clientId);

    /** Pedidos ativos do garçom em um estabelecimento */
    @Query("SELECT o FROM FoodOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
           "WHERE o.waiter.id = :waiterId AND o.client.id = :clientId " +
           "AND o.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY o.createdAt DESC")
    List<FoodOrder> findActiveByWaiterAndClient(@Param("waiterId") UUID waiterId, @Param("clientId") UUID clientId);

    /** Pedidos de uma mesa específica (ativos) */
    @Query("SELECT o FROM FoodOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
           "WHERE o.table.id = :tableId " +
           "AND o.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY o.createdAt DESC")
    List<FoodOrder> findActiveByTable(@Param("tableId") Long tableId);

    /** Pedidos ativos de um client (para mapa de status das mesas) */
    @Query("SELECT o FROM FoodOrder o LEFT JOIN FETCH o.table " +
           "WHERE o.client.id = :clientId " +
           "AND o.table IS NOT NULL " +
           "AND o.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "ORDER BY o.createdAt DESC")
    List<FoodOrder> findActiveByClientId(@Param("clientId") UUID clientId);

    /** Todos os pedidos de uma mesa (incluindo finalizados) */
    @Query("SELECT o FROM FoodOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product " +
           "WHERE o.table.id = :tableId " +
           "ORDER BY o.createdAt DESC")
    List<FoodOrder> findByTableId(@Param("tableId") Long tableId);

    /**
     * Busca pedidos READY ou DELIVERING do mesmo restaurante nos últimos N minutos,
     * cuja Delivery ainda está PENDING ou ACCEPTED (courier não saiu).
     */
    @Query("SELECT o FROM FoodOrder o " +
           "JOIN Delivery d ON d.order = o " +
           "WHERE o.client.id = :clientId " +
           "AND o.status IN ('READY', 'DELIVERING') " +
           "AND d.status IN ('PENDING', 'ACCEPTED') " +
           "AND o.readyAt >= :since " +
           "ORDER BY o.readyAt ASC")
    List<FoodOrder> findRecentDeliveringByClient(
            @Param("clientId") UUID clientId,
            @Param("since") java.time.OffsetDateTime since);
}
