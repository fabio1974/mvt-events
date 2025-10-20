package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.MunicipalPartnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para MunicipalPartnership
 * Parcerias institucionais com prefeituras para entregas
 */
@Repository
public interface MunicipalPartnershipRepository
        extends JpaRepository<MunicipalPartnership, Long>, JpaSpecificationExecutor<MunicipalPartnership> {

    /**
     * Busca parceria por CNPJ
     */
    Optional<MunicipalPartnership> findByCnpj(String cnpj);

    /**
     * Verifica se existe parceria ativa para uma cidade
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM MunicipalPartnership p " +
            "WHERE p.city = :city AND p.state = :state AND p.status = 'ACTIVE'")
    boolean existsActiveByCityAndState(@Param("city") String city, @Param("state") String state);

    /**
     * Verifica se CNPJ já existe
     */
    boolean existsByCnpj(String cnpj);
}
