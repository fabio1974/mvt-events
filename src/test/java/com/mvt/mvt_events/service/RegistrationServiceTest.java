package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.repository.UserRepository;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private Registration registration;
    private Event event;
    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setName("Carlos Silva");
        user.setEmail("carlos.silva@email.com");
        user.setUsername("carlos.silva");
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        event = new Event();
        event.setId(1L);
        event.setName("Corrida de São Paulo");
        event.setMaxParticipants(100);
        event.setRegistrationOpen(true);
        event.setRegistrationStartDate(LocalDateTime.now().minusDays(1));
        event.setRegistrationEndDate(LocalDateTime.now().plusDays(30));
        event.setPrice(new BigDecimal("50.00"));

        registration = new Registration();
        registration.setId(1L);
        registration.setUser(user);
        registration.setEvent(event);
        registration.setPaymentStatus(Registration.PaymentStatus.PENDING);
    }

    @Test
    void shouldCreateRegistrationSuccessfully() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(false);
        when(registrationRepository.countByEventId(1L)).thenReturn(50L);
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        Registration result = registrationService.create(registration);

        // Then
        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertEquals(event, result.getEvent());
        assertEquals(Registration.PaymentStatus.PENDING, result.getPaymentStatus());

        verify(userRepository).findById(userId);
        verify(eventRepository).findById(1L);
        verify(registrationRepository).existsByUserIdAndEventId(userId, 1L);
        verify(registrationRepository).countByEventId(1L);
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEventNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Evento não encontrado", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(eventRepository).findById(1L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyRegistered() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Usuário já está inscrito neste evento", exception.getMessage());
        verify(registrationRepository).existsByUserIdAndEventId(userId, 1L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEventCapacityReached() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(false);
        when(registrationRepository.countByEventId(1L)).thenReturn(100L); // Maximum capacity reached

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Evento já atingiu a capacidade máxima", exception.getMessage());
        verify(registrationRepository).countByEventId(1L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRegistrationClosed() {
        // Given
        event.setRegistrationOpen(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Inscrições fechadas para este evento", exception.getMessage());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRegistrationPeriodNotStarted() {
        // Given
        event.setRegistrationStartDate(LocalDateTime.now().plusDays(1)); // Futuro
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Período de inscrição ainda não começou", exception.getMessage());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenRegistrationPeriodEnded() {
        // Given
        event.setRegistrationEndDate(LocalDateTime.now().minusDays(1)); // Passado
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(userId, 1L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.create(registration));

        assertEquals("Período de inscrição já encerrou", exception.getMessage());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldFindAllRegistrations() {
        // Given
        List<Registration> registrations = Arrays.asList(registration, new Registration());
        when(registrationRepository.findAll()).thenReturn(registrations);

        // When
        List<Registration> result = registrationService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(registrationRepository).findAll();
    }

    @Test
    void shouldFindRegistrationsByEventId() {
        // Given
        List<Registration> registrations = Arrays.asList(registration);
        when(registrationRepository.findByEventId(1L)).thenReturn(registrations);

        // When
        List<Registration> result = registrationService.findByEventId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(registration, result.get(0));
        verify(registrationRepository).findByEventId(1L);
    }

    @Test
    void shouldFindRegistrationsByUserId() {
        // Given
        List<Registration> registrations = Arrays.asList(registration);
        when(registrationRepository.findByUserId(userId)).thenReturn(registrations);

        // When
        List<Registration> result = registrationService.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(registration, result.get(0));
        verify(registrationRepository).findByUserId(userId);
    }

    @Test
    void shouldUpdatePaymentStatus() {
        // Given
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(Registration.class))).thenReturn(registration);

        // When
        Registration result = registrationService.updatePaymentStatus(1L, Registration.PaymentStatus.PAID);

        // Then
        assertNotNull(result);
        assertEquals(Registration.PaymentStatus.PAID, result.getPaymentStatus());
        verify(registrationRepository).findById(1L);
        verify(registrationRepository).save(registration);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingPaymentStatusOfNonExistentRegistration() {
        // Given
        when(registrationRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrationService.updatePaymentStatus(99L, Registration.PaymentStatus.PAID));

        assertEquals("Inscrição não encontrada", exception.getMessage());
        verify(registrationRepository).findById(99L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void shouldAllowOrganizerToRegisterForEvents() {
        // Given
        User organizer = new User();
        organizer.setId(UUID.randomUUID());
        organizer.setName("Maria Organizer");
        organizer.setEmail("maria@org.com");
        organizer.setRole(User.Role.ORGANIZER);
        organizer.setEnabled(true);

        Registration organizerRegistration = new Registration();
        organizerRegistration.setUser(organizer);
        organizerRegistration.setEvent(event);

        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(organizer.getId(), 1L)).thenReturn(false);
        when(registrationRepository.countByEventId(1L)).thenReturn(50L);
        when(registrationRepository.save(any(Registration.class))).thenReturn(organizerRegistration);

        // When
        Registration result = registrationService.create(organizerRegistration);

        // Then
        assertNotNull(result);
        assertEquals(organizer, result.getUser());
        assertEquals(User.Role.ORGANIZER, result.getUser().getRole());
        assertTrue(result.getUser().canRegisterForEvents());
        verify(registrationRepository).save(any(Registration.class));
    }
}