package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.RegistrationListDTO;
import com.mvt.mvt_events.exception.RegistrationConflictException;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.repository.RegistrationRepository;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.specification.RegistrationSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RegistrationService {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    public Registration create(Registration registration) {
        // Validate user exists
        User user = userRepository.findById(registration.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Validate event exists
        Event event = eventRepository.findById(registration.getEvent().getId())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        // Update registration with found entities
        registration.setUser(user);
        registration.setEvent(event);

        // Check if user is already registered for this event
        if (registrationRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new RegistrationConflictException("Usuário já está inscrito neste evento");
        }

        // Check registration period - check specific dates first for detailed error
        // messages
        LocalDate now = LocalDate.now();
        if (event.getRegistrationStartDate() != null && now.isBefore(event.getRegistrationStartDate())) {
            throw new RuntimeException("Período de inscrição ainda não começou");
        }

        if (event.getRegistrationEndDate() != null && now.isAfter(event.getRegistrationEndDate())) {
            throw new RuntimeException("Período de inscrição já encerrou");
        }

        // Check if registration is open (general flag)
        if (!event.getRegistrationOpen()) {
            throw new RuntimeException("Inscrições fechadas para este evento");
        }

        // Check event capacity
        Long currentRegistrations = registrationRepository.countByEventId(event.getId());
        if (event.getMaxParticipants() != null && currentRegistrations >= event.getMaxParticipants()) {
            throw new RuntimeException("Evento já atingiu a capacidade máxima");
        }

        // Set default values
        registration.setRegistrationDate(LocalDateTime.now());
        if (registration.getStatus() == null) {
            registration.setStatus(Registration.RegistrationStatus.PENDING);
        }

        return registrationRepository.save(registration);
    }

    /**
     * Lista leve PAGINADA - retorna apenas dados essenciais sem lazy loading
     * Ideal para listagens e tabelas
     */
    @Transactional(readOnly = true)
    public Page<RegistrationListDTO> list(Pageable pageable) {
        Page<Registration> registrations = registrationRepository.findAll(pageable);
        return registrations.map(r -> new RegistrationListDTO(
                r.getId(),
                r.getRegistrationDate(),
                r.getStatus(),
                r.getNotes(),
                r.getUser().getId().toString(),
                r.getUser().getName(),
                r.getUser().getUsername(),
                r.getEvent().getId(),
                r.getEvent().getName(),
                r.getEvent().getEventDate(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null));
    }

    /**
     * Lista leve - retorna apenas dados essenciais sem lazy loading (SEM PAGINAÇÃO)
     * Mantido para compatibilidade
     */
    @Transactional(readOnly = true)
    public List<RegistrationListDTO> listAll() {
        List<Registration> registrations = registrationRepository.findAll();
        return registrations.stream()
                .map(r -> new RegistrationListDTO(
                        r.getId(),
                        r.getRegistrationDate(),
                        r.getStatus(),
                        r.getNotes(),
                        r.getUser().getId().toString(),
                        r.getUser().getName(),
                        r.getUser().getUsername(),
                        r.getEvent().getId(),
                        r.getEvent().getName(),
                        r.getEvent().getEventDate(),
                        r.getCategory() != null ? r.getCategory().getId() : null,
                        r.getCategory() != null ? r.getCategory().getName() : null))
                .toList();
    }

    /**
     * Lista com filtros dinâmicos usando Specifications
     * Suporta filtro por status, eventId e userId (todos opcionais)
     */
    @Transactional(readOnly = true)
    public Page<RegistrationListDTO> listWithFilters(
            Registration.RegistrationStatus status,
            Long eventId,
            UUID userId,
            Pageable pageable) {

        Specification<Registration> spec = RegistrationSpecification.withFilters(status, eventId, userId);
        Page<Registration> registrations = registrationRepository.findAll(spec, pageable);

        return registrations.map(r -> new RegistrationListDTO(
                r.getId(),
                r.getRegistrationDate(),
                r.getStatus(),
                r.getNotes(),
                r.getUser().getId().toString(),
                r.getUser().getName(),
                r.getUser().getUsername(),
                r.getEvent().getId(),
                r.getEvent().getName(),
                r.getEvent().getEventDate(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null));
    }

    /**
     * Lista por status PAGINADA - retorna apenas inscrições com o status
     * especificado
     * DEPRECATED: Use listWithFilters() ao invés deste método
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Page<RegistrationListDTO> listByStatus(Registration.RegistrationStatus status, Pageable pageable) {
        Specification<Registration> spec = RegistrationSpecification.hasStatus(status);
        Page<Registration> registrations = registrationRepository.findAll(spec, pageable);
        return registrations.map(r -> new RegistrationListDTO(
                r.getId(),
                r.getRegistrationDate(),
                r.getStatus(),
                r.getNotes(),
                r.getUser().getId().toString(),
                r.getUser().getName(),
                r.getUser().getUsername(),
                r.getEvent().getId(),
                r.getEvent().getName(),
                r.getEvent().getEventDate(),
                r.getCategory() != null ? r.getCategory().getId() : null,
                r.getCategory() != null ? r.getCategory().getName() : null));
    }

    /**
     * Lista por status - retorna apenas inscrições com o status especificado (SEM
     * PAGINAÇÃO)
     * Mantido para compatibilidade
     */
    @Transactional(readOnly = true)
    public List<RegistrationListDTO> listByStatus(Registration.RegistrationStatus status) {
        Specification<Registration> spec = RegistrationSpecification.hasStatus(status);
        List<Registration> registrations = registrationRepository.findAll(spec);
        return registrations.stream()
                .map(r -> new RegistrationListDTO(
                        r.getId(),
                        r.getRegistrationDate(),
                        r.getStatus(),
                        r.getNotes(),
                        r.getUser().getId().toString(),
                        r.getUser().getName(),
                        r.getUser().getUsername(),
                        r.getEvent().getId(),
                        r.getEvent().getName(),
                        r.getEvent().getEventDate(),
                        r.getCategory() != null ? r.getCategory().getId() : null,
                        r.getCategory() != null ? r.getCategory().getName() : null))
                .toList();
    }

    public Registration get(Long id) {
        return registrationRepository.findByIdWithUserEventAndPayments(id)
                .orElseThrow(() -> new RuntimeException("Inscrição não encontrada"));
    }

    public List<Registration> getByEventId(Long eventId) {
        return registrationRepository.findByEventId(eventId);
    }

    public List<Registration> findByUserId(UUID userId) {
        return registrationRepository.findByUserIdWithUserEventAndPayments(userId);
    }

    public Registration updateStatus(Long id, Registration.RegistrationStatus status) {
        Registration registration = get(id);
        registration.setStatus(status);
        return registrationRepository.save(registration);
    }

    public Registration update(Long id, Registration payload) {
        Registration existing = get(id);

        // Update allowed fields
        if (payload.getNotes() != null) {
            existing.setNotes(payload.getNotes());
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }

        return registrationRepository.save(existing);
    }

    public void cancelRegistration(Long id) {
        Registration registration = get(id);

        if (!registration.canBeCancelled()) {
            throw new RuntimeException("Esta inscrição não pode ser cancelada");
        }

        registration.setStatus(Registration.RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
    }

    public void delete(Long id) {
        Registration registration = get(id);
        registrationRepository.delete(registration);
    }

    /**
     * Save a registration entity to the database
     */
    public Registration save(Registration registration) {
        return registrationRepository.save(registration);
    }

    // Backward compatibility methods - retorna entidades completas quando
    // necessário
    public List<Registration> findAll() {
        return registrationRepository.findAllWithUserAndEvent();
    }

    public List<Registration> findByEventId(Long eventId) {
        return getByEventId(eventId);
    }

    public Registration findById(Long id) {
        return get(id);
    }
}