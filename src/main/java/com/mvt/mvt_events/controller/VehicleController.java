package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.common.JwtUtil;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Vehicle;
import com.mvt.mvt_events.jpa.VehicleColor;
import com.mvt.mvt_events.jpa.VehicleType;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.VehicleRepository;
import com.mvt.mvt_events.service.UserActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Veículos", description = "Gerenciamento de veículos dos motoristas")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EntityManager entityManager;
    private final UserActivationService userActivationService;

    /**
     * DTO para resposta de veículo
     */
    @Data
    @Builder
    public static class VehicleResponse {
        private Long id;
        private String type;
        private String plate;
        private String brand;
        private String model;
        private String color;
        private String year;
        private Boolean isActive;
        private String ownerName;
        private UUID ownerId;
    }

    /**
     * DTO para criar/atualizar veículo
     */
    @Data
    public static class VehicleRequest {
        private String type; // MOTORCYCLE ou CAR
        private String plate;
        private String brand;
        private String model;
        private String color;
        private String year;
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .type(vehicle.getType().name())
                .plate(vehicle.getPlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor().name())
                .year(vehicle.getYear())
                .isActive(vehicle.getIsActive())
                .ownerName(vehicle.getOwnerName())
                .ownerId(vehicle.getOwner().getId())
                .build();
    }

    /**
     * Lista todos os veículos (paginado) - Admin
     */
    @GetMapping
    @Operation(summary = "Listar todos os veículos", description = "Retorna todos os veículos cadastrados (paginado)")
    public ResponseEntity<Page<VehicleResponse>> getAllVehicles(Pageable pageable) {
        log.debug("📋 Listando todos os veículos - Página: {}", pageable.getPageNumber());
        
        Page<VehicleResponse> vehicles = vehicleRepository.findAll(pageable)
                .map(this::toResponse);
        
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Lista todos os veículos do usuário logado
     */
    @GetMapping("/me")
    @Operation(summary = "Listar meus veículos", description = "Retorna todos os veículos do usuário autenticado (ativos e inativos)")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("🚗 Buscando todos os veículos do usuário: {}", userId);
        
        List<VehicleResponse> vehicles = vehicleRepository.findAllByOwnerId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Lista veículos de um usuário específico por userId
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Listar veículos por User ID", description = "Retorna todos os veículos de um usuário específico")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByUserId(@PathVariable UUID userId) {
        log.debug("🚗 Buscando veículos do usuário: {}", userId);
        
        List<VehicleResponse> vehicles = vehicleRepository.findActiveByOwnerId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Busca veículo por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID", description = "Retorna detalhes de um veículo específico")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria novo veículo para o usuário logado
     * O novo veículo sempre se torna o ativo, desativando qualquer outro
     */
    @PostMapping
    @Transactional
    @Operation(summary = "Cadastrar veículo", description = "Cadastra um novo veículo para o usuário autenticado e o define como ativo")
    public ResponseEntity<?> createVehicle(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody VehicleRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        // Verifica se placa já existe
        if (vehicleRepository.existsByPlate(request.getPlate())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Placa " + request.getPlate() + " já está cadastrada");
        }
        
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Desativa todos os veículos do usuário antes de criar o novo
        List<Vehicle> userVehicles = vehicleRepository.findByOwnerId(userId);
        userVehicles.forEach(v -> {
            v.setIsActive(false);
            vehicleRepository.save(v);
        });
        
        // Força a persistência das desativações no banco ANTES de criar o novo
        entityManager.flush();
        
        // Cria o novo veículo como ativo (principal)
        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .type(VehicleType.valueOf(request.getType().toUpperCase()))
                .plate(request.getPlate().toUpperCase())
                .brand(request.getBrand())
                .model(request.getModel())
                .color(VehicleColor.valueOf(request.getColor().toUpperCase()))
                .year(request.getYear())
                .isActive(true)  // Novo veículo sempre é ativo
                .build();
        
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("✅ Veículo cadastrado: {} - Proprietário: {} - Agora é o veículo ativo",
                saved.getPlate(), owner.getName());

        userActivationService.recalculate(owner.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * Atualiza veículo
     */
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Atualizar veículo", description = "Atualiza dados de um veículo")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody VehicleRequest request) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Verifica se o veículo pertence ao usuário logado
        if (!vehicle.getOwner().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não tem permissão para editar este veículo");
        }
        
        // Verifica se placa mudou e se já existe
        if (!vehicle.getPlate().equalsIgnoreCase(request.getPlate()) 
                && vehicleRepository.existsByPlate(request.getPlate())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Placa " + request.getPlate() + " já está cadastrada");
        }
        
        vehicle.setType(VehicleType.valueOf(request.getType().toUpperCase()));
        vehicle.setPlate(request.getPlate().toUpperCase());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setColor(VehicleColor.valueOf(request.getColor().toUpperCase()));
        vehicle.setYear(request.getYear());
        
        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("✏️ Veículo atualizado: {}", updated.getPlate());
        
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * Desativa veículo (soft delete)
     */
    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Desativar veículo", description = "Desativa um veículo (não remove do banco)")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Verifica se o veículo pertence ao usuário logado
        if (!vehicle.getOwner().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não tem permissão para deletar este veículo");
        }
        
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
        
        log.info("🗑️ Veículo desativado: {}", vehicle.getPlate());
        
        return ResponseEntity.ok("Veículo desativado com sucesso");
    }

    /**
     * Reativa veículo
     * Desativa automaticamente todos os outros veículos do usuário
     */
    @PutMapping("/{id}/reactivate")
    @Transactional
    @Operation(summary = "Reativar veículo", description = "Reativa um veículo previamente desativado e desativa os demais")
    public ResponseEntity<?> reactivateVehicle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Verifica se o veículo pertence ao usuário logado
        if (!vehicle.getOwner().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não tem permissão para reativar este veículo");
        }
        
        // Desativa todos os veículos do usuário (incluindo o atual)
        List<Vehicle> userVehicles = vehicleRepository.findByOwnerId(userId);
        userVehicles.forEach(v -> {
            v.setIsActive(false);
            vehicleRepository.save(v);
        });
        
        // Força a persistência das desativações no banco ANTES de ativar o novo
        entityManager.flush();
        
        // Ativa o veículo selecionado (reativa)
        vehicle.setIsActive(true);
        Vehicle reactivated = vehicleRepository.save(vehicle);
        
        log.info("♻️ Veículo reativado: {} - Outros veículos desativados", reactivated.getPlate());
        
        return ResponseEntity.ok(toResponse(reactivated));
    }

    /**
     * Define veículo como ativo (principal) para o motorista
     * A constraint do banco garante que só pode haver um veículo ativo por usuário
     */
    @PutMapping("/{id}/set-active")
    @Transactional
    @Operation(summary = "Definir veículo ativo", description = "Define qual veículo está em uso ativo pelo motorista")
    public ResponseEntity<?> setActiveVehicle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        
        // Verifica se o veículo pertence ao usuário logado
        if (!vehicle.getOwner().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não tem permissão para modificar este veículo");
        }
        
        // Desativa todos os veículos do motorista (incluindo o atual se já estava ativo)
        List<Vehicle> userVehicles = vehicleRepository.findByOwnerId(userId);
        userVehicles.forEach(v -> {
            v.setIsActive(false);
            vehicleRepository.save(v);
        });
        
        // Força a persistência das desativações no banco ANTES de ativar o novo
        entityManager.flush();
        
        // Ativa o veículo selecionado (torna-o o principal)
        vehicle.setIsActive(true);
        Vehicle updated = vehicleRepository.save(vehicle);
        
        log.info("🚗 Veículo principal definido: {} - Proprietário: {}", updated.getPlate(), updated.getOwnerName());
        
        return ResponseEntity.ok(toResponse(updated));
    }

    /**
     * Busca veículo ativo (em uso) do motorista logado
     */
    @GetMapping("/me/active")
    @Operation(summary = "Buscar meu veículo ativo", description = "Retorna o veículo que está em uso ativo pelo motorista autenticado")
    public ResponseEntity<?> getMyActiveVehicle(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("🔍 Buscando veículo ativo do usuário: {}", userId);
        
        Optional<Vehicle> activeVehicle = vehicleRepository.findActiveVehicleByOwnerId(userId);
        
        if (activeVehicle.isPresent()) {
            return ResponseEntity.ok(toResponse(activeVehicle.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nenhum veículo ativo definido");
        }
    }
}
