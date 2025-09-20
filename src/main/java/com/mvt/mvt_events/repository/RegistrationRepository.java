package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId")
    List<Registration> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT r FROM Registration r WHERE r.athlete.id = :athleteId")
    List<Registration> findByAthleteId(@Param("athleteId") Long athleteId);

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.athlete.id = :athleteId")
    Optional<Registration> findByEventIdAndAthleteId(@Param("eventId") Long eventId,
            @Param("athleteId") Long athleteId);

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.paymentStatus = :paymentStatus")
    List<Registration> findByEventIdAndPaymentStatus(@Param("eventId") Long eventId,
            @Param("paymentStatus") Registration.PaymentStatus paymentStatus);

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.status = :status")
    List<Registration> findByEventIdAndStatus(@Param("eventId") Long eventId,
            @Param("status") Registration.RegistrationStatus status);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'ACTIVE'")
    Long countActiveRegistrationsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId")
    Long countByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndAthleteId(Long eventId, Long athleteId);

    boolean existsByAthleteIdAndEventId(Long athleteId, Long eventId);
}