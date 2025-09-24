package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.EventCreateRequest;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.TransferFrequency;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    private final EventRepository repository;
    private final OrganizationRepository organizationRepository;

    public EventService(EventRepository repository, OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }

    public Event create(EventCreateRequest request) {
        System.out.println("EventCreateRequest organizationId: " + request.getOrganizationId());

        // Find organization
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        System.out.println("Found organization: " + organization.getId() + " - " + organization.getName());

        // Create event entity
        Event event = new Event();
        event.setOrganization(organization);

        System.out.println("Event organization set: "
                + (event.getOrganization() != null ? event.getOrganization().getId() : "NULL"));
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setEventType(request.getEventType());
        event.setEventDate(request.getEventDate());
        event.setEventTime(request.getEventTime());
        event.setLocation(request.getLocation());
        event.setAddress(request.getAddress());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setRegistrationOpen(request.getRegistrationOpen());
        event.setRegistrationStartDate(
                request.getRegistrationStartDate() != null ? request.getRegistrationStartDate().atStartOfDay() : null);
        event.setRegistrationEndDate(
                request.getRegistrationEndDate() != null ? request.getRegistrationEndDate().atTime(23, 59, 59) : null);
        event.setPrice(request.getPrice());
        event.setCurrency(request.getCurrency());
        event.setBannerUrl(request.getBannerUrl());
        event.setPlatformFeePercentage(request.getPlatformFeePercentage());
        event.setTermsAndConditions(request.getTermsAndConditions());
        event.setTransferFrequency(TransferFrequency.WEEKLY); // Default value
        event.setStatus(Event.EventStatus.DRAFT); // Default status

        // Set startsAt from eventDate and eventTime
        if (event.getEventDate() != null) {
            LocalTime time = event.getEventTime() != null ? event.getEventTime() : LocalTime.of(0, 0);
            event.setStartsAt(LocalDateTime.of(event.getEventDate(), time));
        }

        // Generate slug from name if not provided
        if (request.getSlug() == null || request.getSlug().trim().isEmpty()) {
            event.setSlug(generateUniqueSlug(event.getName()));
        } else {
            // Check if slug already exists
            if (repository.existsBySlug(request.getSlug())) {
                throw new RuntimeException("Já existe um evento com este slug");
            }
            event.setSlug(request.getSlug());
        }

        return repository.save(event);
    }

    public Event create(Event event) {
        // Handle organization - either from organization object or organizationId
        if (event.getOrganization() == null) {
            // For now, we need a way to get organizationId from the request
            // This is a temporary fix - ideally we should use a DTO
            throw new RuntimeException("Organização é obrigatória para criar um evento");
        }

        if (event.getOrganization().getId() != null) {
            Organization organization = organizationRepository.findById(event.getOrganization().getId())
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
            event.setOrganization(organization);
        } else {
            throw new RuntimeException("ID da organização é obrigatório");
        }

        // Set startsAt from eventDate and eventTime
        if (event.getEventDate() != null) {
            LocalTime time = event.getEventTime() != null ? event.getEventTime() : LocalTime.of(0, 0);
            event.setStartsAt(LocalDateTime.of(event.getEventDate(), time));
        }

        // Generate slug from name if not provided
        if (event.getSlug() == null || event.getSlug().trim().isEmpty()) {
            event.setSlug(generateUniqueSlug(event.getName()));
        } else {
            // Check if slug already exists
            if (repository.existsBySlug(event.getSlug())) {
                throw new RuntimeException("Já existe um evento com este slug");
            }
        }

        return repository.save(event);
    }

    public List<Event> findAll() {
        return repository.findAll();
    }

    public Optional<Event> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Event> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    public List<Event> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId);
    }

    public List<Event> findPublishedEvents() {
        return repository.findByStatus(Event.EventStatus.PUBLISHED);
    }

    public Event findPublishedEventById(Long id) {
        return repository.findById(id)
                .filter(event -> event.getStatus() == Event.EventStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Evento público não encontrado"));
    }

    public Event publishEvent(Long id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        event.setStatus(Event.EventStatus.PUBLISHED);
        return repository.save(event);
    }

    public Event update(Long id, Event eventData) {
        Event existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        // Validate organization if changed
        if (eventData.getOrganization() != null && eventData.getOrganization().getId() != null) {
            Organization organization = organizationRepository.findById(eventData.getOrganization().getId())
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
            existing.setOrganization(organization);
        }

        // Validate slug if changed
        if (eventData.getSlug() != null && !eventData.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlugAndIdNot(eventData.getSlug(), id)) {
                throw new RuntimeException("Já existe um evento com este slug");
            }
        }

        // Update fields
        if (eventData.getName() != null)
            existing.setName(eventData.getName());
        if (eventData.getSlug() != null)
            existing.setSlug(eventData.getSlug());
        if (eventData.getDescription() != null)
            existing.setDescription(eventData.getDescription());
        if (eventData.getEventType() != null)
            existing.setEventType(eventData.getEventType());
        if (eventData.getStartsAt() != null)
            existing.setStartsAt(eventData.getStartsAt());
        if (eventData.getEventDate() != null)
            existing.setEventDate(eventData.getEventDate());
        if (eventData.getEventTime() != null)
            existing.setEventTime(eventData.getEventTime());
        if (eventData.getLocation() != null)
            existing.setLocation(eventData.getLocation());
        if (eventData.getAddress() != null)
            existing.setAddress(eventData.getAddress());
        if (eventData.getMaxParticipants() != null)
            existing.setMaxParticipants(eventData.getMaxParticipants());
        if (eventData.getRegistrationOpen() != null)
            existing.setRegistrationOpen(eventData.getRegistrationOpen());
        if (eventData.getRegistrationStartDate() != null)
            existing.setRegistrationStartDate(eventData.getRegistrationStartDate());
        if (eventData.getRegistrationEndDate() != null)
            existing.setRegistrationEndDate(eventData.getRegistrationEndDate());
        if (eventData.getPrice() != null)
            existing.setPrice(eventData.getPrice());
        if (eventData.getCurrency() != null)
            existing.setCurrency(eventData.getCurrency());
        if (eventData.getTermsAndConditions() != null)
            existing.setTermsAndConditions(eventData.getTermsAndConditions());
        if (eventData.getBannerUrl() != null)
            existing.setBannerUrl(eventData.getBannerUrl());
        if (eventData.getStatus() != null)
            existing.setStatus(eventData.getStatus());
        if (eventData.getPlatformFeePercentage() != null)
            existing.setPlatformFeePercentage(eventData.getPlatformFeePercentage());
        if (eventData.getTransferFrequency() != null)
            existing.setTransferFrequency(eventData.getTransferFrequency());

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Evento não encontrado");
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
        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // Legacy methods for compatibility
    public List<Event> list() {
        return findAll();
    }

    public Event get(Long id) {
        return findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
}