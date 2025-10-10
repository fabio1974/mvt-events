package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventCategoryRepository
                extends JpaRepository<EventCategory, Long>, JpaSpecificationExecutor<EventCategory> {

        // Métodos com lógica de negócio específica - mantidos
        /**
         * Find available categories (with spots)
         */
        @Query("SELECT ec FROM EventCategory ec WHERE ec.event.id = :eventId " +
                        "AND (ec.maxParticipants IS NULL OR ec.currentParticipants < ec.maxParticipants) " +
                        "ORDER BY ec.price ASC")
        List<EventCategory> findAvailableByEventId(@Param("eventId") Long eventId);

        /**
         * Find full categories (reached max participants)
         */
        @Query("SELECT ec FROM EventCategory ec WHERE ec.event.id = :eventId " +
                        "AND ec.maxParticipants IS NOT NULL " +
                        "AND ec.currentParticipants >= ec.maxParticipants")
        List<EventCategory> findFullCategoriesByEventId(@Param("eventId") Long eventId);

        /**
         * Count categories for an event
         */
        @Query("SELECT COUNT(ec) FROM EventCategory ec WHERE ec.event.id = :eventId")
        Long countActiveByEventId(@Param("eventId") Long eventId);

        /**
         * Check if category exists
         */
        @Query("SELECT CASE WHEN COUNT(ec) > 0 THEN true ELSE false END " +
                        "FROM EventCategory ec WHERE ec.id = :categoryId")
        boolean existsByIdAndIsActive(@Param("categoryId") Long categoryId);

        // Métodos com FETCH JOIN - mantidos por performance
        /**
         * Find category with event loaded
         */
        @Query("SELECT ec FROM EventCategory ec " +
                        "JOIN FETCH ec.event " +
                        "WHERE ec.id = :categoryId")
        Optional<EventCategory> findByIdWithEvent(@Param("categoryId") Long categoryId);

        // Métodos de delete específicos - mantidos
        /**
         * Delete all categories for an event
         */
        void deleteByEventId(Long eventId);
}
