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
    @Query("UPDATE DeliveryStop s SET s.status = 'COMPLETED', s.completedAt = CURRENT_TIMESTAMP " +
           "WHERE s.id = :stopId AND s.delivery.id = :deliveryId")
    int completeStop(@Param("deliveryId") Long deliveryId, @Param("stopId") Long stopId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeliveryStop s SET s.status = 'SKIPPED', s.completedAt = CURRENT_TIMESTAMP " +
           "WHERE s.id = :stopId AND s.delivery.id = :deliveryId AND s.status = 'PENDING'")
    int skipStop(@Param("deliveryId") Long deliveryId, @Param("stopId") Long stopId);
}
