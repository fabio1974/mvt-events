package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.SpecialZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para SpecialZone
 */
@Repository
public interface SpecialZoneRepository extends JpaRepository<SpecialZone, Long> {

    /**
     * Busca zonas ativas por tipo
     */
    List<SpecialZone> findByIsActiveAndZoneType(Boolean isActive, SpecialZone.ZoneType zoneType);

    /**
     * Busca todas as zonas ativas
     */
    List<SpecialZone> findByIsActive(Boolean isActive);

    /**
     * Busca a zona especial mais próxima de uma coordenada específica
     * usando a fórmula de Haversine para calcular distância.
     * Cada zona tem seu próprio raio de cobertura (radius_meters).
     * 
     * @param latitude Latitude do ponto de destino
     * @param longitude Longitude do ponto de destino
     * @return Zona especial mais próxima se existir dentro do seu raio específico
     */
    @Query(value = """
        SELECT z.*, 
               (6371000 * acos(
                   cos(radians(:latitude)) * cos(radians(z.latitude)) * 
                   cos(radians(z.longitude) - radians(:longitude)) + 
                   sin(radians(:latitude)) * sin(radians(z.latitude))
               )) as distance
        FROM special_zones z
        WHERE z.is_active = true
        AND (6371000 * acos(
                cos(radians(:latitude)) * cos(radians(z.latitude)) * 
                cos(radians(z.longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(z.latitude))
            )) <= z.radius_meters
        ORDER BY distance
        LIMIT 1
        """, nativeQuery = true)
    Optional<SpecialZone> findNearestZoneWithinRadius(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );
}
