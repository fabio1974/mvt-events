package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByDocumentNumber(String documentNumber);

    // ============================================================================
    // ROLE-BASED QUERIES
    // ============================================================================

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'USER'")
    List<User> findAllAthletes();

    @Query("SELECT u FROM User u WHERE u.role = 'ORGANIZER'")
    List<User> findAllOrganizers();

    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN'")
    List<User> findAllAdmins();

    // ============================================================================
    // ORGANIZATION-BASED QUERIES
    // ============================================================================

    @Query("SELECT u FROM User u WHERE u.organization.id = :organizationId")
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT u FROM User u WHERE u.role = 'ORGANIZER' AND u.organization.id = :organizationId")
    List<User> findOrganizersByOrganizationId(@Param("organizationId") Long organizationId);

    // ============================================================================
    // ATHLETE-SPECIFIC QUERIES (for users with athlete data)
    // ============================================================================

    @Query("SELECT u FROM User u WHERE u.documentNumber = :documentNumber")
    Optional<User> findByDocumentNumber(@Param("documentNumber") String documentNumber);

    @Query("SELECT u FROM User u WHERE u.gender = :gender")
    List<User> findByGender(@Param("gender") User.Gender gender);

    @Query("SELECT u FROM User u WHERE u.city = :city")
    List<User> findByCity(@Param("city") String city);

    @Query("SELECT u FROM User u WHERE u.state = :state")
    List<User> findByState(@Param("state") String state);

    @Query("SELECT u FROM User u WHERE u.country = :country")
    List<User> findByCountry(@Param("country") String country);

    // ============================================================================
    // SEARCH QUERIES
    // ============================================================================

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    @Query("SELECT u FROM User u WHERE u.role = 'USER' AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchAthletes(@Param("searchTerm") String searchTerm);
}