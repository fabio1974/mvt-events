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

    Optional<FoodOrder> findByDeliveryId(Long deliveryId);

    /**
     * Busca pedidos DELIVERING do mesmo restaurante nos últimos N minutos,
     * cuja Delivery ainda está PENDING ou ACCEPTED (courier não saiu).
     */
    @Query("SELECT o FROM FoodOrder o " +
           "JOIN FETCH o.delivery d " +
           "WHERE o.client.id = :clientId " +
           "AND o.status = 'DELIVERING' " +
           "AND d.status IN ('PENDING', 'ACCEPTED') " +
           "AND o.readyAt >= :since " +
           "ORDER BY o.readyAt ASC")
    List<FoodOrder> findRecentDeliveringByClient(
            @Param("clientId") UUID clientId,
            @Param("since") java.time.OffsetDateTime since);
}
