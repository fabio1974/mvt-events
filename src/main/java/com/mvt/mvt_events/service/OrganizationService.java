package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
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
        if (organizationData.getSlug() != null)
            existing.setSlug(organizationData.getSlug());
        if (organizationData.getContactEmail() != null)
            existing.setContactEmail(organizationData.getContactEmail());
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

    public Organization get(Long id) {
        return findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found with id: " + id));
    }
}