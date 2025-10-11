package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.EventCategory;
import com.mvt.mvt_events.service.EventCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/event-categories")
@CrossOrigin(origins = "*")
@Tag(name = "Categorias de Eventos", description = "Gerenciamento de categorias/modalidades de eventos")
@SecurityRequirement(name = "bearerAuth")
public class EventCategoryController {

    @Autowired
    private EventCategoryService categoryService;

    @GetMapping
    @Operation(summary = "Listar categorias", description = "Lista paginada com filtro opcional por event/eventId")
    public Page<EventCategory> list(
            @RequestParam(value = "event", required = false) Long eventId,
            Pageable pageable) {
        return categoryService.listWithFilters(eventId, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Criar categoria", description = "Apenas ORGANIZER e ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@RequestBody @Valid CategoryRequest request) {
        EventCategory category = convertToEntity(request);
        EventCategory created = categoryService.create(request.getEventId(), category);
        return new CategoryResponse(created);
    }

    /**
     * Update an existing category
     * Only ORGANIZER and ADMIN can update categories
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        EventCategory category = convertToEntity(request);
        EventCategory updated = categoryService.update(id, category);
        return new CategoryResponse(updated);
    }

    /**
     * Get category by ID
     * Public endpoint - anyone can view categories
     */
    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        EventCategory category = categoryService.get(id);
        return new CategoryResponse(category);
    }

    /**
     * List all categories for an event
     * Public endpoint
     */
    @GetMapping("/event/{eventId}")
    public List<CategoryResponse> listByEvent(@PathVariable Long eventId) {
        List<EventCategory> categories = categoryService.listByEvent(eventId);
        return categories.stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * List only active categories for an event
     * Public endpoint
     */
    @GetMapping("/event/{eventId}/active")
    public List<CategoryResponse> listActiveByEvent(@PathVariable Long eventId) {
        List<EventCategory> categories = categoryService.listActiveByEvent(eventId);
        return categories.stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * List available categories (active and with spots) for an event
     * Public endpoint
     */
    @GetMapping("/event/{eventId}/available")
    public List<CategoryResponse> listAvailableByEvent(@PathVariable Long eventId) {
        List<EventCategory> categories = categoryService.listAvailableByEvent(eventId);
        return categories.stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * List categories by gender
     * Public endpoint
     */
    @GetMapping("/event/{eventId}/gender/{gender}")
    public List<CategoryResponse> listByGender(
            @PathVariable Long eventId,
            @PathVariable EventCategory.Gender gender) {
        List<EventCategory> categories = categoryService.listByEventAndGender(eventId, gender);
        return categories.stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * List categories within price range
     * Public endpoint
     */
    @GetMapping("/event/{eventId}/price-range")
    public List<CategoryResponse> listByPriceRange(
            @PathVariable Long eventId,
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        List<EventCategory> categories = categoryService.listByEventAndPriceRange(eventId, minPrice, maxPrice);
        return categories.stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Delete a category
     * Only ORGANIZER and ADMIN can delete categories
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }

    /**
     * Check if category is available for registration
     * Public endpoint
     */
    @GetMapping("/{id}/is-available")
    public ResponseEntity<Map<String, Boolean>> isAvailable(@PathVariable Long id) {
        boolean available = categoryService.isAvailableForRegistration(id);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Check eligibility for a person
     * Public endpoint
     */
    @GetMapping("/{id}/check-eligibility")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(
            @PathVariable Long id,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) EventCategory.Gender gender) {
        boolean eligible = categoryService.isEligible(id, age, gender);
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    /**
     * Get count of active categories for an event
     * Public endpoint
     */
    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<Map<String, Long>> countActive(@PathVariable Long eventId) {
        Long count = categoryService.countActiveByEvent(eventId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ============================================================================
    // DTOs
    // ============================================================================

    @Data
    @NoArgsConstructor
    public static class CategoryRequest {
        private Long eventId;
        private String name;
        private Integer minAge;
        private Integer maxAge;
        private String gender; // MALE, FEMALE, MIXED, OTHER
        private BigDecimal distance;
        private String distanceUnit;
        private BigDecimal price;
        private Integer maxParticipants;
        private String observations;
    }

    @Data
    @NoArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private Long eventId;
        private String name;
        private Integer minAge;
        private Integer maxAge;
        private String ageRangeFormatted;
        private String gender;
        private String genderDisplayName;
        private BigDecimal distance;
        private String distanceUnit;
        private String distanceFormatted;
        private BigDecimal price;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private Integer availableSpots;
        private Boolean isFull;
        private Boolean isAvailableForRegistration;
        private String observations;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public CategoryResponse(EventCategory category) {
            this.id = category.getId();
            this.eventId = category.getEvent() != null ? category.getEvent().getId() : null;
            this.name = category.getName();
            this.minAge = category.getMinAge();
            this.maxAge = category.getMaxAge();
            this.ageRangeFormatted = category.getAgeRangeFormatted();
            this.gender = category.getGender() != null ? category.getGender().name() : null;
            this.genderDisplayName = category.getGender() != null ? category.getGender().getDisplayName() : null;
            this.distance = category.getDistance();
            this.distanceUnit = category.getDistanceUnit() != null ? category.getDistanceUnit().name() : null;
            this.distanceFormatted = category.getDistanceFormatted();
            this.price = category.getPrice();
            this.maxParticipants = category.getMaxParticipants();
            this.currentParticipants = category.getCurrentParticipants();
            this.availableSpots = category.getAvailableSpots();
            this.isFull = category.isFull();
            this.isAvailableForRegistration = category.isAvailableForRegistration();
            this.observations = category.getObservations();
            this.createdAt = category.getCreatedAt();
            this.updatedAt = category.getUpdatedAt();
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private EventCategory convertToEntity(CategoryRequest request) {
        EventCategory category = new EventCategory();
        category.setName(request.getName());
        category.setMinAge(request.getMinAge());
        category.setMaxAge(request.getMaxAge());

        if (request.getGender() != null) {
            category.setGender(EventCategory.Gender.valueOf(request.getGender()));
        }

        category.setDistance(request.getDistance());
        if (request.getDistanceUnit() != null)
            category.setDistanceUnit(
                    com.mvt.mvt_events.jpa.EventCategory.DistanceUnit.valueOf(request.getDistanceUnit()));
        category.setPrice(request.getPrice());
        category.setMaxParticipants(request.getMaxParticipants());
        category.setObservations(request.getObservations());

        return category;
    }
}
