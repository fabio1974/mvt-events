package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.EventCreateRequest;
import com.mvt.mvt_events.dto.EventUpdateRequest;
import com.mvt.mvt_events.jpa.City;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.EventCategory;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.jpa.TransferFrequency;
import com.mvt.mvt_events.repository.CityRepository;
import com.mvt.mvt_events.repository.EventCategoryRepository;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import com.mvt.mvt_events.specification.EventCategorySpecification;
import com.mvt.mvt_events.specification.EventSpecification;
import com.mvt.mvt_events.specification.EventSpecifications;
import com.mvt.mvt_events.tenant.TenantContext;
import com.mvt.mvt_events.util.CascadeUpdateHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {
    public java.util.Map<String, Integer> getStats() {
        List<Event> allEvents = findAll();
        int total = allEvents.size();
        int active = (int) allEvents.stream().filter(e -> "PUBLISHED".equalsIgnoreCase(e.getStatus().name())).count();
        int finished = (int) allEvents.stream().filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus().name())).count();
        int cancelled = (int) allEvents.stream().filter(e -> "CANCELLED".equalsIgnoreCase(e.getStatus().name()))
                .count();

        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("finished", finished);
        stats.put("cancelled", cancelled);
        return stats;
    }

    private final EventRepository repository;
    private final OrganizationRepository organizationRepository;
    private final EventCategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final CascadeUpdateHelper cascadeUpdateHelper;

    public EventService(EventRepository repository, OrganizationRepository organizationRepository,
            EventCategoryRepository categoryRepository, CityRepository cityRepository,
            CascadeUpdateHelper cascadeUpdateHelper) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.categoryRepository = categoryRepository;
        this.cityRepository = cityRepository;
        this.cascadeUpdateHelper = cascadeUpdateHelper;
    }

    // DEBUG: Expor repository temporariamente
    public EventRepository getRepository() {
        return repository;
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
        event.setLocation(request.getLocation());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setRegistrationOpen(request.getRegistrationOpen());
        event.setRegistrationStartDate(request.getRegistrationStartDate());
        event.setRegistrationEndDate(request.getRegistrationEndDate());
        event.setPrice(request.getPrice());
        event.setCurrency(request.getCurrency());
        event.setPlatformFeePercentage(request.getPlatformFeePercentage());
        event.setTermsAndConditions(request.getTermsAndConditions());
        event.setTransferFrequency(TransferFrequency.WEEKLY); // Default value
        event.setStatus(Event.EventStatus.DRAFT); // Default status

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

        // Save event first
        Event savedEvent = repository.save(event);

        // Create categories if provided
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            for (EventCreateRequest.CategoryRequest catRequest : request.getCategories()) {
                EventCategory category = new EventCategory();
                category.setEvent(savedEvent);
                category.setName(catRequest.getName());
                category.setMinAge(catRequest.getMinAge());
                category.setMaxAge(catRequest.getMaxAge());
                category.setGender(catRequest.getGender());
                category.setDistance(catRequest.getDistance());
                if (catRequest.getDistanceUnit() != null)
                    category.setDistanceUnit(EventCategory.DistanceUnit.valueOf(catRequest.getDistanceUnit()));
                category.setPrice(catRequest.getPrice());
                category.setMaxParticipants(catRequest.getMaxParticipants());
                category.setCurrentParticipants(0);
                category.setObservations(catRequest.getObservations());

                categoryRepository.save(category);
            }
        }

        return savedEvent;
    }

    public Event create(Event event) {
        // Handle organization - either from organization object or organizationId
        if (event.getOrganization() == null) {
            throw new RuntimeException("Organização é obrigatória para criar um evento");
        }

        if (event.getOrganization().getId() != null) {
            Organization organization = organizationRepository.findById(event.getOrganization().getId())
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
            event.setOrganization(organization);
        } else {
            throw new RuntimeException("ID da organização é obrigatório");
        }

        // Handle city if provided
        if (event.getCity() != null && event.getCity().getId() != null) {
            City city = cityRepository.findById(event.getCity().getId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            event.setCity(city);
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

        // Set default values if not provided
        if (event.getStatus() == null) {
            event.setStatus(Event.EventStatus.DRAFT);
        }
        if (event.getTransferFrequency() == null) {
            event.setTransferFrequency(TransferFrequency.WEEKLY);
        }
        if (event.getRegistrationOpen() == null) {
            event.setRegistrationOpen(true);
        }

        // Establish bidirectional relationship with categories
        if (event.getCategories() != null && !event.getCategories().isEmpty()) {
            event.getCategories().forEach(category -> category.setEvent(event));
        }

        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        // Buscar todos os IDs primeiro
        List<Long> allIds = repository.findAll().stream()
                .map(Event::getId)
                .collect(java.util.stream.Collectors.toList());

        // Buscar eventos completos com EntityGraph
        return repository.findAllByIds(allIds);
    }

    public Optional<Event> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<Event> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public List<Event> findByOrganizationId(Long organizationId) {
        Specification<Event> spec = EventSpecification.hasOrganizationId(organizationId);
        List<Event> events = repository.findAll(spec);
        // Force load categories
        events.forEach(e -> e.getCategories().size());
        return events;
    }

    @Transactional(readOnly = true)
    public List<Event> findPublishedEvents() {
        Specification<Event> spec = EventSpecification.hasStatus(Event.EventStatus.PUBLISHED);
        List<Event> events = repository.findAll(spec);
        // Force load categories
        events.forEach(e -> e.getCategories().size());
        return events;
    }

    @Transactional(readOnly = true)
    public Event findPublishedEventById(Long id) {
        Event event = repository.findById(id)
                .filter(e -> e.getStatus() == Event.EventStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Evento público não encontrado"));
        // Force load categories
        event.getCategories().size();
        return event;
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

        // Validate city if changed
        if (eventData.getCity() != null && eventData.getCity().getId() != null) {
            City city = cityRepository.findById(eventData.getCity().getId())
                    .orElseThrow(() -> new RuntimeException("Cidade não encontrada"));
            existing.setCity(city);
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
        if (eventData.getEventDate() != null)
            existing.setEventDate(eventData.getEventDate());
        if (eventData.getLocation() != null)
            existing.setLocation(eventData.getLocation());
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
        if (eventData.getStatus() != null)
            existing.setStatus(eventData.getStatus());
        if (eventData.getPlatformFeePercentage() != null)
            existing.setPlatformFeePercentage(eventData.getPlatformFeePercentage());
        if (eventData.getTransferFrequency() != null)
            existing.setTransferFrequency(eventData.getTransferFrequency());

        // Save event first
        Event savedEvent = repository.save(existing);

        // ==================== HANDLE CATEGORIES (CASCADE UPDATE) ====================
        // Usa CascadeUpdateHelper para gerenciar relacionamento 1:N
        if (eventData.getCategories() != null) {
            // Buscar categorias existentes
            Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(savedEvent.getId());
            List<EventCategory> existingCategories = categoryRepository.findAll(spec);

            // Usar helper genérico para cascade update
            cascadeUpdateHelper.updateChildrenWithInit(
                    savedEvent, // Entidade pai
                    eventData.getCategories(), // Lista de filhos do payload
                    existingCategories, // Lista de filhos existentes
                    EventCategory::getId, // Função para extrair ID
                    EventCategory::setEvent, // Setter para vincular ao pai
                    (existingCat, payloadCat) -> { // Função de update de campos
                        if (payloadCat.getName() != null)
                            existingCat.setName(payloadCat.getName());
                        if (payloadCat.getMinAge() != null)
                            existingCat.setMinAge(payloadCat.getMinAge());
                        if (payloadCat.getMaxAge() != null)
                            existingCat.setMaxAge(payloadCat.getMaxAge());
                        if (payloadCat.getGender() != null)
                            existingCat.setGender(payloadCat.getGender());
                        if (payloadCat.getDistance() != null)
                            existingCat.setDistance(payloadCat.getDistance());
                        if (payloadCat.getDistanceUnit() != null)
                            existingCat.setDistanceUnit(payloadCat.getDistanceUnit());
                        if (payloadCat.getPrice() != null)
                            existingCat.setPrice(payloadCat.getPrice());
                        if (payloadCat.getMaxParticipants() != null)
                            existingCat.setMaxParticipants(payloadCat.getMaxParticipants());
                        if (payloadCat.getObservations() != null)
                            existingCat.setObservations(payloadCat.getObservations());
                    },
                    (child) -> { // Função de inicialização para novos filhos
                        if (child.getCurrentParticipants() == null) {
                            child.setCurrentParticipants(0);
                        }
                    },
                    categoryRepository // Repository JPA
            );
        }

        // Refresh event to load updated categories
        Event refreshedEvent = repository.findById(savedEvent.getId())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        refreshedEvent.getCategories().size(); // Force load

        return refreshedEvent;
    }

    /**
     * Update event with categories in a single transaction
     */
    @Transactional
    public Event updateWithCategories(Long id, EventUpdateRequest request) {
        Event existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        // Update organization if changed
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
            existing.setOrganization(organization);
        }

        // Validate slug if changed
        if (request.getSlug() != null && !request.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlugAndIdNot(request.getSlug(), id)) {
                throw new RuntimeException("Já existe um evento com este slug");
            }
            existing.setSlug(request.getSlug());
        }

        // Update event fields
        if (request.getName() != null)
            existing.setName(request.getName());
        if (request.getDescription() != null)
            existing.setDescription(request.getDescription());
        if (request.getEventType() != null)
            existing.setEventType(request.getEventType());
        if (request.getEventDate() != null)
            existing.setEventDate(request.getEventDate());
        if (request.getLocation() != null)
            existing.setLocation(request.getLocation());
        if (request.getMaxParticipants() != null)
            existing.setMaxParticipants(request.getMaxParticipants());
        if (request.getRegistrationOpen() != null)
            existing.setRegistrationOpen(request.getRegistrationOpen());
        if (request.getRegistrationStartDate() != null)
            existing.setRegistrationStartDate(request.getRegistrationStartDate());
        if (request.getRegistrationEndDate() != null)
            existing.setRegistrationEndDate(request.getRegistrationEndDate());
        if (request.getPrice() != null)
            existing.setPrice(request.getPrice());
        if (request.getCurrency() != null)
            existing.setCurrency(request.getCurrency());
        if (request.getPlatformFeePercentage() != null)
            existing.setPlatformFeePercentage(request.getPlatformFeePercentage());
        if (request.getTermsAndConditions() != null)
            existing.setTermsAndConditions(request.getTermsAndConditions());
        if (request.getStatus() != null)
            existing.setStatus(request.getStatus());

        // Save event
        Event savedEvent = repository.save(existing);

        // Handle categories if provided
        if (request.getCategories() != null) {
            // Buscar todas as categorias existentes do evento usando Specification
            Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(savedEvent.getId());
            java.util.List<EventCategory> existingCategories = categoryRepository.findAll(spec);

            // Pegar os IDs das categorias enviadas na requisição que já existem (para
            // update)
            java.util.Set<Long> requestCategoryIds = request.getCategories().stream()
                    .map(EventUpdateRequest.CategoryUpdateRequest::getId)
                    .filter(id2 -> id2 != null)
                    .collect(java.util.stream.Collectors.toSet());

            System.out.println("Categorias existentes no banco: " + existingCategories.size());
            System.out.println("IDs das categorias na requisição: " + requestCategoryIds);

            // Deletar categorias que não estão mais na requisição
            java.util.List<EventCategory> categoriesToDelete = new java.util.ArrayList<>();
            for (EventCategory existingCat : existingCategories) {
                if (!requestCategoryIds.contains(existingCat.getId())) {
                    System.out
                            .println("Deletando categoria ID: " + existingCat.getId() + " - " + existingCat.getName());
                    categoriesToDelete.add(existingCat);
                }
            }

            if (!categoriesToDelete.isEmpty()) {
                categoryRepository.deleteAll(categoriesToDelete);
                categoryRepository.flush(); // Force immediate deletion
            }

            // Processar as categorias da requisição
            for (EventUpdateRequest.CategoryUpdateRequest catRequest : request.getCategories()) {
                if (catRequest.get_delete() != null && catRequest.get_delete()) {
                    // Delete category
                    if (catRequest.getId() != null) {
                        categoryRepository.deleteById(catRequest.getId());
                    }
                } else if (catRequest.getId() != null) {
                    // Update existing category
                    EventCategory existingCat = categoryRepository.findById(catRequest.getId())
                            .orElseThrow(() -> new RuntimeException("Categoria não encontrada: " + catRequest.getId()));

                    if (catRequest.getName() != null)
                        existingCat.setName(catRequest.getName());
                    if (catRequest.getMinAge() != null)
                        existingCat.setMinAge(catRequest.getMinAge());
                    if (catRequest.getMaxAge() != null)
                        existingCat.setMaxAge(catRequest.getMaxAge());
                    if (catRequest.getGender() != null)
                        existingCat.setGender(catRequest.getGender());
                    if (catRequest.getDistance() != null)
                        existingCat.setDistance(catRequest.getDistance());
                    if (catRequest.getDistanceUnit() != null)
                        existingCat.setDistanceUnit(EventCategory.DistanceUnit.valueOf(catRequest.getDistanceUnit()));
                    if (catRequest.getPrice() != null)
                        existingCat.setPrice(catRequest.getPrice());
                    if (catRequest.getMaxParticipants() != null)
                        existingCat.setMaxParticipants(catRequest.getMaxParticipants());
                    if (catRequest.getObservations() != null)
                        existingCat.setObservations(catRequest.getObservations());

                    categoryRepository.save(existingCat);
                } else {
                    // Create new category
                    EventCategory newCategory = new EventCategory();
                    newCategory.setEvent(savedEvent);
                    newCategory.setName(catRequest.getName());
                    newCategory.setMinAge(catRequest.getMinAge());
                    newCategory.setMaxAge(catRequest.getMaxAge());
                    newCategory.setGender(catRequest.getGender());
                    newCategory.setDistance(catRequest.getDistance());
                    newCategory.setDistanceUnit(EventCategory.DistanceUnit.valueOf(catRequest.getDistanceUnit()));
                    newCategory.setPrice(catRequest.getPrice());
                    newCategory.setMaxParticipants(catRequest.getMaxParticipants());
                    newCategory.setCurrentParticipants(0);
                    newCategory.setObservations(catRequest.getObservations());

                    categoryRepository.save(newCategory);
                }
            }
        }

        // Refresh event to load categories
        Event refreshedEvent = repository.findById(savedEvent.getId())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        refreshedEvent.getCategories().size(); // Force load

        return refreshedEvent;
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
    /**
     * Lista eventos com paginação SIMPLES (aceita HHH90003004)
     */
    @Transactional(readOnly = true)
    public Page<Event> list(Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenantId();
        boolean isAdmin = TenantContext.isAdmin();

        System.out.println("🔍 EventService.list - Tenant ID: " + tenantId);
        System.out.println("🔍 EventService.list - Is Admin: " + isAdmin);

        Page<Event> events;

        if (isAdmin) {
            // ADMIN vê todos os eventos, sem filtro
            System.out.println("👑 EventService.list - ADMIN mode: returning ALL events");
            events = repository.findAll(pageable);
        } else {
            // Usuário normal: aplica filtro de organização
            if (tenantId == null) {
                throw new RuntimeException("Tenant ID não encontrado no contexto");
            }

            Specification<Event> spec = EventSpecifications.forOrganization(tenantId);
            events = repository.findAll(spec, pageable);
        }

        // IMPORTANTE: Força carregamento de relações LAZY dentro da transação
        // para evitar LazyInitializationException
        events.getContent().forEach(event -> {
            if (event.getOrganization() != null) {
                event.getOrganization().getName(); // Força carregamento
            }
            if (event.getCategories() != null) {
                event.getCategories().size(); // Força carregamento
            }
            if (event.getCity() != null) {
                event.getCity().getName(); // Força carregamento
            }
        });

        System.out.println("🔍 EventService.list - Total elements: " + events.getTotalElements());
        System.out.println("🔍 EventService.list - Content size: " + events.getContent().size());

        return events;
    }

    /**
     * Lista eventos com filtros dinâmicos
     */
    @Transactional(readOnly = true)
    public Page<Event> listWithFilters(
            Event.EventStatus status,
            Long organizationId,
            Long categoryId,
            String city,
            String state,
            Event.EventType eventType,
            String name,
            Pageable pageable) {

        boolean isAdmin = TenantContext.isAdmin();
        Specification<Event> combinedSpec;

        if (isAdmin) {
            // ADMIN vê todos - apenas aplica filtros fornecidos
            System.out.println("👑 EventService.listWithFilters - ADMIN mode: no tenant filter");
            combinedSpec = EventSpecification.withFilters(status, organizationId, categoryId, city, state,
                    eventType, name);
        } else {
            // Usuário normal: sempre aplica filtro de tenant primeiro
            Long tenantId = TenantContext.getCurrentTenantId();
            if (tenantId == null) {
                throw new RuntimeException("Tenant ID não encontrado no contexto");
            }

            // Combina filtro de tenant com outros filtros
            Specification<Event> tenantSpec = EventSpecifications.forOrganization(tenantId);
            Specification<Event> filterSpec = EventSpecification.withFilters(status, organizationId, categoryId, city,
                    state,
                    eventType, name);

            combinedSpec = tenantSpec.and(filterSpec);
        }

        Page<Event> events = repository.findAll(combinedSpec, pageable);

        // IMPORTANTE: Força carregamento de relações LAZY
        events.getContent().forEach(event -> {
            if (event.getOrganization() != null) {
                event.getOrganization().getName();
            }
            if (event.getCategories() != null) {
                event.getCategories().size();
            }
            if (event.getCity() != null) {
                event.getCity().getName();
            }
        });

        return events;
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        boolean isAdmin = TenantContext.isAdmin();
        Long tenantId = TenantContext.getCurrentTenantId();

        System.out.println("🔍 EventService.get - ID: " + id);
        System.out.println("🔍 EventService.get - Is Admin: " + isAdmin);
        System.out.println("🔍 EventService.get - Tenant ID: " + tenantId);

        Event event = findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        System.out.println("🔍 EventService.get - Event found: " + event.getName());
        System.out.println("🔍 EventService.get - Event org ID: "
                + (event.getOrganization() != null ? event.getOrganization().getId() : "null"));

        // Se não for ADMIN, valida se o evento pertence à organização do usuário
        if (!isAdmin) {
            if (tenantId == null) {
                throw new RuntimeException("Tenant ID não encontrado no contexto");
            }

            if (event.getOrganization() == null || !event.getOrganization().getId().equals(tenantId)) {
                System.out.println("⛔ EventService.get - Access denied: event belongs to org " +
                        (event.getOrganization() != null ? event.getOrganization().getId() : "null") +
                        " but user is from org " + tenantId);
                throw new RuntimeException("Event not found with id: " + id);
            }
        }

        // Force load categories to avoid lazy loading issues
        event.getCategories().size();

        // Force load organization
        if (event.getOrganization() != null) {
            event.getOrganization().getName();
        }

        // Force load city
        if (event.getCity() != null) {
            event.getCity().getName();
        }

        return event;
    }
}