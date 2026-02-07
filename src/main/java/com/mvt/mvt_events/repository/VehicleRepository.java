package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Vehicle;
import com.mvt.mvt_events.jpa.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para Vehicle
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Busca veículo por placa
     */
    Optional<Vehicle> findByPlate(String plate);

    /**
     * Busca veículos por proprietário
     */
    @Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId")
    List<Vehicle> findByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Busca TODOS os veículos por proprietário (com JOIN FETCH para evitar lazy loading)
     */
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.owner WHERE v.owner.id = :ownerId ORDER BY v.isActive DESC, v.id")
    List<Vehicle> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Busca veículos ativos por proprietário
     */
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.owner WHERE v.owner.id = :ownerId AND v.isActive = true")
    List<Vehicle> findActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Busca veículos por tipo
     */
    List<Vehicle> findByType(VehicleType type);

    /**
     * Busca veículos ativos por tipo
     */
    List<Vehicle> findByTypeAndIsActiveTrue(VehicleType type);

    /**
     * Verifica se placa já existe
     */
    boolean existsByPlate(String plate);

    /**
     * Busca veículo ativo (principal) do motorista
     * A constraint garante que só há um veículo ativo por usuário
     */
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.owner WHERE v.owner.id = :ownerId AND v.isActive = true")
    Optional<Vehicle> findActiveVehicleByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Conta veículos ativos de um proprietário
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.owner.id = :ownerId AND v.isActive = true")
    long countActiveByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Busca veículos por proprietário e tipo
     */
    @Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId AND v.type = :type AND v.isActive = true")
    List<Vehicle> findByOwnerIdAndType(@Param("ownerId") UUID ownerId, @Param("type") VehicleType type);
}
