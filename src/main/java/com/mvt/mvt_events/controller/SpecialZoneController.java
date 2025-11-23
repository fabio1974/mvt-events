package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.SpecialZone;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.service.SpecialZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller para gerenciar zonas especiais (periculosidade e alta renda)
 * Apenas ADMINs podem gerenciar zonas
 */
@RestController
@RequestMapping("/api/special-zones")
@Tag(name = "Special Zones", description = "Gerenciamento de zonas especiais para cálculo de frete")
public class SpecialZoneController {

    @Autowired
    private SpecialZoneService specialZoneService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Lista todas as zonas com paginação
     */
    @GetMapping
    @Operation(summary = "Listar zonas especiais", description = "Qualquer usuário autenticado pode ver")
    public ResponseEntity<Page<SpecialZone>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String zoneType) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // TODO: Implementar filtros se necessário
        Page<SpecialZone> zones = specialZoneService.findAll(pageable);
        return ResponseEntity.ok(zones);
    }

    /**
     * Busca zona por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar zona por ID")
    public ResponseEntity<SpecialZone> getById(@PathVariable Long id) {
        SpecialZone zone = specialZoneService.findById(id);
        return ResponseEntity.ok(zone);
    }

    /**
     * Cria nova zona especial
     * Apenas ADMIN
     */
    @PostMapping
    @Operation(summary = "Criar zona especial", description = "Apenas ADMIN pode criar")
    public ResponseEntity<SpecialZone> create(
            @RequestBody @Valid SpecialZone zone,
            Authentication authentication) {
        
        validateAdmin(authentication);
        SpecialZone created = specialZoneService.save(zone);
        return ResponseEntity.ok(created);
    }

    /**
     * Atualiza zona existente
     * Apenas ADMIN
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar zona especial", description = "Apenas ADMIN pode atualizar")
    public ResponseEntity<SpecialZone> update(
            @PathVariable Long id,
            @RequestBody @Valid SpecialZone updatedZone,
            Authentication authentication) {
        
        validateAdmin(authentication);
        
        SpecialZone existing = specialZoneService.findById(id);
        
        // Atualizar campos
        existing.setLatitude(updatedZone.getLatitude());
        existing.setLongitude(updatedZone.getLongitude());
        existing.setAddress(updatedZone.getAddress());
        existing.setZoneType(updatedZone.getZoneType());
        existing.setIsActive(updatedZone.getIsActive());
        existing.setRadiusMeters(updatedZone.getRadiusMeters());
        existing.setNotes(updatedZone.getNotes());
        
        SpecialZone saved = specialZoneService.save(existing);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deleta zona especial
     * Apenas ADMIN
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar zona especial", description = "Apenas ADMIN pode deletar")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        
        validateAdmin(authentication);
        specialZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lista apenas zonas ativas
     */
    @GetMapping("/active")
    @Operation(summary = "Listar zonas ativas")
    public ResponseEntity<List<SpecialZone>> listActive() {
        List<SpecialZone> zones = specialZoneService.findActiveZones();
        return ResponseEntity.ok(zones);
    }

    /**
     * Busca zona mais próxima de uma coordenada
     */
    @GetMapping("/nearest")
    @Operation(summary = "Buscar zona mais próxima", description = "Busca zona considerando o raio específico de cada zona")
    public ResponseEntity<SpecialZone> findNearest(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        
        return specialZoneService.findNearestZone(latitude, longitude)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Valida se o usuário é ADMIN
     */
    private void validateAdmin(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Apenas ADMIN pode gerenciar zonas especiais");
        }
    }
}
