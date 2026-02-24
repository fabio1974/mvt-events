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

import java.util.List;
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

    @GetMapping("/me/activation-status")
    @Operation(summary = "Verificar status de ativação do usuário logado", 
               description = "Retorna o status de ativação em tempo real, detalhando exatamente o que está faltando " +
                           "para o usuário estar completamente habilitado no sistema. " +
                           "COURIER precisa: veículo, conta bancária, telefone. " +
                           "CUSTOMER precisa: meio de pagamento, telefone.")
    @Transactional(readOnly = true)
    public com.mvt.mvt_events.dto.ActivationStatusResponse getActivationStatus(Authentication authentication) {
        return userService.getActivationStatus(authentication);
    }

    @GetMapping("/search/couriers")
    @Operation(summary = "Buscar motoboys para typeahead", 
               description = "Busca motoboys por parte do nome ou email para adicionar ao grupo. " +
                           "Retorna lista leve e limitada para uso em typeahead mobile. " +
                           "Exclui motoboys que já estão vinculados à organização do usuário logado.")
    @Transactional(readOnly = true)
    public List<CourierSearchResponse> searchCouriers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            Authentication authentication) {
        return userService.searchCouriersForTypeahead(search, limit, authentication);
    }

    @GetMapping("/my-couriers")
    @Operation(summary = "Listar motoboys da minha organização", 
               description = "Retorna todos os motoboys vinculados à organização do usuário logado (ORGANIZER). " +
                           "Inclui informações básicas do motoboy e status do contrato de trabalho.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CourierForOrganizerResponse>> listMyCouriers(Authentication authentication) {
        List<CourierForOrganizerResponse> couriers = userService.getCouriersForLoggedOrganizer(authentication);
        return ResponseEntity.ok(couriers);
    }

    @PostMapping("/my-couriers/{courierId}")
    @Operation(summary = "Adicionar motoboy ao meu grupo", 
               description = "Vincula um motoboy à organização do usuário logado (ORGANIZER). " +
                           "Cria um contrato de trabalho (EmploymentContract) ativo.")
    public ResponseEntity<?> addCourierToMyGroup(
            @PathVariable UUID courierId,
            Authentication authentication) {
        try {
            CourierForOrganizerResponse response = userService.addCourierToOrganization(courierId, authentication);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/my-couriers/{courierId}")
    @Operation(summary = "Remover motoboy do meu grupo", 
               description = "Remove o vínculo de um motoboy com a organização do usuário logado (ORGANIZER). " +
                           "Desativa o contrato de trabalho (EmploymentContract).")
    public ResponseEntity<?> removeCourierFromMyGroup(
            @PathVariable UUID courierId,
            Authentication authentication) {
        try {
            userService.removeCourierFromOrganization(courierId, authentication);
            return ResponseEntity.ok(java.util.Map.of("message", "Motoboy removido do grupo com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // CLIENT GROUP MANAGEMENT (my-clients)
    // ============================================================================

    @GetMapping("/search/clients")
    @Operation(summary = "Buscar clientes para adicionar ao grupo (typeahead)",
               description = "Busca clientes por parte do nome ou email para adicionar ao grupo. " +
                           "Retorna lista leve e limitada para uso em typeahead mobile. " +
                           "Exclui clientes que já estão vinculados à organização do usuário logado.")
    @Transactional(readOnly = true)
    public List<ClientSearchResponse> searchClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            Authentication authentication) {
        return userService.searchClientsForTypeahead(search, limit, authentication);
    }

    @GetMapping("/my-clients")
    @Operation(summary = "Listar clientes da minha organização", 
               description = "Retorna todos os clientes vinculados à organização do usuário logado (ORGANIZER). " +
                           "Inclui informações básicas do cliente e status do contrato de serviço.")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ClientForOrganizerResponse>> listMyClients(Authentication authentication) {
        List<ClientForOrganizerResponse> clients = userService.getClientsForLoggedOrganizer(authentication);
        return ResponseEntity.ok(clients);
    }

    @PostMapping("/my-clients/{clientId}")
    @Operation(summary = "Adicionar cliente ao meu grupo", 
               description = "Vincula um cliente à organização do usuário logado (ORGANIZER). " +
                           "Cria um contrato de serviço (ClientContract) ativo.")
    public ResponseEntity<?> addClientToMyGroup(
            @PathVariable UUID clientId,
            Authentication authentication) {
        try {
            ClientForOrganizerResponse response = userService.addClientToOrganization(clientId, authentication);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/my-clients/{clientId}")
    @Operation(summary = "Remover cliente do meu grupo", 
               description = "Remove o vínculo de um cliente com a organização do usuário logado (ORGANIZER). " +
                           "Deleta o contrato de serviço (ClientContract).")
    public ResponseEntity<?> removeClientFromMyGroup(
            @PathVariable UUID clientId,
            Authentication authentication) {
        try {
            userService.removeClientFromOrganization(clientId, authentication);
            return ResponseEntity.ok(java.util.Map.of("message", "Cliente removido do grupo com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public UserResponse get(@PathVariable UUID id) {
        User user = userService.findById(id);
        UserResponse response = new UserResponse(user);

        // Carregar contratos de trabalho (se for COURIER)
        if (user.getRole() == User.Role.COURIER) {
            response.setEmploymentContracts(buildEmploymentContractsForUser(id));
        }

        // Carregar contratos de serviço (se for CLIENT ou CUSTOMER)
        if (user.getRole() == User.Role.CLIENT || user.getRole() == User.Role.CUSTOMER) {
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
    @Operation(summary = "Atualizar localização do usuário", description = "Atualiza gpsLatitude, gpsLongitude e timestamp do usuário")
    public ResponseEntity<UserResponse> updateLocation(
            @PathVariable UUID id,
            @RequestBody @Valid LocationUpdateRequest request,
            Authentication authentication) {
        User updatedUser = userService.updateUserLocation(id, request.getGpsLatitude(), request.getGpsLongitude(),
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
                    response.setStartDate(data[4] != null ? data[4].toString() : null);
                    response.setEndDate(data[5] != null ? data[5].toString() : null);
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
        private String cpf; // CPF (deprecated - use documentNumber)
        private String documentNumber; // CPF ou CNPJ (obrigatório)
        private String phone; // Deprecated - use phoneDdd + phoneNumber
        private String phoneDdd; // DDD do telefone (2 dígitos)
        private String phoneNumber; // Número sem DDD (8 ou 9 dígitos)
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

    // DTO for address data in update requests
    @Data
    @NoArgsConstructor
    public static class AddressDTO {
        private Long id; // Se presente, é update; se null, é insert
        private String street;
        private String number;
        private String complement;
        private String neighborhood;
        private AddressCityDTO city;    // Aceita objeto city com id
        private String state;
        private String zipCode;
        private String referencePoint;
        private Double latitude;        // Aceita Double
        private Double longitude;       // Aceita Double
        private Boolean isDefault;
        
        // Helper para obter o cityId
        public Long getCityId() {
            return city != null ? city.getId() : null;
        }
        
        // Helper para obter o nome da cidade
        public String getCityName() {
            return city != null ? city.getName() : null;
        }
        
        // Helper para obter o estado da cidade
        public String getCityState() {
            return city != null ? city.getState() : null;
        }
    }
    
    // DTO para City nos requests de Address (evita conflito com CityDTO importado)
    @Data
    @NoArgsConstructor
    public static class AddressCityDTO {
        private Long id;
        private String name;
        private String state;
        private String stateCode;
    }

    // DTO para atualização de usuário
    @Data
    @NoArgsConstructor
    public static class UserUpdateRequest {
        // Identificação
        private String username;           // Email (unique)
        private String name;               // Nome completo
        
        // Dados pessoais
        private String documentNumber;     // CPF ou CNPJ
        private String dateOfBirth;        // Data de nascimento (ISO: YYYY-MM-DD)
        private String gender;             // MALE, FEMALE, OTHER
        
        // Telefone (para KYC Pagar.me)
        private String phoneDdd;           // DDD (2 dígitos)
        private String phoneNumber;        // Número sem DDD (8-9 dígitos)
        
        // Endereços
        private List<AddressDTO> addresses; // Array de endereços
        
        // Localização GPS em tempo real (rastreamento)
        private Double gpsLatitude;        // Latitude GPS em tempo real
        private Double gpsLongitude;       // Longitude GPS em tempo real

        // Tipo de serviço (apenas para COURIER): DELIVERY, PASSENGER_TRANSPORT, BOTH
        private String serviceType;
    }

    // DTO para atualização de localização
    @Data
    @NoArgsConstructor
    public static class LocationUpdateRequest {
        @jakarta.validation.constraints.NotNull(message = "GPS Latitude é obrigatória")
        @jakarta.validation.constraints.DecimalMin(value = "-90.0", message = "GPS Latitude deve estar entre -90 e 90")
        @jakarta.validation.constraints.DecimalMax(value = "90.0", message = "GPS Latitude deve estar entre -90 e 90")
        private Double gpsLatitude;

        @jakarta.validation.constraints.NotNull(message = "GPS Longitude é obrigatória")
        @jakarta.validation.constraints.DecimalMin(value = "-180.0", message = "GPS Longitude deve estar entre -180 e 180")
        @jakarta.validation.constraints.DecimalMax(value = "180.0", message = "GPS Longitude deve estar entre -180 e 180")
        private Double gpsLongitude;

        // Timestamp do GPS - se não fornecido, usa timestamp atual
        private String updatedAt; // ISO DateTime string (ex: "2025-10-31T15:30:45.123Z")
    }

    // DTO para Address na resposta do usuário
    @Data
    @NoArgsConstructor
    public static class AddressResponseDTO {
        private Long id;
        private String street;
        private String number;
        private String complement;
        private String neighborhood;
        private String city;
        private String state;
        private String zipCode;
        private String referencePoint;
        private Double latitude;
        private Double longitude;
        private Boolean isDefault;
        private String fullAddress;

        public AddressResponseDTO(com.mvt.mvt_events.jpa.Address address) {
            this.id = address.getId();
            this.street = address.getStreet();
            this.number = address.getNumber();
            this.complement = address.getComplement();
            this.neighborhood = address.getNeighborhood();
            this.city = address.getCityName();
            this.state = address.getState();
            this.zipCode = address.getZipCode();
            this.referencePoint = address.getReferencePoint();
            this.latitude = address.getLatitude();
            this.longitude = address.getLongitude();
            this.isDefault = address.getIsDefault();
            this.fullAddress = address.getFullAddress();
        }
    }

    // DTO para resposta (evita problemas de lazy loading)
    @Data
    @NoArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String username;
        private String name;
        private String phoneDdd; // DDD do telefone (2 dígitos)
        private String phoneNumber; // Número do telefone sem DDD (8 ou 9 dígitos)
        private String address; // Deprecated - use addresses array
        private List<AddressResponseDTO> addresses; // Lista de endereços com latitude/longitude
        private CityDTO city;
        private String state;
        private String country;
        private String dateOfBirth;
        private String gender;
        private String documentNumber; // CPF ou CNPJ formatado
        private String role;
        private String serviceType; // Tipo de serviço (apenas para COURIER): DELIVERY, PASSENGER_TRANSPORT, BOTH
        private OrganizationDTO organization;

        // Campos de localização GPS em tempo real (rastreamento do usuário)
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
            this.phoneDdd = user.getPhoneDdd();
            this.phoneNumber = user.getPhoneNumber();
            
            // Address from Address entity (deprecated - use addresses array)
            if (user.getAddress() != null) {
                this.address = user.getAddress().getFullAddress();
            }
            
            // Populate addresses array (contém latitude/longitude do endereço fixo)
            if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
                this.addresses = user.getAddresses().stream()
                    .map(AddressResponseDTO::new)
                    .collect(java.util.stream.Collectors.toList());
            } else {
                this.addresses = new java.util.ArrayList<>();
            }
            
            this.dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            this.gender = user.getGender() != null ? user.getGender().toString() : null;
            this.documentNumber = user.getDocumentFormatted();
            this.role = user.getRole() != null ? user.getRole().toString() : null;
            this.serviceType = user.getServiceType() != null ? user.getServiceType().name() : null;

            // Campos de localização GPS (em tempo real) - rastreamento do usuário
            this.gpsLatitude = user.getGpsLatitude();
            this.gpsLongitude = user.getGpsLongitude();
            this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;

            // Carregar dados da cidade via Address usando DTOMapper
            this.city = user.getAddress() != null ? DTOMapper.toDTO(user.getAddress().getCity()) : null;

            // Organization is now accessed through the reverse relationship (Organization.owner)
            // Will be populated separately if needed
            this.organization = null;

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
        private String startDate;
        private String endDate;
    }

    /**
     * DTO leve para busca de motoboys em typeahead (mobile)
     * Retorna apenas campos essenciais para exibição em lista de seleção
     */
    @Data
    @NoArgsConstructor
    public static class CourierSearchResponse {
        private UUID id;
        private String name;
        private String email;
        private String phone; // Formato: (DDD) XXXXX-XXXX

        public CourierSearchResponse(User user) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getUsername();
            // Formata telefone para exibição
            if (user.getPhoneDdd() != null && user.getPhoneNumber() != null) {
                this.phone = "(" + user.getPhoneDdd() + ") " + user.getPhoneNumber();
            }
        }
    }

    /**
     * DTO para listagem de motoboys vinculados à organização do ORGANIZER
     * Retorna dados completos do motoboy + status do contrato
     */
    @Data
    @NoArgsConstructor
    public static class CourierForOrganizerResponse {
        private UUID id;
        private String name;
        private String email;
        private String phone;           // Formato: (DDD) XXXXX-XXXX
        private String documentNumber;  // CPF formatado
        private String dateOfBirth;
        private String gender;
        
        // Localização GPS em tempo real
        private Double gpsLatitude;
        private Double gpsLongitude;
        private String lastLocationUpdate;  // Timestamp da última atualização
        
        // Status do contrato
        private Boolean isActive;
        private String linkedAt;         // Data de vínculo com a organização
        
        // Status Pagar.me
        private String pagarmeStatus;

        public CourierForOrganizerResponse(User courier, com.mvt.mvt_events.jpa.EmploymentContract contract) {
            this.id = courier.getId();
            this.name = courier.getName();
            this.email = courier.getUsername();
            
            // Formata telefone
            if (courier.getPhoneDdd() != null && courier.getPhoneNumber() != null) {
                this.phone = "(" + courier.getPhoneDdd() + ") " + courier.getPhoneNumber();
            }
            
            this.documentNumber = courier.getDocumentFormatted();
            this.dateOfBirth = courier.getDateOfBirth() != null ? courier.getDateOfBirth().toString() : null;
            this.gender = courier.getGender() != null ? courier.getGender().toString() : null;
            
            // GPS em tempo real
            this.gpsLatitude = courier.getGpsLatitude();
            this.gpsLongitude = courier.getGpsLongitude();
            this.lastLocationUpdate = courier.getUpdatedAt() != null ? courier.getUpdatedAt().toString() : null;
            
            // Status Pagar.me
            this.pagarmeStatus = courier.getPagarmeStatus();
            
            // Dados do contrato
            if (contract != null) {
                this.isActive = contract.isActive();
                this.linkedAt = contract.getLinkedAt() != null ? contract.getLinkedAt().toString() : null;
            }
        }
    }

    // ============================================================================
    // CLIENT GROUP DTOs
    // ============================================================================

    /**
     * DTO para busca de clientes (typeahead) - dados mínimos
     */
    @Data
    @NoArgsConstructor
    public static class ClientSearchResponse {
        private UUID id;
        private String name;
        private String email;
        private String phone;

        public ClientSearchResponse(User user) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getUsername();
            
            // Formata telefone para exibição
            if (user.getPhoneDdd() != null && user.getPhoneNumber() != null) {
                this.phone = "(" + user.getPhoneDdd() + ") " + user.getPhoneNumber();
            }
        }
    }

    /**
     * DTO para listagem de clientes vinculados à organização do ORGANIZER
     * Retorna dados completos do cliente + status do contrato
     */
    @Data
    @NoArgsConstructor
    public static class ClientForOrganizerResponse {
        private UUID id;
        private String name;
        private String email;
        private String phone;           // Formato: (DDD) XXXXX-XXXX
        private String documentNumber;  // CPF formatado
        private String dateOfBirth;
        private String gender;
        
        // Status do contrato
        private String contractStatus;  // ACTIVE, SUSPENDED, CANCELLED
        private Boolean isPrimary;      // Se é contrato titular
        private String startDate;       // Data de início do contrato
        private String endDate;         // Data de fim do contrato (opcional)

        public ClientForOrganizerResponse(User client, com.mvt.mvt_events.jpa.ClientContract contract) {
            this.id = client.getId();
            this.name = client.getName();
            this.email = client.getUsername();
            
            // Formata telefone
            if (client.getPhoneDdd() != null && client.getPhoneNumber() != null) {
                this.phone = "(" + client.getPhoneDdd() + ") " + client.getPhoneNumber();
            }
            
            this.documentNumber = client.getDocumentFormatted();
            this.dateOfBirth = client.getDateOfBirth() != null ? client.getDateOfBirth().toString() : null;
            this.gender = client.getGender() != null ? client.getGender().toString() : null;
            
            // Dados do contrato
            if (contract != null) {
                this.contractStatus = contract.getStatus() != null ? contract.getStatus().toString() : null;
                this.isPrimary = contract.isPrimary();
                this.startDate = contract.getStartDate() != null ? contract.getStartDate().toString() : null;
                this.endDate = contract.getEndDate() != null ? contract.getEndDate().toString() : null;
            }
        }
    }
}