package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.StoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreProfileRepository extends JpaRepository<StoreProfile, Long> {

    @Query("SELECT sp FROM StoreProfile sp JOIN FETCH sp.user WHERE sp.user.id = :userId")
    Optional<StoreProfile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT sp FROM StoreProfile sp JOIN FETCH sp.user WHERE sp.isOpen = true")
    List<StoreProfile> findByIsOpenTrue();

    @Query("SELECT sp FROM StoreProfile sp JOIN FETCH sp.user")
    List<StoreProfile> findAllWithUser();

    boolean existsByUserId(UUID userId);
}
