package com.mvt.mvt_events.service;

import com.mvt.mvt_events.controller.OrganizationController.OrganizationCreateRequest;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.specification.OrganizationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /**
     * Create organization and assign to user in a single transaction
     */
    @Transactional
    public Organization createWithUser(OrganizationCreateRequest request) {
        // Create organization
        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setContactEmail(request.getContactEmail());
        organization.setPhone(request.getPhone());
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

        // Update user with organization if userId is provided
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + request.getUserId()));

            // Verify user is an organizer
            if (user.getRole() != User.Role.ORGANIZER) {
                throw new RuntimeException("Apenas usuários com role ORGANIZER podem ser vinculados a uma organização");
            }

            user.setOrganization(savedOrganization);
            userRepository.save(user);
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

    public Organization update(Long id, Organization organizationData) {
        Organization existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        // Validate slug if changed
        if (organizationData.getSlug() != null && !organizationData.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlugAndIdNot(organizationData.getSlug(), id)) {
                throw new RuntimeException("Já existe uma organização com este slug");
            }
        }

        // Update fields
        if (organizationData.getName() != null)
            existing.setName(organizationData.getName());
        if (organizationData.getPhone() != null)
            existing.setPhone(organizationData.getPhone());
        if (organizationData.getWebsite() != null)
            existing.setWebsite(organizationData.getWebsite());
        if (organizationData.getDescription() != null)
            existing.setDescription(organizationData.getDescription());
        if (organizationData.getLogoUrl() != null)
            existing.setLogoUrl(organizationData.getLogoUrl());

        return repository.save(existing);
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

    public Organization get(Long id) {
        return findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));
    }

    /**
     * Get organization by user ID
     */
    public Organization getByUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));

        if (user.getOrganization() == null) {
            throw new RuntimeException("Usuário não está vinculado a nenhuma organização");
        }

        return user.getOrganization();
    }
}