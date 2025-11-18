package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ADMProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para ADMProfile
 * Perfil de gerentes locais (ADM) - TENANT da aplicação
 */
@Repository
public interface ADMProfileRepository
        extends JpaRepository<ADMProfile, Long>, JpaSpecificationExecutor<ADMProfile> {

    /**
     * Busca perfil de ADM por User ID
     */
    Optional<ADMProfile> findByUserId(UUID userId);

    /**
     * Verifica se existe perfil para o usuário
     */
    boolean existsByUserId(UUID userId);

    /**
     * Busca ADMs por região
     */
    List<ADMProfile> findByRegion(String region);

    /**
     * Busca ADMs ativos por região
     */
    @Query("SELECT a FROM ADMProfile a WHERE a.region = :region AND a.status = 'ACTIVE'")
    List<ADMProfile> findActiveByRegion(@Param("region") String region);

    // REMOVIDO: findByPartnershipId() - Municipal Partnerships foi removido do sistema
}
