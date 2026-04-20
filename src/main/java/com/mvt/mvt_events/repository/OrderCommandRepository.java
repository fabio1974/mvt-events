package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.OrderCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderCommandRepository extends JpaRepository<OrderCommand, Long> {

    List<OrderCommand> findByOrderIdOrderByDisplayNumberAsc(Long orderId);

    @Query("SELECT COALESCE(MAX(c.displayNumber), 0) FROM OrderCommand c WHERE c.order.id = :orderId")
    Integer findMaxDisplayNumberByOrderId(@Param("orderId") Long orderId);

    Optional<OrderCommand> findByIdAndOrderId(Long id, Long orderId);
}
