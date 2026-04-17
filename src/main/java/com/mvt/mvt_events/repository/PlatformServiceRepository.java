package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.PlatformService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformServiceRepository extends JpaRepository<PlatformService, Long> {

    Optional<PlatformService> findByCode(String code);
}
