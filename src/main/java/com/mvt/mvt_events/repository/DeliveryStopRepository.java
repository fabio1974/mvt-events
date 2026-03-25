package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.DeliveryStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryStopRepository extends JpaRepository<DeliveryStop, Long> {

    List<DeliveryStop> findByDeliveryIdOrderByStopOrderAsc(Long deliveryId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeliveryStop s SET s.status = 'COMPLETED', s.completedAt = CURRENT_TIMESTAMP, s.completionOrder = :completionOrder " +
           "WHERE s.id = :stopId AND s.delivery.id = :deliveryId AND s.completionOrder IS NULL")
    int completeStop(@Param("deliveryId") Long deliveryId, @Param("stopId") Long stopId, @Param("completionOrder") int completionOrder);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeliveryStop s SET s.status = 'SKIPPED', s.completedAt = CURRENT_TIMESTAMP, s.completionOrder = 0 " +
           "WHERE s.id = :stopId AND s.delivery.id = :deliveryId AND s.status = 'PENDING'")
    int skipStop(@Param("deliveryId") Long deliveryId, @Param("stopId") Long stopId);

    /**
     * Returns the highest completionOrder among COMPLETED stops of a delivery.
     * Returns 0 if none exist yet.
     */
    @Query("SELECT COALESCE(MAX(s.completionOrder), 0) FROM DeliveryStop s " +
           "WHERE s.delivery.id = :deliveryId AND s.status = 'COMPLETED'")
    int maxCompletionOrder(@Param("deliveryId") Long deliveryId);

    /**
     * Updates completionOrder for a PENDING stop (planned visit sequence).
     * Only updates if the stop is still PENDING (completionOrder not yet locked).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeliveryStop s SET s.completionOrder = :order " +
           "WHERE s.id = :stopId AND s.status = 'PENDING'")
    void updatePlannedOrder(@Param("stopId") Long stopId, @Param("order") int order);
}
