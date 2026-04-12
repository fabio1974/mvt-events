package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByClientIdOrderByDisplayOrderAsc(UUID clientId);

    List<Product> findByClientIdAndAvailableTrueOrderByDisplayOrderAsc(UUID clientId);

    List<Product> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);

    List<Product> findByCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(Long categoryId);
}
