package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.ClientContract;
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
public interface ClientContractRepository extends JpaRepository<ClientContract, Long> {

        /**
         * Find contract by client and organization
         */
        Optional<ClientContract> findByClientAndOrganization(User client, Organization organization);

        /**
         * Find all contracts for a client
         */
        List<ClientContract> findByClient(User client);

        /**
         * Find all contracts for an organization
         */
        List<ClientContract> findByOrganization(Organization organization);

        /**
         * Find active contracts for a client
         */
        @Query("SELECT c FROM ClientContract c WHERE c.client = :client AND c.status = 'ACTIVE'")
        List<ClientContract> findActiveByClient(@Param("client") User client);

        /**
         * Find active contracts for an organization
         */
        @Query("SELECT c FROM ClientContract c WHERE c.organization = :organization AND c.status = 'ACTIVE'")
        List<ClientContract> findActiveByOrganization(@Param("organization") Organization organization);

        /**
         * Find primary contract for a client
         */
        @Query("SELECT c FROM ClientContract c WHERE c.client = :client AND c.isPrimary = true")
        Optional<ClientContract> findPrimaryByClient(@Param("client") User client);

        /**
         * Find primary contract for client in organization
         */
        @Query("SELECT c FROM ClientContract c WHERE c.client = :client AND c.organization = :organization AND c.isPrimary = true")
        Optional<ClientContract> findPrimaryByClientAndOrganization(@Param("client") User client,
                        @Param("organization") Organization organization);

        /**
         * Check if client has active contract with organization
         */
        @Query("SELECT COUNT(c) > 0 FROM ClientContract c WHERE c.client.id = :clientId AND c.organization.id = :organizationId AND c.status = 'ACTIVE'")
        boolean hasActiveContract(@Param("clientId") UUID clientId, @Param("organizationId") Long organizationId);

        /**
         * Delete all contracts for an organization
         */
        void deleteByOrganization(Organization organization);

        /**
         * Delete all contracts for a client
         */
        void deleteByClient(User client);

        /**
         * Count active contracts for organization
         */
        @Query("SELECT COUNT(c) FROM ClientContract c WHERE c.organization = :organization AND c.status = 'ACTIVE'")
        long countActiveByOrganization(@Param("organization") Organization organization);

        /**
         * Busca todos os contratos de serviço de uma organização
         */
        @Query("SELECT c FROM ClientContract c WHERE c.organization.id = :organizationId")
        List<ClientContract> findByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca contratos ativos de uma organização
         */
        @Query("SELECT c FROM ClientContract c WHERE c.organization.id = :organizationId AND c.status = 'ACTIVE'")
        List<ClientContract> findActiveByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca contratos de um cliente específico
         */
        @Query("SELECT c FROM ClientContract c WHERE c.client.id = :clientId")
        List<ClientContract> findByClientId(@Param("clientId") UUID clientId);

        /**
         * Busca contratos ativos de um cliente específico com organização carregada
         */
        @Query("SELECT c FROM ClientContract c JOIN FETCH c.organization o LEFT JOIN FETCH o.owner WHERE c.client.id = :clientId AND c.status = 'ACTIVE'")
        List<ClientContract> findActiveByClientId(@Param("clientId") UUID clientId);

        /**
         * Busca contrato primário de um cliente com uma organização
         */
        @Query("SELECT c FROM ClientContract c WHERE c.client.id = :clientId AND c.organization.id = :organizationId AND c.isPrimary = true AND c.status = 'ACTIVE'")
        ClientContract findPrimaryByClientAndOrganization(@Param("clientId") UUID clientId,
                        @Param("organizationId") Long organizationId);

        /**
         * Verifica se existe contrato ativo entre cliente e organização
         */
        @Query("SELECT COUNT(c) > 0 FROM ClientContract c WHERE c.client.id = :clientId AND c.organization.id = :organizationId AND c.status = 'ACTIVE'")
        boolean existsActiveByClientAndOrganization(@Param("clientId") UUID clientId,
                        @Param("organizationId") Long organizationId);

        /**
         * Deleta todos os contratos de uma organização (query customizada)
         */
        @Modifying
        @Query("DELETE FROM ClientContract c WHERE c.organization = :organization")
        void deleteAllByOrganization(@Param("organization") com.mvt.mvt_events.jpa.Organization organization);

        /**
         * Busca dados dos contratos de uma organização SEM carregar os objetos User
         * Retorna: [client_id, is_primary, status, contract_date,
         * start_date, end_date]
         */
        @Query("SELECT c.client.id, c.isPrimary, c.status, c.contractDate, c.startDate, c.endDate FROM ClientContract c WHERE c.organization.id = :organizationId")
        List<Object[]> findContractDataByOrganizationId(@Param("organizationId") Long organizationId);

        /**
         * Busca contratos de um CLIENT com dados da organização SEM carregar objetos
         * completos
         * Retorna: [organization_id, organization_name, is_primary,
         * status, contract_date, start_date, end_date]
         */
        @Query("SELECT c.organization.id, c.organization.name, c.isPrimary, c.status, c.contractDate, c.startDate, c.endDate FROM ClientContract c WHERE c.client.id = :clientId")
        List<Object[]> findContractDataByClientId(@Param("clientId") java.util.UUID clientId);
}
