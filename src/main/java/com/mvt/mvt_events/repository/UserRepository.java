package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

       // Métodos de busca única e validação - mantidos
       Optional<User> findByUsername(String username);

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
}