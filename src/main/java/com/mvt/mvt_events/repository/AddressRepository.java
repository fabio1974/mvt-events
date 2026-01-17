package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Address
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Busca endereço por ID do usuário
     */
    Optional<Address> findByUserId(UUID userId);

    /**
     * Busca todos os endereços de um usuário
     */
    List<Address> findAllByUserId(UUID userId);

    /**
     * Busca todos os endereços de um usuário com City carregada (evita lazy loading)
     */
    @Query("SELECT a FROM Address a LEFT JOIN FETCH a.city WHERE a.user.id = :userId")
    List<Address> findAllByUserIdWithCity(@Param("userId") UUID userId);

    /**
     * Busca endereço por ID do usuário com City carregada
     */
    @Query("SELECT a FROM Address a LEFT JOIN FETCH a.city WHERE a.user.id = :userId")
    Optional<Address> findByUserIdWithCity(@Param("userId") UUID userId);

    /**
     * Busca endereços por cidade
     */
    List<Address> findByCityId(Long cityId);

    /**
     * Busca endereços por bairro
     */
    List<Address> findByNeighborhoodContainingIgnoreCase(String neighborhood);

    /**
     * Busca endereços por rua
     */
    List<Address> findByStreetContainingIgnoreCase(String street);

    /**
     * Verifica se já existe endereço para o usuário
     */
    boolean existsByUserId(UUID userId);

    /**
     * Deleta endereço por ID do usuário
     */
    void deleteByUserId(UUID userId);

    /**
     * Busca endereços dentro de um raio (em km) de uma coordenada
     */
    @Query(value = "SELECT a.* FROM addresses a " +
           "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(a.latitude)) * " +
           "cos(radians(a.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(a.latitude)))) <= :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(a.latitude)) * " +
           "cos(radians(a.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(a.latitude))))",
           nativeQuery = true)
    List<Address> findWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm
    );
}
