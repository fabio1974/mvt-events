package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    List<RestaurantTable> findByClientIdOrderByNumber(UUID clientId);

    List<RestaurantTable> findByClientIdAndActiveOrderByNumber(UUID clientId, Boolean active);

    Optional<RestaurantTable> findByClientIdAndNumber(UUID clientId, Integer number);

    boolean existsByClientIdAndNumber(UUID clientId, Integer number);

    Optional<RestaurantTable> findByClientIdAndIsCounterTrue(UUID clientId);
}
