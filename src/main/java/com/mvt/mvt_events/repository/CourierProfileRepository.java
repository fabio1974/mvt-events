package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.CourierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para CourierProfile
 * Perfil de entregadores com métricas e informações de veículo
 */
@Repository
public interface CourierProfileRepository
        extends JpaRepository<CourierProfile, Long>, JpaSpecificationExecutor<CourierProfile> {

    /**
     * Busca perfil de courier por User ID
     */
    Optional<CourierProfile> findByUserId(UUID userId);

    /**
     * Verifica se existe perfil para o usuário
     */
    boolean existsByUserId(UUID userId);

    /**
     * Busca perfil por placa do veículo
     */
    Optional<CourierProfile> findByVehiclePlate(String vehiclePlate);

    /**
     * Busca couriers disponíveis (ACTIVE) em uma região
     * 
     * @param latitude  Latitude central
     * @param longitude Longitude central
     * @param radiusKm  Raio de busca em km
     */
    @Query(value = "SELECT cp.* FROM courier_profiles cp " +
            "JOIN users u ON cp.user_id = u.id " +
            "WHERE cp.status = 'ACTIVE' " +
            "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
            "cos(radians(u.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(u.latitude)))) <= :radiusKm " +
            "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(u.latitude)) * " +
            "cos(radians(u.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(u.latitude))))", nativeQuery = true)
    java.util.List<CourierProfile> findAvailableCouriersNearby(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm);
}
