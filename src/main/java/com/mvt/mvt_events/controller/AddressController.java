package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Address;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.AddressRepository;
import com.mvt.mvt_events.repository.CityRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.common.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
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

    /**
     * DTO para request de criação/atualização de endereço
     */
    @Data
    public static class AddressRequest {
        private String street;
        private String number;
        private String complement;
        private String neighborhood;
        private String referencePoint;
        private String zipCode;
        private Double latitude;
        private Double longitude;
        private Boolean isDefault;
        private Long cityId;
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
     * Busca o endereço padrão do usuário logado
     * 
     * Endpoint: GET /api/addresses/me/default
     * 
     * @param authHeader Bearer token JWT
     * @return Endereço padrão do usuário logado ou 404
     */
    @GetMapping("/me/default")
    @Operation(summary = "Buscar endereço padrão", description = "Retorna o endereço marcado como padrão do usuário autenticado")
    public ResponseEntity<?> getDefaultAddress(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Buscando endereço padrão do usuário: {}", userId);
        
        List<Address> addresses = addressRepository.findAllByUserIdWithCity(userId);
        Address defaultAddress = addresses.stream()
                .filter(Address::getIsDefault)
                .findFirst()
                .orElse(null);
        
        if (defaultAddress == null) {
            log.debug("⚠️ Usuário {} não possui endereço padrão", userId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(toResponse(defaultAddress));
    }

    /**
     * Atualiza o endereço padrão do usuário logado
     * 
     * Endpoint: PUT /api/addresses/me/default
     * 
     * @param request Dados do endereço para atualização
     * @param authHeader Bearer token JWT
     * @return Endereço atualizado ou erro
     */
    @PutMapping("/me/default")
    @Operation(summary = "Atualizar endereço padrão", 
               description = "Atualiza os dados do endereço marcado como padrão do usuário logado")
    public ResponseEntity<?> updateDefaultAddress(
            @RequestBody @Valid AddressRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Atualizando endereço padrão do usuário: {}", userId);
        
        // Buscar endereço padrão do usuário
        List<Address> addresses = addressRepository.findAllByUserIdWithCity(userId);
        Address defaultAddress = addresses.stream()
                .filter(Address::getIsDefault)
                .findFirst()
                .orElse(null);
        
        if (defaultAddress == null) {
            log.warn("⚠️ Usuário {} não possui endereço padrão para atualizar", userId);
            return ResponseEntity.status(404).body("Usuário não possui endereço padrão");
        }
        
        // Atualizar campos
        if (request.getStreet() != null) defaultAddress.setStreet(request.getStreet());
        if (request.getNumber() != null) defaultAddress.setNumber(request.getNumber());
        if (request.getComplement() != null) defaultAddress.setComplement(request.getComplement());
        if (request.getNeighborhood() != null) defaultAddress.setNeighborhood(request.getNeighborhood());
        if (request.getReferencePoint() != null) defaultAddress.setReferencePoint(request.getReferencePoint());
        if (request.getZipCode() != null) defaultAddress.setZipCode(request.getZipCode().replaceAll("[^0-9]", ""));
        if (request.getLatitude() != null) defaultAddress.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) defaultAddress.setLongitude(request.getLongitude());
        
        // Atualizar cidade
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            defaultAddress.setCity(city);
        }
        
        Address savedAddress = addressRepository.save(defaultAddress);
        
        log.info("✅ Endereço padrão atualizado para usuário {}", userId);
        
        // Buscar novamente com city carregada via JOIN FETCH
        Address addressWithCity = addressRepository.findByIdWithCity(savedAddress.getId())
                .orElse(savedAddress);
        
        return ResponseEntity.ok(toResponse(addressWithCity));
    }

    /**
     * Cria um novo endereço padrão para o usuário logado
     * 
     * Endpoint: POST /api/addresses/me/default
     * 
     * @param request Dados do novo endereço
     * @param authHeader Bearer token JWT
     * @return Endereço criado como padrão
     */
    @PostMapping("/me/default")
    @Operation(summary = "Criar endereço padrão", 
               description = "Cria um novo endereço e o marca como padrão do usuário logado. Desmarca os demais endereços.")
    public ResponseEntity<?> createDefaultAddress(
            @RequestBody @Valid AddressRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Criando novo endereço padrão para usuário: {}", userId);
        
        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Desmarcar todos os endereços existentes do usuário
        List<Address> userAddresses = addressRepository.findAllByUserIdWithCity(userId);
        userAddresses.forEach(addr -> {
            if (addr.getIsDefault()) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        });
        
        // Criar novo endereço
        Address address = new Address();
        address.setUser(user);
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setReferencePoint(request.getReferencePoint());
        address.setZipCode(request.getZipCode() != null ? request.getZipCode().replaceAll("[^0-9]", "") : null);
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setIsDefault(true); // Sempre marca como padrão
        
        // Atualizar cidade
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            address.setCity(city);
        }
        
        Address savedAddress = addressRepository.save(address);
        
        log.info("✅ Novo endereço padrão criado para usuário {}: ID {}", userId, savedAddress.getId());
        
        // Buscar novamente com city carregada via JOIN FETCH
        Address addressWithCity = addressRepository.findByIdWithCity(savedAddress.getId())
                .orElse(savedAddress);
        
        return ResponseEntity.status(201).body(toResponse(addressWithCity));
    }

    /**
     * Define um endereço como padrão para o usuário logado
     * 
     * Endpoint: PUT /api/addresses/{addressId}/set-default
     * 
     * @param addressId ID do endereço a ser marcado como default
     * @param authHeader Bearer token JWT
     * @return Endereço atualizado ou erro
     */
    @PutMapping("/{addressId}/set-default")
    @Operation(summary = "Definir endereço como padrão", 
               description = "Marca o endereço especificado como padrão do usuário logado e desmarca os demais")
    public ResponseEntity<?> setDefaultAddress(
            @PathVariable Long addressId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Definindo endereço {} como padrão para usuário: {}", addressId, userId);
        
        // Verificar se o endereço existe e pertence ao usuário
        Address targetAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
        
        if (!targetAddress.getUser().getId().equals(userId)) {
            log.warn("⚠️ Usuário {} tentou modificar endereço {} de outro usuário", userId, addressId);
            return ResponseEntity.status(403).body("Você não tem permissão para modificar este endereço");
        }
        
        // Desmarcar todos os outros endereços do usuário
        List<Address> userAddresses = addressRepository.findAllByUserIdWithCity(userId);
        userAddresses.forEach(addr -> {
            if (addr.getIsDefault()) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        });
        
        // Marcar o endereço alvo como default
        targetAddress.setIsDefault(true);
        Address savedAddress = addressRepository.save(targetAddress);
        
        log.info("✅ Endereço {} definido como padrão para usuário {}", addressId, userId);
        
        return ResponseEntity.ok(toResponse(savedAddress));
    }

    /**
     * Atualiza um endereço existente do usuário logado
     * 
     * Endpoint: PUT /api/addresses/{addressId}
     * 
     * @param addressId ID do endereço a ser atualizado
     * @param request Dados do endereço para atualização
     * @param authHeader Bearer token JWT
     * @return Endereço atualizado ou erro
     */
    @PutMapping("/{addressId}")
    @Operation(summary = "Atualizar endereço", 
               description = "Atualiza os dados de um endereço do usuário logado. Se isDefault=true, desmarca os demais endereços.")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long addressId,
            @RequestBody @Valid AddressRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Atualizando endereço {} para usuário: {}", addressId, userId);
        
        // Verificar se o endereço existe e pertence ao usuário
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
        
        if (!address.getUser().getId().equals(userId)) {
            log.warn("⚠️ Usuário {} tentou modificar endereço {} de outro usuário", userId, addressId);
            return ResponseEntity.status(403).body("Você não tem permissão para modificar este endereço");
        }
        
        // Atualizar campos
        if (request.getStreet() != null) address.setStreet(request.getStreet());
        if (request.getNumber() != null) address.setNumber(request.getNumber());
        if (request.getComplement() != null) address.setComplement(request.getComplement());
        if (request.getNeighborhood() != null) address.setNeighborhood(request.getNeighborhood());
        if (request.getReferencePoint() != null) address.setReferencePoint(request.getReferencePoint());
        if (request.getZipCode() != null) address.setZipCode(request.getZipCode().replaceAll("[^0-9]", ""));
        if (request.getLatitude() != null) address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) address.setLongitude(request.getLongitude());
        
        // Atualizar cidade
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            address.setCity(city);
        }
        
        // Gerenciar isDefault
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // Desmarcar todos os outros endereços do usuário
            List<Address> userAddresses = addressRepository.findAllByUserIdWithCity(userId);
            userAddresses.forEach(addr -> {
                if (!addr.getId().equals(addressId) && addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            });
            address.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            address.setIsDefault(false);
        }
        
        Address savedAddress = addressRepository.save(address);
        
        log.info("✅ Endereço {} atualizado para usuário {}", addressId, userId);
        
        return ResponseEntity.ok(toResponse(savedAddress));
    }

    /**
     * Cria um novo endereço para o usuário logado
     * 
     * Endpoint: POST /api/addresses
     * 
     * @param request Dados do novo endereço
     * @param authHeader Bearer token JWT
     * @return Endereço criado
     */
    @PostMapping
    @Operation(summary = "Criar novo endereço", 
               description = "Cria um novo endereço para o usuário logado. Se isDefault=true, desmarca os demais endereços.")
    public ResponseEntity<?> createAddress(
            @RequestBody @Valid AddressRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        UUID userId = UUID.fromString(jwtUtil.getUserIdFromToken(token));
        
        log.debug("📍 Criando novo endereço para usuário: {}", userId);
        
        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Criar novo endereço
        Address address = new Address();
        address.setUser(user);
        address.setStreet(request.getStreet());
        address.setNumber(request.getNumber());
        address.setComplement(request.getComplement());
        address.setNeighborhood(request.getNeighborhood());
        address.setReferencePoint(request.getReferencePoint());
        address.setZipCode(request.getZipCode() != null ? request.getZipCode().replaceAll("[^0-9]", "") : null);
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        
        // Atualizar cidade
        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            address.setCity(city);
        }
        
        // Gerenciar isDefault
        if (request.getIsDefault() != null && request.getIsDefault()) {
            // Desmarcar todos os outros endereços do usuário
            List<Address> userAddresses = addressRepository.findAllByUserIdWithCity(userId);
            userAddresses.forEach(addr -> {
                if (addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            });
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }
        
        Address savedAddress = addressRepository.save(address);
        
        log.info("✅ Novo endereço criado para usuário {}: ID {}", userId, savedAddress.getId());
        
        return ResponseEntity.ok(toResponse(savedAddress));
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
