package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.StoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreProfileRepository extends JpaRepository<StoreProfile, Long> {

    Optional<StoreProfile> findByUserId(UUID userId);

    List<StoreProfile> findByIsOpenTrue();

    boolean existsByUserId(UUID userId);
}
