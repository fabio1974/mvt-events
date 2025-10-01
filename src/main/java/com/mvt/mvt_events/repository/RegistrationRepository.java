package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

        @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId")
        List<Registration> findByEventId(@Param("eventId") Long eventId);

        @Query("SELECT r FROM Registration r WHERE r.user.id = :userId")
        List<Registration> findByUserId(@Param("userId") UUID userId);

        @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.user.id = :userId")
        Optional<Registration> findByEventIdAndUserId(@Param("eventId") Long eventId,
                        @Param("userId") UUID userId);

        @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.status = :status")
        List<Registration> findByEventIdAndStatus(@Param("eventId") Long eventId,
                        @Param("status") Registration.RegistrationStatus status);

        @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'ACTIVE'")
        Long countActiveRegistrationsByEventId(@Param("eventId") Long eventId);

        @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId")
        Long countByEventId(@Param("eventId") Long eventId);

        @Query("SELECT r FROM Registration r JOIN FETCH r.user WHERE r.event.id = :eventId")
        List<Registration> findByEventIdWithUser(@Param("eventId") Long eventId);

        @Query("SELECT r FROM Registration r JOIN FETCH r.event WHERE r.user.id = :userId")
        List<Registration> findByUserIdWithEvent(@Param("userId") UUID userId);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event " +
                        "WHERE r.user.id = :userId")
        List<Registration> findByUserIdWithUserAndEvent(@Param("userId") UUID userId);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event " +
                        "LEFT JOIN FETCH r.payments " +
                        "WHERE r.user.id = :userId")
        List<Registration> findByUserIdWithUserEventAndPayments(@Param("userId") UUID userId);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event " +
                        "WHERE r.id = :id")
        Optional<Registration> findByIdWithUserAndEvent(@Param("id") Long id);

        @Query("SELECT r FROM Registration r " +
                        "JOIN FETCH r.user u " +
                        "LEFT JOIN FETCH u.organization " +
                        "JOIN FETCH r.event " +
                        "LEFT JOIN FETCH r.payments " +
                        "WHERE r.id = :id")
        Optional<Registration> findByIdWithUserEventAndPayments(@Param("id") Long id);

        boolean existsByEventIdAndUserId(Long eventId, UUID userId);

        boolean existsByUserIdAndEventId(UUID userId, Long eventId);

        @Query("SELECT COUNT(r) FROM Registration r WHERE r.user.id = :userId AND r.status = 'ACTIVE'")
        Long countActiveRegistrationsByUserId(@Param("userId") UUID userId);
}