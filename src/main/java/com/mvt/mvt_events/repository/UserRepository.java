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
       @EntityGraph(attributePaths = { "city" })
       Optional<User> findByUsername(String username);

       // Método para autenticação (carrega todos os relacionamentos necessários para
       // JWT)
       @EntityGraph(attributePaths = { "city" })
       @Query("SELECT u FROM User u WHERE u.username = :username")
       Optional<User> findByUsernameForAuth(@Param("username") String username);

       // Método para autenticação por CPF (carrega todos os relacionamentos necessários para JWT)
       @EntityGraph(attributePaths = { "city" })
       @Query("SELECT u FROM User u WHERE u.cpf = :cpf")
       Optional<User> findByCpfForAuth(@Param("cpf") String cpf);

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

       boolean existsByCpf(String cpf);

       @Query("SELECT u FROM User u WHERE u.cpf = :cpf")
       Optional<User> findByCpf(@Param("cpf") String cpf);

       // Métodos de busca textual - mantidos (não cobertos por Specifications simples)
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