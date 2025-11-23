package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.SpecialZone;
import com.mvt.mvt_events.repository.SpecialZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciar zonas especiais
 */
@Service
public class SpecialZoneService {

    @Autowired
    private SpecialZoneRepository specialZoneRepository;

    /**
     * Busca zona especial próxima ao destino considerando o raio específico de cada zona.
     * 
     * Se o destino estiver dentro de múltiplas zonas sobrepostas, retorna a ZONA MAIS PRÓXIMA
     * (menor distância entre o destino e o centro da zona), independente do tipo.
     * 
     * Exemplos:
     * - Destino a 50m da Zona A (HIGH_INCOME) e 100m da Zona B (DANGER) → retorna Zona A
     * - Destino a 200m da Zona C (DANGER) e 150m da Zona D (HIGH_INCOME) → retorna Zona D
     * 
     * @param latitude Latitude do destino
     * @param longitude Longitude do destino
     * @return Zona especial mais próxima ou null se não houver zona no raio
     */
    public Optional<SpecialZone> findNearestZone(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        return specialZoneRepository.findNearestZoneWithinRadius(latitude, longitude);
    }

    /**
     * Cria ou atualiza uma zona especial
     */
    @Transactional
    public SpecialZone save(SpecialZone zone) {
        return specialZoneRepository.save(zone);
    }

    /**
     * Busca zona por ID
     */
    public SpecialZone findById(Long id) {
        return specialZoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zona especial não encontrada"));
    }

    /**
     * Lista todas as zonas com paginação
     */
    public Page<SpecialZone> findAll(Pageable pageable) {
        return specialZoneRepository.findAll(pageable);
    }

    /**
     * Lista zonas ativas
     */
    public List<SpecialZone> findActiveZones() {
        return specialZoneRepository.findByIsActive(true);
    }

    /**
     * Lista zonas ativas por tipo
     */
    public List<SpecialZone> findActiveZonesByType(SpecialZone.ZoneType type) {
        return specialZoneRepository.findByIsActiveAndZoneType(true, type);
    }

    /**
     * Deleta uma zona
     */
    @Transactional
    public void delete(Long id) {
        specialZoneRepository.deleteById(id);
    }
}
