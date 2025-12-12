package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

       // Métodos de busca única e validação - mantidos
       // Note: organization removed from User entity, now access via Organization.owner
       // Note: city removed from User entity, now access via Address.city
       Optional<User> findByUsername(String username);

       // Método para autenticação (carrega todos os relacionamentos necessários para
       // JWT)
       @EntityGraph(attributePaths = {"addresses", "addresses.city", "employmentContracts", "clientContracts"})
       @Query("SELECT u FROM User u WHERE u.username = :username")
       Optional<User> findByUsernameForAuth(@Param("username") String username);

       // Método para autenticação por CPF (carrega todos os relacionamentos necessários para JWT)
       @EntityGraph(attributePaths = {"addresses", "addresses.city", "employmentContracts", "clientContracts"})
       @Query("SELECT u FROM User u WHERE u.documentNumber = :documentNumber")
       Optional<User> findByDocumentNumberForAuth(@Param("documentNumber") String documentNumber);

       // Método simples para reset password (sem carregar relacionamentos)
       @Query("SELECT u FROM User u WHERE u.username = :username")
       Optional<User> findByUsernameSimple(@Param("username") String username);

       // Método para tenant filter (busca apenas role sem carregar relacionamentos)
       @Query("SELECT u FROM User u WHERE u.username = :username")
       Optional<User> findByUsernameWithoutRelations(@Param("username") String username);

       // Update direto da senha (evita problemas com collections)
       @Modifying
       @Transactional
       @Query("UPDATE User u SET u.password = :password WHERE u.username = :username")
       int updatePasswordByUsername(@Param("username") String username, @Param("password") String password);

       boolean existsByUsername(String username);

       boolean existsByDocumentNumber(String documentNumber);

       @Query("SELECT u FROM User u WHERE u.documentNumber = :documentNumber")
       Optional<User> findByDocumentNumber(@Param("documentNumber") String documentNumber);

       // Método otimizado para buscar usuário sem carregar contratos (evita concurrent modification)
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.addresses a " +
           "LEFT JOIN FETCH a.city " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithAddresses(@Param("id") UUID id);

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.employmentContracts ec " +
           "LEFT JOIN FETCH ec.organization " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithEmploymentContracts(@Param("id") UUID id);

    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.clientContracts cc " +
           "LEFT JOIN FETCH cc.organization " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithClientContracts(@Param("id") UUID id);       // Métodos de busca textual - mantidos (não cobertos por Specifications simples)
       @Query("SELECT u FROM User u WHERE " +
                     "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                     "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
       List<User> searchUsers(@Param("searchTerm") String searchTerm);

       @Query("SELECT u FROM User u WHERE u.role = 'USER' AND " +
                     "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
       List<User> searchAthletes(@Param("searchTerm") String searchTerm);

       /**
        * Busca usuários ADM/ORGANIZER que são donos de organizações específicas
        */
       @Query("SELECT u FROM User u WHERE u.id IN (SELECT o.owner.id FROM Organization o WHERE o.id IN :organizationIds) AND u.role IN ('ADMIN', 'ORGANIZER')")
       List<User> findAdmsByOrganizationIds(@Param("organizationIds") List<Long> organizationIds);

}
