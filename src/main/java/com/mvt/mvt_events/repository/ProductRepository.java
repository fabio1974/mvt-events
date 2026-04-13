package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    java.util.Optional<Product> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.client.id = :clientId ORDER BY p.displayOrder")
    List<Product> findByClientIdOrderByDisplayOrderAsc(@Param("clientId") UUID clientId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.client.id = :clientId AND p.available = true ORDER BY p.displayOrder")
    List<Product> findByClientIdAndAvailableTrueOrderByDisplayOrderAsc(@Param("clientId") UUID clientId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId ORDER BY p.displayOrder")
    List<Product> findByCategoryIdOrderByDisplayOrderAsc(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId AND p.available = true ORDER BY p.displayOrder")
    List<Product> findByCategoryIdAndAvailableTrueOrderByDisplayOrderAsc(@Param("categoryId") Long categoryId);
}
