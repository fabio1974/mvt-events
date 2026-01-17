package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.repository.AddressRepository;
import com.mvt.mvt_events.common.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Endereços", description = "Gerenciamento de endereços de usuários")
public class AddressController {

    private final AddressRepository addressRepository;
    private final JwtUtil jwtUtil;

    /**
     * DTO para cidade
     */
    @Data
    @Builder
    public static class CityResponse {
        private Long id;
        private String name;
        private String state;
    }

    /**
     * DTO para resposta de endereço (evita lazy loading)
     */
    @Data
    @Builder
    public static class AddressResponse {
        private Long id;
        private String street;
        private String number;
        private String complement;
        private String neighborhood;
        private String referencePoint;
        private String zipCode;
        private Double latitude;
        private Double longitude;
        private Boolean isDefault;
        private CityResponse city;
    }

    private AddressResponse toResponse(Address address) {
        CityResponse cityResponse = null;
        if (address.getCity() != null) {
            cityResponse = CityResponse.builder()
                    .id(address.getCity().getId())
                    .name(address.getCity().getName())
                    .state(address.getCity().getState())
                    .build();
        }
        
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .neighborhood(address.getNeighborhood())
                .referencePoint(address.getReferencePoint())
                .zipCode(address.getZipCode())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.getIsDefault())
                .city(cityResponse)
                .build();
    }

    /**
     * Busca todos os endereços do usuário logado
     * 
     * Endpoint: GET /api/addresses/me
     * 
     * @param authHeader Bearer token JWT
     * @return Lista de endereços do usuário logado
     */
    @GetMapping("/me")
    @Operation(summary = "Buscar endereços do usuário logado", description = "Retorna todos os endereços do usuário autenticado via JWT")
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Buscando endereços do usuário logado: {}", userId);
        
        List<AddressResponse> addresses = addressRepository.findAllByUserIdWithCity(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(addresses);
    }

    /**
     * Busca endereço por User ID (UUID)
     * 
     * Endpoint: GET /api/addresses/{userId}
     * 
     * @param userId UUID do usuário
     * @return Address do usuário ou 404
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Buscar endereço por User ID", description = "Retorna o endereço de um usuário específico")
    public ResponseEntity<AddressResponse> getAddressByUserId(@PathVariable UUID userId) {
        log.debug("📍 Buscando endereço do usuário: {}", userId);
        
        return addressRepository.findByUserIdWithCity(userId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
