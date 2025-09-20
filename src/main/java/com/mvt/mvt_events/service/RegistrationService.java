package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.Athlete;
import com.mvt.mvt_events.repository.RegistrationRepository;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.repository.AthleteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RegistrationService {

    private final RegistrationRepository repository;
    private final EventRepository eventRepository;
    private final AthleteRepository athleteRepository;

    public RegistrationService(RegistrationRepository repository,
            EventRepository eventRepository,
            AthleteRepository athleteRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.athleteRepository = athleteRepository;
    }

    public Registration create(Registration registration) {
        // Validate athlete exists
        Athlete athlete = athleteRepository.findById(registration.getAthlete().getId())
                .orElseThrow(() -> new RuntimeException("Atleta não encontrado"));

        // Validate event exists
        Event event = eventRepository.findById(registration.getEvent().getId())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        // Check if athlete is already registered for this event
        if (repository.existsByAthleteIdAndEventId(athlete.getId(), event.getId())) {
            throw new RuntimeException("Atleta já está inscrito neste evento");
        }

        // Check registration period
        if (!event.getRegistrationOpen()) {
            throw new RuntimeException("Inscrições fechadas para este evento");
        }

        LocalDateTime now = LocalDateTime.now();
        if (event.getRegistrationStartDate() != null && now.isBefore(event.getRegistrationStartDate())) {
            throw new RuntimeException("Período de inscrição ainda não começou");
        }

        if (event.getRegistrationEndDate() != null && now.isAfter(event.getRegistrationEndDate())) {
            throw new RuntimeException("Período de inscrição já encerrou");
        }

        // Check event capacity
        if (event.getMaxParticipants() != null) {
            Long currentRegistrations = repository.countByEventId(event.getId());
            if (currentRegistrations >= event.getMaxParticipants()) {
                throw new RuntimeException("Evento já atingiu a capacidade máxima");
            }
        }

        // Set entities
        registration.setAthlete(athlete);
        registration.setEvent(event);

        // Set registration date if not provided
        if (registration.getRegistrationDate() == null) {
            registration.setRegistrationDate(LocalDateTime.now());
        }

        return repository.save(registration);
    }

    public List<Registration> findAll() {
        return repository.findAll();
    }

    public List<Registration> findByEventId(Long eventId) {
        return repository.findByEventId(eventId);
    }

    public List<Registration> findByAthleteId(Long athleteId) {
        return repository.findByAthleteId(athleteId);
    }

    public Registration updatePaymentStatus(Long id, Registration.PaymentStatus paymentStatus) {
        Registration registration = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));

        registration.setPaymentStatus(paymentStatus);
        return repository.save(registration);
    }

    public Registration update(Long id, Registration registrationData) {
        Registration existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));

        // Update allowed fields
        if (registrationData.getPaymentStatus() != null) {
            existing.setPaymentStatus(registrationData.getPaymentStatus());
        }
        if (registrationData.getStatus() != null) {
            existing.setStatus(registrationData.getStatus());
        }

        return repository.save(existing);
    }

    public Registration updateStatus(Long id, Registration.RegistrationStatus status) {
        Registration registration = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));

        registration.setStatus(status);
        return repository.save(registration);
    }

    public void cancelRegistration(Long id) {
        Registration registration = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));

        registration.setStatus(Registration.RegistrationStatus.CANCELLED);
        repository.save(registration);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Inscrição não encontrada");
        }
        repository.deleteById(id);
    }

    // Legacy methods for compatibility
    public List<Registration> list() {
        return findAll();
    }

    public Registration get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found with id: " + id));
    }

    public List<Registration> getByEventId(Long eventId) {
        return findByEventId(eventId);
    }

    public List<Registration> getByAthleteId(Long athleteId) {
        return findByAthleteId(athleteId);
    }
}