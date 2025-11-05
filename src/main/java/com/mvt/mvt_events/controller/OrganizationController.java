package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.common.CityDTO;
import com.mvt.mvt_events.dto.mapper.DTOMapper;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizações", description = "Gerenciamento de organizações")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar organizações (paginado)", description = """
            Lista organizações com suporte a busca e filtros.

            **Filtros Disponíveis:**
            - `search` - Busca em nome, slug ou email (case-insensitive, parcial)
            - `active` - Filtrar por status ativo/inativo (true/false)

            **Paginação:**
            - `page` - Número da página (default: 0)
            - `size` - Tamanho da página (default: 20)
            - `sort` - Ordenação (ex: name,asc)

            **Exemplos:**
            ```
            /api/organizations?search=sport
            /api/organizations?active=true
            /api/organizations?search=club&active=true
            /api/organizations?sort=name,asc&size=50
            ```
            """)
    public Page<OrganizationResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {

        // Se houver algum filtro, usa o método com filtros
        Page<Organization> organizations;
        if (search != null || active != null) {
            organizations = service.listWithFilters(search, active, pageable);
        } else {
            organizations = service.list(pageable);
        }

        return organizations.map(OrganizationResponse::new);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar organização por ID")
    public OrganizationResponse get(@PathVariable Long id) {
        Organization organization = service.get(id);
        OrganizationResponse response = new OrganizationResponse(organization);

        // Carregar contratos de forma segura usando queries customizadas
        response.setEmploymentContracts(buildEmploymentContractsResponse(id));
        response.setClientContracts(buildServiceContractsResponse(id));

        return response;
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Buscar organização do usuário")
    public OrganizationResponse getByUserId(@PathVariable UUID userId) {
        Organization organization = service.getByUserId(userId);
        OrganizationResponse response = new OrganizationResponse(organization);

        // Carregar contratos de forma segura usando queries customizadas
        response.setEmploymentContracts(buildEmploymentContractsResponse(organization.getId()));
        response.setClientContracts(buildServiceContractsResponse(organization.getId()));

        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@RequestBody @Valid OrganizationCreateRequest request) {
        Organization organization = service.createWithUser(request);
        return new OrganizationResponse(organization);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable Long id, @RequestBody @Valid OrganizationUpdateRequest request) {
        Organization organization = service.update(id, request);
        return new OrganizationResponse(organization);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Constrói lista de EmploymentContractResponse a partir de dados brutos (sem
     * lazy loading)
     */
    private java.util.List<EmploymentContractResponse> buildEmploymentContractsResponse(Long organizationId) {
        java.util.List<Object[]> contractsData = service.getEmploymentContractsData(organizationId);
        return contractsData.stream()
                .map(data -> {
                    EmploymentContractResponse response = new EmploymentContractResponse();
                    response.setCourier(data[0] != null ? data[0].toString() : null); // UUID
                    response.setLinkedAt(data[1] != null ? data[1].toString() : null); // LocalDateTime
                    response.setIsActive((Boolean) data[2]); // boolean
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Constrói lista de ClientContractResponse a partir de dados brutos (sem lazy
     * loading)
     */
    private java.util.List<ClientContractResponse> buildServiceContractsResponse(Long organizationId) {
        java.util.List<Object[]> contractsData = service.getServiceContractsData(organizationId);
        return contractsData.stream()
                .map(data -> {
                    ClientContractResponse response = new ClientContractResponse();
                    response.setClient(data[0] != null ? data[0].toString() : null); // UUID
                    response.setContractNumber((String) data[1]); // String
                    response.setIsPrimary((Boolean) data[2]); // boolean
                    response.setStatus(data[3] != null ? data[3].toString() : null); // ContractStatus enum
                    response.setContractDate(data[4] != null ? data[4].toString() : null); // LocalDate
                    response.setStartDate(data[5] != null ? data[5].toString() : null); // LocalDate
                    response.setEndDate(data[6] != null ? data[6].toString() : null); // LocalDate
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * DTO for creating organization with user assignment
     */
    @Data
    public static class OrganizationCreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        private String name;

        @Size(max = 100, message = "Slug deve ter no máximo 100 caracteres")
        private String slug;

        @NotBlank(message = "Email de contato é obrigatório")
        @Email(message = "Email de contato deve ser válido")
        private String contactEmail;

        private String phone;
        private String website;
        private String description;
        private String logoUrl;

        // ID do usuário Gerente ADM que será vinculado à organização
        private UUID userId;
    }

    /**
     * DTO for updating organization
     */
    @Data
    @NoArgsConstructor
    public static class OrganizationUpdateRequest {
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        private String name;

        @Size(max = 100, message = "Slug deve ter no máximo 100 caracteres")
        private String slug;

        @Email(message = "Email de contato deve ser válido")
        private String contactEmail;

        private String phone;
        private String website;
        private String description;
        private String logoUrl;

        private Long cityId; // Aceita cityId direto
        private CityIdWrapper city; // Aceita também {"id": 1068}

        private String status;
        private BigDecimal commissionPercentage;

        // Relacionamentos de contratos
        private java.util.List<EmploymentContractRequest> employmentContracts;
        private java.util.List<ContractRequest> serviceContracts;

        // Método helper para obter o cityId de qualquer formato
        public Long getCityIdResolved() {
            if (cityId != null)
                return cityId;
            if (city != null && city.getId() != null)
                return city.getId();
            return null;
        }
    }

    /**
     * DTO para EmploymentContract
     */
    @Data
    @NoArgsConstructor
    public static class EmploymentContractRequest {
        private String courier; // UUID como string
        private String linkedAt; // ISO DateTime string
        private Boolean isActive;
    }

    /**
     * DTO para Contract (serviceContracts)
     */
    @Data
    @NoArgsConstructor
    public static class ContractRequest {
        private String client; // UUID como string
        private String contractNumber;
        private Boolean isPrimary;
        private String status; // ACTIVE, SUSPENDED, CANCELLED
        private String contractDate; // ISO Date string
        private String startDate; // ISO DateTime string
        private String endDate; // ISO DateTime string (pode ser vazio)
    }

    /**
     * Wrapper para aceitar {"id": valor}
     */
    @Data
    @NoArgsConstructor
    public static class CityIdWrapper {
        private Long id;
    }

    /**
     * DTO para resposta de organização (evita problemas de lazy loading)
     */
    @Data
    @NoArgsConstructor
    public static class OrganizationResponse {
        private Long id;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String name;
        private String slug;
        private String contactEmail;
        private String phone;
        private String website;
        private String description;
        private String logoUrl;
        private CityDTO city;
        private BigDecimal commissionPercentage;
        private String status;

        // Relacionamentos de contratos
        private java.util.List<EmploymentContractResponse> employmentContracts;
        private java.util.List<ClientContractResponse> clientContracts;

        public OrganizationResponse(Organization organization) {
            this.id = organization.getId();
            this.createdAt = organization.getCreatedAt();
            this.updatedAt = organization.getUpdatedAt();
            this.name = organization.getName();
            this.slug = organization.getSlug();
            this.contactEmail = organization.getContactEmail();
            this.phone = organization.getPhone();
            this.website = organization.getWebsite();
            this.description = organization.getDescription();
            this.logoUrl = organization.getLogoUrl();
            this.commissionPercentage = organization.getCommissionPercentage();
            this.status = organization.getStatus() != null ? organization.getStatus().toString() : null;

            // Carregar dados da cidade como objeto usando DTOMapper
            this.city = DTOMapper.toDTO(organization.getCity());

            // Carregar contratos apenas se estiverem inicializados
            // IMPORTANTE: NÃO acessar User dentro do contrato para evitar lazy loading e
            // StackOverflow
            this.employmentContracts = new java.util.ArrayList<>();
            this.clientContracts = new java.util.ArrayList<>();

            if (org.hibernate.Hibernate.isInitialized(organization.getEmploymentContracts()) &&
                    organization.getEmploymentContracts() != null) {
                this.employmentContracts = new java.util.ArrayList<>(organization.getEmploymentContracts()).stream()
                        .map(ec -> {
                            EmploymentContractResponse response = new EmploymentContractResponse();
                            // Extrair ID do proxy sem inicializar usando Hibernate API
                            if (ec.getCourier() != null) {
                                Object courierId = org.hibernate.Hibernate.unproxy(ec.getCourier());
                                if (courierId instanceof com.mvt.mvt_events.jpa.User) {
                                    response.setCourier(((com.mvt.mvt_events.jpa.User) courierId).getId().toString());
                                }
                            }
                            response.setLinkedAt(ec.getLinkedAt() != null ? ec.getLinkedAt().toString() : null);
                            response.setIsActive(ec.isActive());
                            return response;
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                this.employmentContracts = new java.util.ArrayList<>();
            }

            if (org.hibernate.Hibernate.isInitialized(organization.getClientContracts()) &&
                    organization.getClientContracts() != null) {
                this.clientContracts = new java.util.ArrayList<>(organization.getClientContracts()).stream()
                        .map(sc -> {
                            ClientContractResponse response = new ClientContractResponse();
                            // Extrair ID do proxy sem inicializar usando Hibernate API
                            if (sc.getClient() != null) {
                                Object clientId = org.hibernate.Hibernate.unproxy(sc.getClient());
                                if (clientId instanceof com.mvt.mvt_events.jpa.User) {
                                    response.setClient(((com.mvt.mvt_events.jpa.User) clientId).getId().toString());
                                }
                            }
                            response.setContractNumber(sc.getContractNumber());
                            response.setIsPrimary(sc.isPrimary());
                            response.setStatus(sc.getStatus() != null ? sc.getStatus().toString() : null);
                            response.setContractDate(
                                    sc.getContractDate() != null ? sc.getContractDate().toString() : null);
                            response.setStartDate(sc.getStartDate() != null ? sc.getStartDate().toString() : null);
                            response.setEndDate(sc.getEndDate() != null ? sc.getEndDate().toString() : null);
                            return response;
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                this.clientContracts = new java.util.ArrayList<>();
            }
        }
    }

    /**
     * DTO para resposta de EmploymentContract (sem organização para evitar circular
     * reference)
     */
    @Data
    @NoArgsConstructor
    public static class EmploymentContractResponse {
        private String courier; // UUID do motoboy
        private String linkedAt;
        private Boolean isActive;

        public EmploymentContractResponse(com.mvt.mvt_events.jpa.EmploymentContract contract) {
            // Acessa apenas ID sem inicializar lazy loading
            if (contract.getCourier() != null) {
                this.courier = contract.getCourier().getId().toString();
            }
            this.linkedAt = contract.getLinkedAt().toString();
            this.isActive = contract.isActive();
        }
    }

    /**
     * DTO para resposta de ClientContract (sem organização para evitar circular
     * reference)
     */
    @Data
    @NoArgsConstructor
    public static class ClientContractResponse {
        private String client; // UUID do cliente
        private String contractNumber;
        private Boolean isPrimary;
        private String status;
        private String contractDate;
        private String startDate;
        private String endDate;

        public ClientContractResponse(com.mvt.mvt_events.jpa.ClientContract contract) {
            // Acessa apenas ID sem inicializar lazy loading
            if (contract.getClient() != null) {
                this.client = contract.getClient().getId().toString();
            }
            this.contractNumber = contract.getContractNumber();
            this.isPrimary = contract.isPrimary();
            this.status = contract.getStatus() != null ? contract.getStatus().toString() : null;
            this.contractDate = contract.getContractDate() != null ? contract.getContractDate().toString() : null;
            this.startDate = contract.getStartDate() != null ? contract.getStartDate().toString() : null;
            this.endDate = contract.getEndDate() != null ? contract.getEndDate().toString() : null;
        }
    }
}