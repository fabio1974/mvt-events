package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.OrganizationController.OrganizationCreateRequest;
import com.mvt.mvt_events.controller.OrganizationController.OrganizationUpdateRequest;
import com.mvt.mvt_events.controller.OrganizationController.EmploymentContractRequest;
import com.mvt.mvt_events.controller.OrganizationController.ContractRequest;
import com.mvt.mvt_events.jpa.*;
import com.mvt.mvt_events.repository.*;
import com.mvt.mvt_events.specification.OrganizationSpecification;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final EmploymentContractRepository employmentContractRepository;
    private final ClientContractRepository clientContractRepository;

    public OrganizationService(OrganizationRepository repository, UserRepository userRepository,
            CityRepository cityRepository, EmploymentContractRepository employmentContractRepository,
            ClientContractRepository clientContractRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.employmentContractRepository = employmentContractRepository;
        this.clientContractRepository = clientContractRepository;
    }

    /**
     * Create organization and assign to user in a single transaction
     */
    @Transactional
    public Organization createWithUser(OrganizationCreateRequest request) {
        // Create organization
        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setWebsite(request.getWebsite());
        organization.setDescription(request.getDescription());
        organization.setLogoUrl(request.getLogoUrl());

        // Generate slug from name if not provided
        if (request.getSlug() == null || request.getSlug().trim().isEmpty()) {
            organization.setSlug(generateUniqueSlug(request.getName()));
        } else {
            // Check if slug already exists
            if (repository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("Já existe uma organização com este slug");
            }
            organization.setSlug(request.getSlug());
        }

        // Save organization first
        Organization savedOrganization = repository.save(organization);

        // Set owner if userId is provided
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + request.getUserId()));

            // Verify user is an organizer
            if (user.getRole() != User.Role.ORGANIZER) {
                throw new RuntimeException("Apenas usuários com role ORGANIZER podem ser donos de uma organização");
            }

            savedOrganization.setOwner(user);
            savedOrganization = repository.save(savedOrganization);
        }

        return savedOrganization;
    }

    public Organization create(Organization organization) {
        // Generate slug from name if not provided
        if (organization.getSlug() == null || organization.getSlug().trim().isEmpty()) {
            organization.setSlug(generateUniqueSlug(organization.getName()));
        } else {
            // Check if slug already exists
            if (repository.existsBySlug(organization.getSlug())) {
                throw new RuntimeException("Já existe uma organização com este slug");
            }
        }

        return repository.save(organization);
    }

    public List<Organization> findAll() {
        return repository.findAll();
    }

    public Optional<Organization> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Organization> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Transactional
    public Organization update(Long id, OrganizationUpdateRequest request) {
        Organization existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        // Validate slug if changed
        if (request.getSlug() != null && !request.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlugAndIdNot(request.getSlug(), id)) {
                throw new RuntimeException("Já existe uma organização com este slug");
            }
            existing.setSlug(request.getSlug());
        }

        // Update fields
        if (request.getName() != null)
            existing.setName(request.getName());
        if (request.getWebsite() != null)
            existing.setWebsite(request.getWebsite());
        if (request.getDescription() != null)
            existing.setDescription(request.getDescription());
        if (request.getLogoUrl() != null)
            existing.setLogoUrl(request.getLogoUrl());

        // Update status if provided
        if (request.getStatus() != null) {
            try {
                existing.setStatus(OrganizationStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Status inválido: " + request.getStatus());
            }
        }

        // Update commission percentage if provided
        if (request.getCommissionPercentage() != null) {
            existing.setCommissionPercentage(request.getCommissionPercentage());
        }

        // Update owner if provided
        if (request.getOwnerId() != null) {
            try {
                UUID ownerId = UUID.fromString(request.getOwnerId());
                User owner = userRepository.findById(ownerId)
                        .orElseThrow(() -> new RuntimeException("Proprietário não encontrado: " + ownerId));
                existing.setOwner(owner);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("ID de proprietário inválido: " + request.getOwnerId());
            }
        }

        // Process Employment Contracts (Contratos Motoboy)
        if (request.getEmploymentContracts() != null) {
            processEmploymentContracts(existing, request.getEmploymentContracts());
        }

        // Process Client Contracts (Contratos de Cliente)
        if (request.getClientContracts() != null) {
            processClientContracts(existing, request.getClientContracts());
        }

        // Salvar organização
        Organization saved = repository.save(existing);

        // Forçar flush e retornar organização limpa (sem relacionamentos carregados)
        repository.flush();

        return saved;
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Organização não encontrada");
        }
        repository.deleteById(id);
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = generateSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (repository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    @Transactional
    private void processEmploymentContracts(Organization organization,
            List<EmploymentContractRequest> contractRequests) {
        // Clear existing contracts
        employmentContractRepository.deleteAllByOrganization(organization);
        organization.getEmploymentContracts().clear();

        // Add new contracts
        for (EmploymentContractRequest contractRequest : contractRequests) {
            try {
                UUID courierId = UUID.fromString(contractRequest.getCourierId());
                User courier = userRepository.findById(courierId)
                        .orElseThrow(() -> new RuntimeException("Motoboy não encontrado: " + courierId));

                EmploymentContract contract = new EmploymentContract();
                contract.setOrganization(organization);
                contract.setCourier(courier);

                // Parse linkedAt date
                if (contractRequest.getLinkedAt() != null) {
                    contract.setLinkedAt(LocalDateTime.parse(contractRequest.getLinkedAt()));
                } else {
                    contract.setLinkedAt(LocalDateTime.now());
                }

                contract.setActive(contractRequest.getIsActive() != null ? contractRequest.getIsActive() : true);

                // Salvar contrato (não adicionar à coleção da organização para evitar circular
                // reference)
                employmentContractRepository.save(contract);

            } catch (IllegalArgumentException e) {
                throw new RuntimeException("ID de motoboy inválido: " + contractRequest.getCourierId());
            }
        }
    }

    @Transactional
    private void processClientContracts(Organization organization, List<ContractRequest> contractRequests) {
        // Clear existing contracts
        clientContractRepository.deleteAllByOrganization(organization);
        organization.getClientContracts().clear();

        // Add new contracts
        for (ContractRequest contractRequest : contractRequests) {
            try {
                UUID clientId = UUID.fromString(contractRequest.getClientId());
                User client = userRepository.findById(clientId)
                        .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clientId));

                ClientContract contract = new ClientContract();
                contract.setOrganization(organization);
                contract.setClient(client);
                contract.setPrimary(contractRequest.getIsPrimary() != null ? contractRequest.getIsPrimary() : false);

                // Parse contract status
                if (contractRequest.getStatus() != null) {
                    try {
                        contract.setStatus(
                                ClientContract.ContractStatus.valueOf(contractRequest.getStatus().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        contract.setStatus(ClientContract.ContractStatus.ACTIVE); // Default
                    }
                } else {
                    contract.setStatus(ClientContract.ContractStatus.ACTIVE);
                }

                // Parse dates - handle ISO 8601 with timezone (2025-10-25T03:00:00.000Z)
                if (contractRequest.getContractDate() != null && !contractRequest.getContractDate().trim().isEmpty()) {
                    contract.setContractDate(parseToLocalDate(contractRequest.getContractDate()));
                }

                if (contractRequest.getStartDate() != null && !contractRequest.getStartDate().trim().isEmpty()) {
                    contract.setStartDate(parseToLocalDate(contractRequest.getStartDate()));
                }

                if (contractRequest.getEndDate() != null && !contractRequest.getEndDate().trim().isEmpty()) {
                    contract.setEndDate(parseToLocalDate(contractRequest.getEndDate()));
                }

                // Salvar contrato (não adicionar à coleção da organização para evitar circular
                // reference)
                clientContractRepository.save(contract);

            } catch (IllegalArgumentException e) {
                throw new RuntimeException("ID de cliente inválido: " + contractRequest.getClientId());
            }
        }
    }

    // Legacy methods for compatibility
    public List<Organization> list() {
        return findAll();
    }

    /**
     * Lista organizações com paginação e busca
     */
    @Transactional(readOnly = true)
    public Page<Organization> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Lista organizações com filtros dinâmicos
     */
    @Transactional(readOnly = true)
    public Page<Organization> listWithFilters(String search, Boolean active, Pageable pageable) {
        Specification<Organization> spec = OrganizationSpecification.withFilters(search, active);
        return repository.findAll(spec, pageable);
    }

    /**
     * Lista organizações aplicando filtro de tenant baseado no role do usuário
     * - ADMIN: vê todas as organizações
     * - ORGANIZER: vê apenas sua própria organização
     * - Outros roles: lista vazia
     */
    @Transactional(readOnly = true)
    public Page<Organization> listWithTenantFilter(String search, Boolean active, Pageable pageable,
            org.springframework.security.core.Authentication authentication) {
        
        // Extrair email do usuário autenticado
        String userEmail = authentication.getName();
        
        // Buscar usuário SEM carregar relacionamentos (evita lazy loading)
        User user = userRepository.findByUsernameWithoutRelations(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userEmail));
        
        Page<Organization> result;
        
        // ADMIN: sem filtro de tenant, vê tudo
        if (user.getRole() == User.Role.ADMIN) {
            Specification<Organization> spec = OrganizationSpecification.withFilters(search, active);
            result = repository.findAll(spec, pageable);
        }
        // ORGANIZER: filtra apenas sua organização
        else if (user.getRole() == User.Role.ORGANIZER) {
            // Buscar organização onde o usuário é owner
            Optional<Organization> orgOpt = repository.findByOwner(user);
            
            if (orgOpt.isEmpty()) {
                // ORGANIZER sem organização: retorna página vazia
                return Page.empty(pageable);
            }
            
            // Adicionar filtro por organization_id
            Specification<Organization> spec = OrganizationSpecification.withFilters(search, active)
                    .and(OrganizationSpecification.byOwnerId(orgOpt.get().getId()));
            result = repository.findAll(spec, pageable);
        }
        // Outros roles (COURIER, CLIENT): retornam lista vazia
        else {
            return Page.empty(pageable);
        }
        
        // Inicializar owner para evitar lazy loading no controller
        result.getContent().forEach(org -> {
            if (org.getOwner() != null) {
                Hibernate.initialize(org.getOwner());
            }
        });
        
        return result;
    }

    @Transactional(readOnly = true)
    public Organization get(Long id) {
        // Carregar organização SEM os contratos (evita JOIN FETCH que causa
        // StackOverflow)
        Organization organization = findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));

        // Forçar inicialização do owner
        if (organization.getOwner() != null) {
            Hibernate.initialize(organization.getOwner());
        }

        // NÃO inicializar os contratos aqui - eles serão carregados via queries
        // customizadas
        // no controller se necessário

        return organization;
    }

    /**
     * Get organization by user ID (where user is the owner)
     */
    @Transactional(readOnly = true)
    public Organization getByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));

        // Find organization where user is the owner
        Optional<Organization> organization = repository.findByOwner(user);
        
        if (organization.isEmpty()) {
            throw new RuntimeException("Usuário não é dono de nenhuma organização");
        }

        return organization.get();
    }

    /**
     * Helper method to parse date strings that may include timezone (ISO 8601)
     * Handles formats like:
     * - 2025-10-25
     * - 2025-10-25T03:00:00.000Z
     * - 2025-10-25T03:00:00
     */
    private LocalDate parseToLocalDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            // Try to parse as ZonedDateTime first (handles timezone 'Z')
            if (dateString.contains("T")) {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString);
                return zonedDateTime.toLocalDate();
            } else {
                // Simple date format
                return LocalDate.parse(dateString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Formato de data inválido: " + dateString
                    + ". Use formato ISO 8601 (ex: 2025-10-25 ou 2025-10-25T03:00:00.000Z)");
        }
    }

    /**
     * Carrega dados dos Employment Contracts SEM carregar objetos User completos
     * Retorna lista de Object[] com: [courier_id, linked_at, is_active]
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getEmploymentContractsData(Long organizationId) {
        return employmentContractRepository.findContractDataByOrganizationId(organizationId);
    }

    /**
     * Carrega dados dos Service Contracts SEM carregar objetos User completos
     * Retorna lista de Object[] com: [client_id, contract_number, is_primary,
     * status, contract_date, start_date, end_date]
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getClientContractsData(Long organizationId) {
        return clientContractRepository.findContractDataByOrganizationId(organizationId);
    }
}