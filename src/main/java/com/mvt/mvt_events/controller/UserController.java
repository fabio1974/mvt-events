package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.common.CityDTO;
import com.mvt.mvt_events.dto.common.OrganizationDTO;
import com.mvt.mvt_events.dto.mapper.DTOMapper;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Suporta filtros: role, organizationId, enabled, search (nome ou email)")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<User> users = userService.listWithFilters(role, organizationId, enabled, search, pageable);
        return users.map(UserResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UserResponse get(@PathVariable UUID id) {
        User user = userService.findById(id);
        UserResponse response = new UserResponse(user);

        // Carregar contratos de trabalho (se for COURIER)
        if (user.getRole() == User.Role.COURIER) {
            response.setEmploymentContracts(buildEmploymentContractsForUser(id));
        }

        // Carregar contratos de serviço (se for CLIENT)
        if (user.getRole() == User.Role.CLIENT) {
            response.setClientContracts(buildClientContractsForUser(id));
        }

        return response;
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário")
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request,
            Authentication authentication) {
        User createdUser = userService.createUser(request, authentication);
        return ResponseEntity.ok(new UserResponse(createdUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody @Valid UserUpdateRequest request,
            Authentication authentication) {
        User updatedUser = userService.updateUser(id, request, authentication);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        try {
            userService.deleteUser(id, authentication);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Atualizar localização do usuário", description = "Atualiza latitude, longitude e timestamp do usuário")
    public ResponseEntity<UserResponse> updateLocation(
            @PathVariable UUID id,
            @RequestBody @Valid LocationUpdateRequest request,
            Authentication authentication) {
        User updatedUser = userService.updateUserLocation(id, request.getLatitude(), request.getLongitude(),
                request.getUpdatedAt(), authentication);
        return ResponseEntity.ok(new UserResponse(updatedUser));
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Constrói lista de EmploymentContracts para um usuário COURIER
     */
    private java.util.List<EmploymentContractForUserResponse> buildEmploymentContractsForUser(UUID userId) {
        java.util.List<Object[]> contractsData = userService.getEmploymentContractsForUser(userId);
        return contractsData.stream()
                .map(data -> {
                    EmploymentContractForUserResponse response = new EmploymentContractForUserResponse();

                    // Criar objeto aninhado OrganizationDTO (consistente com metadata)
                    OrganizationDTO orgDto = new OrganizationDTO();
                    orgDto.setId((Long) data[0]); // organization_id
                    orgDto.setName((String) data[1]); // organization name
                    response.setOrganization(orgDto);

                    response.setLinkedAt(data[2] != null ? data[2].toString() : null);
                    response.setIsActive((Boolean) data[3]);
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Constrói lista de ClientContracts para um usuário CLIENT
     */
    private java.util.List<ClientContractForUserResponse> buildClientContractsForUser(UUID userId) {
        java.util.List<Object[]> contractsData = userService.getClientContractsForUser(userId);
        return contractsData.stream()
                .map(data -> {
                    ClientContractForUserResponse response = new ClientContractForUserResponse();

                    // Criar objeto aninhado OrganizationDTO (consistente com metadata)
                    OrganizationDTO orgDto = new OrganizationDTO();
                    orgDto.setId((Long) data[0]); // organization_id
                    orgDto.setName((String) data[1]); // organization name
                    response.setOrganization(orgDto);

                    response.setIsPrimary((Boolean) data[2]);
                    response.setStatus(data[3] != null ? data[3].toString() : null);
                    response.setContractDate(data[4] != null ? data[4].toString() : null);
                    response.setStartDate(data[5] != null ? data[5].toString() : null);
                    response.setEndDate(data[6] != null ? data[6].toString() : null);
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // DTO para criação de usuário
    @Data
    @NoArgsConstructor
    public static class UserCreateRequest {
        private String username; // email (obrigatório)
        private String name; // nome completo (obrigatório)
        private String password; // senha (opcional - padrão: "12345678" para ADM, "senha123" para outros)
        private String role; // USER, COURIER, CLIENT, ADM, ADMIN (obrigatório)
        private String cpf; // CPF (obrigatório)
        private String phone;
        private String address;
        private Long cityId; // ID da cidade relacionada (aceita direto)
        private CityIdWrapper city; // Aceita também {"id": 1058}
        private String state;
        private String country;
        private String dateOfBirth; // ISO format: YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss.sssZ
        private String gender; // MALE, FEMALE, OTHER
        private Long organizationId; // ID da organização (opcional, aceita direto)
        private OrganizationIdWrapper organization; // Aceita também {"id": 6}
        private Boolean enabled = true; // default true

        // Método helper para obter o cityId de qualquer formato
        public Long getCityIdResolved() {
            if (cityId != null)
                return cityId;
            if (city != null && city.getId() != null)
                return city.getId();
            return null;
        }

        // Método helper para obter o organizationId de qualquer formato
        public Long getOrganizationIdResolved() {
            if (organizationId != null)
                return organizationId;
            if (organization != null && organization.getId() != null)
                return organization.getId();
            return null;
        }
    }

    // Wrapper para aceitar {"id": valor}
    @Data
    @NoArgsConstructor
    public static class CityIdWrapper {
        private Long id;
    }

    @Data
    @NoArgsConstructor
    public static class OrganizationIdWrapper {
        private Long id;
    }

    // DTO para atualização de usuário
    @Data
    @NoArgsConstructor
    public static class UserUpdateRequest {
        private String name;
        private String phone;
        private String address;
        private Long cityId; // ID da cidade relacionada
        private String state;
        private String country;
        private String birthDate; // Mapeia para "birthDate" do front-end
        private String gender; // M/F/OTHER
        private String cpf; // Mapeia para "cpf" do front-end
        private Double latitude; // Coordenadas do endereço fixo
        private Double longitude; // Coordenadas do endereço fixo
    }

    // DTO para atualização de localização
    @Data
    @NoArgsConstructor
    public static class LocationUpdateRequest {
        @jakarta.validation.constraints.NotNull(message = "Latitude é obrigatória")
        @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "Latitude deve estar entre -90 e 90")
        @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "Latitude deve estar entre -90 e 90")
        private Double latitude;

        @jakarta.validation.constraints.NotNull(message = "Longitude é obrigatória")
        @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "Longitude deve estar entre -180 e 180")
        @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "Longitude deve estar entre -180 e 180")
        private Double longitude;

        // Timestamp do GPS - se não fornecido, usa timestamp atual
        private String updatedAt; // ISO DateTime string (ex: "2025-10-31T15:30:45.123Z")
    }

    // DTO para resposta (evita problemas de lazy loading)
    @Data
    @NoArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String username;
        private String name;
        private String phone;
        private String address;
        private CityDTO city;
        private String state;
        private String country;
        private String dateOfBirth;
        private String gender;
        private String cpf;
        private String role;
        private OrganizationDTO organization;

        // Campos de localização do endereço fixo
        private Double latitude;
        private Double longitude;
        
        // Campos de localização GPS (em tempo real)
        private Double gpsLatitude;
        private Double gpsLongitude;
        private String updatedAt; // Timestamp da última atualização GPS

        // Contratos (apenas para COURIER e CLIENT)
        private java.util.List<EmploymentContractForUserResponse> employmentContracts;
        private java.util.List<ClientContractForUserResponse> clientContracts;

        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            this.phone = user.getPhone();
            this.address = user.getAddress();
            this.state = user.getState();
            this.country = user.getCountry();
            this.dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            this.gender = user.getGender() != null ? user.getGender().toString() : null;
            this.cpf = user.getCpfFormatted();
            this.role = user.getRole() != null ? user.getRole().toString() : null;

            // Campos de localização do endereço fixo
            this.latitude = user.getLatitude();
            this.longitude = user.getLongitude();
            
            // Campos de localização GPS (em tempo real)
            this.gpsLatitude = user.getGpsLatitude();
            this.gpsLongitude = user.getGpsLongitude();
            this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;

            // Carregar dados da cidade como objeto usando DTOMapper
            this.city = DTOMapper.toDTO(user.getCity());

            // Carregar dados da organização como objeto usando DTOMapper
            this.organization = DTOMapper.toDTO(user.getOrganization());

            // Inicializar listas vazias (serão preenchidas no controller se necessário)
            this.employmentContracts = new java.util.ArrayList<>();
            this.clientContracts = new java.util.ArrayList<>();
        }
    }

    /**
     * DTO para EmploymentContract na perspectiva do COURIER
     * Mostra em quais organizações o motoboy trabalha
     */
    @Data
    @NoArgsConstructor
    public static class EmploymentContractForUserResponse {
        private OrganizationDTO organization; // Objeto aninhado consistente com metadata
        private String linkedAt;
        private Boolean isActive;
    }

    /**
     * DTO para ServiceContract na perspectiva do CLIENT
     * Mostra os contratos de serviço do cliente
     */
    @Data
    @NoArgsConstructor
    public static class ClientContractForUserResponse {
        private OrganizationDTO organization; // Objeto aninhado consistente com metadata
        private Boolean isPrimary;
        private String status;
        private String contractDate;
        private String startDate;
        private String endDate;
    }
}