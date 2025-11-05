package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.EmploymentContract;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmploymentContractRepository extends JpaRepository<EmploymentContract, Long> {

    /**
     * Find contract by courier and organization
     */
    Optional<EmploymentContract> findByCourierAndOrganization(User courier, Organization organization);

    /**
     * Find all contracts for a courier
     */
    List<EmploymentContract> findByCourier(User courier);

    /**
     * Find all contracts for an organization
     */
    List<EmploymentContract> findByOrganization(Organization organization);

    /**
     * Find active contracts for a courier
     */
    @Query("SELECT ec FROM EmploymentContract ec WHERE ec.courier = :courier AND ec.isActive = true")
    List<EmploymentContract> findActiveByCourier(@Param("courier") User courier);

    /**
     * Find active contracts for an organization
     */
    @Query("SELECT ec FROM EmploymentContract ec WHERE ec.organization = :organization AND ec.isActive = true")
    List<EmploymentContract> findActiveByOrganization(@Param("organization") Organization organization);

    /**
     * Check if courier has active contract with organization
     */
    @Query("SELECT COUNT(ec) > 0 FROM EmploymentContract ec WHERE ec.courier.id = :courierId AND ec.organization.id = :organizationId AND ec.isActive = true")
    boolean hasActiveContract(@Param("courierId") UUID courierId, @Param("organizationId") Long organizationId);

    /**
     * Delete all contracts for an organization
     */
    void deleteByOrganization(Organization organization);

    /**
     * Delete all contracts for a courier
     */
    void deleteByCourier(User courier);

    /**
     * Count active contracts for organization
     */
    @Query("SELECT COUNT(ec) FROM EmploymentContract ec WHERE ec.organization = :organization AND ec.isActive = true")
    long countActiveByOrganization(@Param("organization") Organization organization);

    /**
     * Busca todos os contratos de trabalho de uma organização
     */
    @Query("SELECT ec FROM EmploymentContract ec WHERE ec.organization.id = :organizationId")
    List<EmploymentContract> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Busca contratos ativos de uma organização
     */
    @Query("SELECT ec FROM EmploymentContract ec WHERE ec.organization.id = :organizationId AND ec.isActive = true")
    List<EmploymentContract> findActiveByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Busca contratos de um courier específico
     */
    @Query("SELECT ec FROM EmploymentContract ec WHERE ec.courier.id = :courierId")
    List<EmploymentContract> findByCourierId(@Param("courierId") UUID courierId);

    /**
     * Verifica se existe contrato ativo entre courier e organização
     */
    @Query("SELECT COUNT(ec) > 0 FROM EmploymentContract ec WHERE ec.courier.id = :courierId AND ec.organization.id = :organizationId AND ec.isActive = true")
    boolean existsActiveByCourierAndOrganization(@Param("courierId") UUID courierId,
            @Param("organizationId") Long organizationId);

    /**
     * Deleta todos os contratos de uma organização (query customizada)
     */
    @Modifying
    @Query("DELETE FROM EmploymentContract ec WHERE ec.organization = :organization")
    void deleteAllByOrganization(@Param("organization") com.mvt.mvt_events.jpa.Organization organization);

    /**
     * Busca dados dos contratos de uma organização SEM carregar os objetos User
     * Retorna: [courier_id, linked_at, is_active]
     */
    @Query("SELECT ec.courier.id, ec.linkedAt, ec.isActive FROM EmploymentContract ec WHERE ec.organization.id = :organizationId")
    List<Object[]> findContractDataByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Busca contratos de um COURIER com dados da organização SEM carregar objetos
     * completos
     * Retorna: [organization_id, organization_name, linked_at, is_active]
     */
    @Query("SELECT ec.organization.id, ec.organization.name, ec.linkedAt, ec.isActive FROM EmploymentContract ec WHERE ec.courier.id = :courierId")
    List<Object[]> findContractDataByCourierId(@Param("courierId") java.util.UUID courierId);
}
