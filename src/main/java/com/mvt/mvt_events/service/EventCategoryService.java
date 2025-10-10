package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.EventCategory;
import com.mvt.mvt_events.repository.EventCategoryRepository;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.specification.EventCategorySpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@Transactional
public class EventCategoryService {

    @Autowired
    private EventCategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    /**
     * Create a new event category
     */
    public EventCategory create(Long eventId, EventCategory category) {
        log.info("Creating category for event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        category.setEvent(event);

        // Set defaults
        if (category.getCurrentParticipants() == null) {
            category.setCurrentParticipants(0);
        }
        if (category.getPrice() == null) {
            category.setPrice(BigDecimal.ZERO);
        }

        return categoryRepository.save(category);
    }

    /**
     * Update an existing category
     */
    public EventCategory update(Long categoryId, EventCategory updatedCategory) {
        log.info("Updating category: {}", categoryId);

        EventCategory existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));

        // Update fields
        if (updatedCategory.getName() != null) {
            existingCategory.setName(updatedCategory.getName());
        }
        if (updatedCategory.getMinAge() != null) {
            existingCategory.setMinAge(updatedCategory.getMinAge());
        }
        if (updatedCategory.getMaxAge() != null) {
            existingCategory.setMaxAge(updatedCategory.getMaxAge());
        }
        if (updatedCategory.getGender() != null) {
            existingCategory.setGender(updatedCategory.getGender());
        }
        if (updatedCategory.getDistance() != null) {
            existingCategory.setDistance(updatedCategory.getDistance());
        }
        if (updatedCategory.getDistanceUnit() != null) {
            existingCategory.setDistanceUnit(updatedCategory.getDistanceUnit());
        }
        if (updatedCategory.getPrice() != null) {
            existingCategory.setPrice(updatedCategory.getPrice());
        }
        if (updatedCategory.getMaxParticipants() != null) {
            existingCategory.setMaxParticipants(updatedCategory.getMaxParticipants());
        }
        if (updatedCategory.getObservations() != null) {
            existingCategory.setObservations(updatedCategory.getObservations());
        }

        return categoryRepository.save(existingCategory);
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public EventCategory get(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    /**
     * Get category with event loaded
     */
    @Transactional(readOnly = true)
    public EventCategory getWithEvent(Long categoryId) {
        return categoryRepository.findByIdWithEvent(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    /**
     * List all categories for an event
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listByEvent(Long eventId) {
        Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(eventId);
        return categoryRepository.findAll(spec);
    }

    /**
     * List only active categories for an event (use Specification)
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listActiveByEvent(Long eventId) {
        Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(eventId)
                .and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        return categoryRepository.findAll(spec);
    }

    /**
     * List available categories (active and with spots) for an event
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listAvailableByEvent(Long eventId) {
        return categoryRepository.findAvailableByEventId(eventId);
    }

    /**
     * List categories by gender (use Specification)
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listByEventAndGender(Long eventId, EventCategory.Gender gender) {
        Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(eventId)
                .and((root, query, cb) -> cb.equal(root.get("gender"), gender))
                .and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        return categoryRepository.findAll(spec);
    }

    /**
     * List categories within price range (use Specification)
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listByEventAndPriceRange(Long eventId, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<EventCategory> spec = EventCategorySpecification.belongsToEvent(eventId)
                .and((root, query, cb) -> cb.between(root.get("price"), minPrice, maxPrice))
                .and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        return categoryRepository.findAll(spec);
    }

    /**
     * Delete a category
     */
    public void delete(Long categoryId) {
        log.info("Deleting category: {}", categoryId);

        EventCategory category = get(categoryId);

        // Check if category has participants
        if (category.getCurrentParticipants() > 0) {
            throw new IllegalStateException("Cannot delete category with registered participants");
        }

        categoryRepository.deleteById(categoryId);
    }

    /**
     * Increment participant count
     */
    public void incrementParticipants(Long categoryId) {
        EventCategory category = get(categoryId);
        category.incrementParticipants();
        categoryRepository.save(category);
    }

    /**
     * Decrement participant count
     */
    public void decrementParticipants(Long categoryId) {
        EventCategory category = get(categoryId);
        category.decrementParticipants();
        categoryRepository.save(category);
    }

    /**
     * Check if category is available for registration
     */
    @Transactional(readOnly = true)
    public boolean isAvailableForRegistration(Long categoryId) {
        EventCategory category = get(categoryId);
        return category.isAvailableForRegistration();
    }

    /**
     * Check eligibility for a person
     */
    @Transactional(readOnly = true)
    public boolean isEligible(Long categoryId, Integer age, EventCategory.Gender gender) {
        EventCategory category = get(categoryId);
        return category.isEligible(age, gender);
    }

    /**
     * Get count of active categories for an event
     */
    @Transactional(readOnly = true)
    public Long countActiveByEvent(Long eventId) {
        return categoryRepository.countActiveByEventId(eventId);
    }

    /**
     * List all categories (admin view)
     */
    @Transactional(readOnly = true)
    public List<EventCategory> listAll() {
        return categoryRepository.findAll();
    }

    /**
     * List categories with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<EventCategory> listWithFilters(Long eventId, Pageable pageable) {
        Specification<EventCategory> spec = EventCategorySpecification.buildSpecification(eventId);
        return categoryRepository.findAll(spec, pageable);
    }
}
