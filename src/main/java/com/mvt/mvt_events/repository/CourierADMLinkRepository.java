package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CourierADMLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para CourierADMLink
 * Relacionamento N:M entre Courier e ADM
 * Permite que um courier trabalhe para múltiplos ADMs
 */
@Repository
public interface CourierADMLinkRepository
                extends JpaRepository<CourierADMLink, Long>, JpaSpecificationExecutor<CourierADMLink> {

        /**
         * Busca ADM primário ativo do courier
         */
        @Query("SELECT l FROM CourierADMLink l " +
                        "WHERE l.courier.id = :courierId " +
                        "AND l.isPrimary = true AND l.isActive = true")
        Optional<CourierADMLink> findPrimaryActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca todos os ADMs ativos do courier
         */
        @Query("SELECT l FROM CourierADMLink l " +
                        "WHERE l.courier.id = :courierId AND l.isActive = true")
        List<CourierADMLink> findActiveByCourierId(@Param("courierId") UUID courierId);

        /**
         * Busca todos os couriers de um ADM
         */
        @Query("SELECT l FROM CourierADMLink l " +
                        "WHERE l.adm.id = :admId AND l.isActive = true")
        List<CourierADMLink> findActiveByAdmId(@Param("admId") UUID admId);

        /**
         * Verifica se existe link ativo entre courier e ADM
         */
        @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM CourierADMLink l " +
                        "WHERE l.courier.id = :courierId " +
                        "AND l.adm.id = :admId AND l.isActive = true")
        boolean existsActiveLinkBetween(
                        @Param("courierId") UUID courierId,
                        @Param("admId") UUID admId);

        /**
         * Busca link específico entre courier e ADM
         */
        Optional<CourierADMLink> findByCourierIdAndAdmId(UUID courierId, UUID admId);
}
