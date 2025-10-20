package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ClientManagerLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para ClientManagerLink
 * Relacionamento N:M entre Client e ADM
 * Permite multi-tenancy onde um cliente pode estar em múltiplos ADMs
 */
@Repository
public interface ClientManagerLinkRepository
        extends JpaRepository<ClientManagerLink, Long>, JpaSpecificationExecutor<ClientManagerLink> {

    /**
     * Busca todos os ADMs de um cliente
     */
    @Query("SELECT l FROM ClientManagerLink l WHERE l.client.id = :clientId")
    List<ClientManagerLink> findByClientId(@Param("clientId") UUID clientId);

    /**
     * Busca todos os clientes de um ADM
     */
    @Query("SELECT l FROM ClientManagerLink l WHERE l.adm.id = :admId")
    List<ClientManagerLink> findByAdmId(@Param("admId") UUID admId);

    /**
     * Verifica se existe link entre cliente e ADM
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM ClientManagerLink l " +
            "WHERE l.client.id = :clientId AND l.adm.id = :admId")
    boolean existsLinkBetween(@Param("clientId") UUID clientId, @Param("admId") UUID admId);

    /**
     * Busca link específico entre cliente e ADM
     */
    Optional<ClientManagerLink> findByClientIdAndAdmId(UUID clientId, UUID admId);
}
