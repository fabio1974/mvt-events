package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.SiteConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para SiteConfiguration
 */
@Repository
public interface SiteConfigurationRepository extends JpaRepository<SiteConfiguration, Long> {

    /**
     * Busca a configuração ativa (apenas uma deve existir)
     */
    @Query("SELECT sc FROM SiteConfiguration sc WHERE sc.isActive = true")
    Optional<SiteConfiguration> findActiveConfiguration();

    /**
     * Verifica se existe alguma configuração ativa
     */
    @Query("SELECT COUNT(sc) > 0 FROM SiteConfiguration sc WHERE sc.isActive = true")
    boolean existsActiveConfiguration();

    /**
     * Busca configurações por status (ativo/inativo) com paginação
     */
    Page<SiteConfiguration> findByIsActive(Boolean isActive, Pageable pageable);
}
