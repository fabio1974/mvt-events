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
    Optional<User> findByIdWithClientContracts(@Param("id") UUID id);

       // Métodos de busca textual - mantidos (não cobertos por Specifications simples)
       // Usa immutable_unaccent para busca insensível a acentos (native query)
       @Query(value = "SELECT * FROM users u WHERE " +
                     "immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :searchTerm, '%'))) OR " +
                     "immutable_unaccent(LOWER(u.username)) LIKE immutable_unaccent(LOWER(CONCAT('%', :searchTerm, '%')))",
              nativeQuery = true)
       List<User> searchUsers(@Param("searchTerm") String searchTerm);

       @Query(value = "SELECT * FROM users u WHERE u.role = 'USER' AND " +
                     "immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :searchTerm, '%')))",
              nativeQuery = true)
       List<User> searchAthletes(@Param("searchTerm") String searchTerm);

       /**
        * Busca usuários ADM/ORGANIZER que são donos de organizações específicas
        */
       @Query("SELECT u FROM User u WHERE u.id IN (SELECT o.owner.id FROM Organization o WHERE o.id IN :organizationIds) AND u.role IN ('ADMIN', 'ORGANIZER')")
       List<User> findAdmsByOrganizationIds(@Param("organizationIds") List<Long> organizationIds);

       /**
        * Busca couriers próximos usando fórmula Haversine
        * Filtra apenas motoboys SEM entregas ativas (livres para aceitar nova entrega)
        * Ordena por distância (mais próximo primeiro)
        * @param latitude Latitude do ponto de referência
        * @param longitude Longitude do ponto de referência
        * @param radiusKm Raio de busca em quilômetros
        * @return Lista de couriers livres dentro do raio, ordenados por proximidade
        */
       @Query("SELECT u FROM User u WHERE u.role = 'COURIER' " +
              "AND u.gpsLatitude IS NOT NULL " +
              "AND u.gpsLongitude IS NOT NULL " +
              "AND NOT EXISTS (" +
              "  SELECT 1 FROM Delivery d WHERE d.courier.id = u.id " +
              "  AND d.status IN ('PENDING', 'ACCEPTED', 'IN_TRANSIT')" +
              ") " +
              "AND (u.serviceType IS NULL OR u.serviceType IN :serviceTypes) " +
              "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(u.gpsLatitude)) * " +
              "cos(radians(u.gpsLongitude) - radians(:longitude)) + " +
              "sin(radians(:latitude)) * sin(radians(u.gpsLatitude)))) <= :radiusKm " +
              "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(u.gpsLatitude)) * " +
              "cos(radians(u.gpsLongitude) - radians(:longitude)) + " +
              "sin(radians(:latitude)) * sin(radians(u.gpsLatitude)))) ASC")
       List<User> findAvailableCouriersNearby(@Param("latitude") Double latitude,
                                              @Param("longitude") Double longitude,
                                              @Param("radiusKm") Double radiusKm,
                                              @Param("serviceTypes") List<String> serviceTypes);

       /**
        * Busca couriers próximos com veículo ativo do tipo especificado.
        * Usado para filtrar couriers por tipo de veículo (MOTORCYCLE, CAR).
        * @param latitude Latitude do ponto de referência
        * @param longitude Longitude do ponto de referência
        * @param radiusKm Raio de busca em quilômetros
        * @param vehicleType Tipo de veículo ativo do courier (MOTORCYCLE ou CAR)
        * @return Lista de couriers com veículo ativo do tipo especificado, ordenados por proximidade
        */
       @Query("SELECT u FROM User u WHERE u.role = 'COURIER' " +
              "AND u.gpsLatitude IS NOT NULL " +
              "AND u.gpsLongitude IS NOT NULL " +
              "AND NOT EXISTS (" +
              "  SELECT 1 FROM Delivery d WHERE d.courier.id = u.id " +
              "  AND d.status IN ('PENDING', 'ACCEPTED', 'IN_TRANSIT')" +
              ") " +
              "AND EXISTS (" +
              "  SELECT 1 FROM Vehicle v WHERE v.owner.id = u.id " +
              "  AND v.isActive = true AND v.type = :vehicleType" +
              ") " +
              "AND (u.serviceType IS NULL OR u.serviceType IN :serviceTypes) " +
              "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(u.gpsLatitude)) * " +
              "cos(radians(u.gpsLongitude) - radians(:longitude)) + " +
              "sin(radians(:latitude)) * sin(radians(u.gpsLatitude)))) <= :radiusKm " +
              "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(u.gpsLatitude)) * " +
              "cos(radians(u.gpsLongitude) - radians(:longitude)) + " +
              "sin(radians(:latitude)) * sin(radians(u.gpsLatitude)))) ASC")
       List<User> findAvailableCouriersNearbyWithVehicleType(
                                              @Param("latitude") Double latitude,
                                              @Param("longitude") Double longitude,
                                              @Param("radiusKm") Double radiusKm,
                                              @Param("vehicleType") com.mvt.mvt_events.jpa.VehicleType vehicleType,
                                              @Param("serviceTypes") List<String> serviceTypes);

       // ============================================================================
       // EMAIL CONFIRMATION
       // ============================================================================

       /**
        * Busca usuário pelo token de confirmação de email
        */
       Optional<User> findByConfirmationToken(String confirmationToken);

       // ============================================================================
       // PASSWORD RESET
       // ============================================================================

       /**
        * Busca usuário pelo token de reset de senha
        */
       Optional<User> findByResetToken(String resetToken);

       // ============================================================================
       // COURIER SEARCH (typeahead para mobile)
       // ============================================================================

       /**
        * Busca motoboys por nome ou email que NÃO estão na organização especificada
        * Para typeahead mobile do gerente ao adicionar motoboys no grupo
        * Usa immutable_unaccent para busca insensível a acentos
        */
       @Query(value = "SELECT * FROM users u WHERE u.role = 'COURIER' " +
              "AND u.enabled = true " +
              "AND (immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%'))) OR immutable_unaccent(LOWER(u.username)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%')))) " +
              "AND NOT EXISTS (SELECT 1 FROM employment_contracts ec WHERE ec.courier_id = u.id AND ec.organization_id = :organizationId AND ec.is_active = true) " +
              "ORDER BY u.name ASC " +
              "LIMIT :limit", nativeQuery = true)
       List<User> searchCouriersNotInOrganization(@Param("search") String search, 
                                                   @Param("organizationId") Long organizationId, 
                                                   @Param("limit") Integer limit);

       /**
        * Busca motoboys por nome ou email (sem filtro de organização)
        * Usa immutable_unaccent para busca insensível a acentos
        */
       @Query(value = "SELECT * FROM users u WHERE u.role = 'COURIER' " +
              "AND u.enabled = true " +
              "AND (immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%'))) OR immutable_unaccent(LOWER(u.username)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%')))) " +
              "ORDER BY u.name ASC " +
              "LIMIT :limit", nativeQuery = true)
       List<User> searchCouriersWithLimit(@Param("search") String search, 
                                          @Param("limit") Integer limit);

       // ============================================================================
       // CLIENT SEARCH (typeahead para mobile)
       // ============================================================================

       /**
        * Busca clientes por nome ou email que NÃO estão na organização especificada
        * Para typeahead mobile do gerente ao adicionar clientes no grupo
        * Usa immutable_unaccent para busca insensível a acentos
        */
       @Query(value = "SELECT * FROM users u WHERE u.role IN ('CLIENT', 'CUSTOMER') " +
              "AND u.enabled = true " +
              "AND (immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%'))) OR immutable_unaccent(LOWER(u.username)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%')))) " +
              "AND NOT EXISTS (SELECT 1 FROM client_contracts cc WHERE cc.client_id = u.id AND cc.organization_id = :organizationId AND cc.status = 'ACTIVE') " +
              "ORDER BY u.name ASC " +
              "LIMIT :limit", nativeQuery = true)
       List<User> searchClientsNotInOrganization(@Param("search") String search, 
                                                  @Param("organizationId") Long organizationId, 
                                                  @Param("limit") Integer limit);

       /**
        * Busca clientes por nome ou email (sem filtro de organização)
        * Usa immutable_unaccent para busca insensível a acentos
        */
       @Query(value = "SELECT * FROM users u WHERE u.role IN ('CLIENT', 'CUSTOMER') " +
              "AND u.enabled = true " +
              "AND (immutable_unaccent(LOWER(u.name)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%'))) OR immutable_unaccent(LOWER(u.username)) LIKE immutable_unaccent(LOWER(CONCAT('%', :search, '%')))) " +
              "ORDER BY u.name ASC " +
              "LIMIT :limit", nativeQuery = true)
       List<User> searchClientsWithLimit(@Param("search") String search, 
                                         @Param("limit") Integer limit);

}
