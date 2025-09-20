package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.repository.EventRepository;
import com.mvt.mvt_events.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private EventService eventService;

    private Event event;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Tech Conference Brasil");
        organization.setContactEmail("contato@techconf.com.br");

        event = new Event();
        event.setId(1L);
        event.setName("Corrida de São Paulo 2025");
        event.setDescription("Uma corrida incrível pela cidade");
        event.setEventDate(LocalDate.of(2025, 12, 15));
        event.setEventTime(LocalTime.of(7, 0));
        event.setLocation("Parque Ibirapuera");
        event.setAddress("Av. Paulista, 1000 - São Paulo, SP");
        event.setMaxParticipants(500);
        event.setPrice(new BigDecimal("75.00"));
        event.setCurrency("BRL");
        event.setEventType(Event.EventType.RUNNING);
        event.setStatus(Event.EventStatus.DRAFT);
        event.setOrganization(organization);
        event.setSlug("corrida-de-sao-paulo-2025");
        event.setRegistrationOpen(true);
        event.setRegistrationStartDate(LocalDateTime.now().minusDays(1));
        event.setRegistrationEndDate(LocalDateTime.now().plusDays(30));
    }

    @Test
    void shouldCreateEventSuccessfully() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(eventRepository.existsBySlug(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        Event result = eventService.create(event);

        // Then
        assertNotNull(result);
        assertEquals("Corrida de São Paulo 2025", result.getName());
        assertEquals(organization, result.getOrganization());
        assertEquals("corrida-de-sao-paulo-2025", result.getSlug());
        assertEquals(new BigDecimal("75.00"), result.getPrice());

        verify(organizationRepository).findById(1L);
        verify(eventRepository).existsBySlug(anyString());
        verify(eventRepository).save(event);
    }

    @Test
    void shouldGenerateSlugAutomaticallyFromName() {
        // Given
        event.setSlug(null); // Slug não definido
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(eventRepository.existsBySlug(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            assertNotNull(saved.getSlug());
            assertTrue(saved.getSlug().contains("corrida-de-sao-paulo-2025"));
            return saved;
        });

        // When
        Event result = eventService.create(event);

        // Then
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void shouldThrowExceptionWhenOrganizationNotFound() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.create(event));

        assertEquals("Organização não encontrada", exception.getMessage());
        verify(organizationRepository).findById(1L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSlugAlreadyExists() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(eventRepository.existsBySlug(anyString())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.create(event));

        assertEquals("Já existe um evento com este slug", exception.getMessage());
        verify(eventRepository).existsBySlug(anyString());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldFindAllEvents() {
        // Given
        List<Event> events = Arrays.asList(event, new Event());
        when(eventRepository.findAll()).thenReturn(events);

        // When
        List<Event> result = eventService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository).findAll();
    }

    @Test
    void shouldFindEventById() {
        // Given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When
        Optional<Event> result = eventService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Corrida de São Paulo 2025", result.get().getName());
        verify(eventRepository).findById(1L);
    }

    @Test
    void shouldFindEventBySlug() {
        // Given
        when(eventRepository.findBySlug("corrida-de-sao-paulo-2025")).thenReturn(Optional.of(event));

        // When
        Optional<Event> result = eventService.findBySlug("corrida-de-sao-paulo-2025");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Corrida de São Paulo 2025", result.get().getName());
        verify(eventRepository).findBySlug("corrida-de-sao-paulo-2025");
    }

    @Test
    void shouldFindEventsByOrganizationId() {
        // Given
        List<Event> events = Arrays.asList(event);
        when(eventRepository.findByOrganizationId(1L)).thenReturn(events);

        // When
        List<Event> result = eventService.findByOrganizationId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(event, result.get(0));
        verify(eventRepository).findByOrganizationId(1L);
    }

    @Test
    void shouldUpdateEventSuccessfully() {
        // Given
        Event existingEvent = new Event();
        existingEvent.setId(1L);
        existingEvent.setSlug("old-slug");
        existingEvent.setOrganization(organization);

        Event updateData = new Event();
        updateData.setName("Updated Event Name");
        updateData.setDescription("Updated description");
        updateData.setSlug("updated-event-name");
        updateData.setOrganization(organization);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(eventRepository.existsBySlugAndIdNot(anyString(), any())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenReturn(updateData);

        // When
        Event result = eventService.update(1L, updateData);

        // Then
        assertNotNull(result);
        assertEquals("Updated Event Name", result.getName());
        verify(eventRepository).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentEvent() {
        // Given
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.update(99L, event));

        assertEquals("Evento não encontrado", exception.getMessage());
        verify(eventRepository).findById(99L);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldDeleteEventSuccessfully() {
        // Given
        when(eventRepository.existsById(1L)).thenReturn(true);

        // When
        eventService.delete(1L);

        // Then
        verify(eventRepository).existsById(1L);
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentEvent() {
        // Given
        when(eventRepository.existsById(99L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> eventService.delete(99L));

        assertEquals("Evento não encontrado", exception.getMessage());
        verify(eventRepository).existsById(99L);
        verify(eventRepository, never()).deleteById(any());
    }

    @Test
    void shouldFindPublishedEvents() {
        // Given
        List<Event> publishedEvents = Arrays.asList(event);
        when(eventRepository.findByStatus(Event.EventStatus.PUBLISHED)).thenReturn(publishedEvents);

        // When
        List<Event> result = eventService.findPublishedEvents();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findByStatus(Event.EventStatus.PUBLISHED);
    }

    @Test
    void shouldPublishEvent() {
        // Given
        event.setStatus(Event.EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            assertEquals(Event.EventStatus.PUBLISHED, saved.getStatus());
            return saved;
        });

        // When
        Event result = eventService.publishEvent(1L);

        // Then
        assertNotNull(result);
        verify(eventRepository).findById(1L);
        verify(eventRepository).save(event);
    }

    @Test
    void shouldGenerateUniqueSlugWhenDuplicateExists() {
        // Given
        event.setSlug(null);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(eventRepository.existsBySlug("corrida-de-sao-paulo-2025")).thenReturn(true);
        when(eventRepository.existsBySlug("corrida-de-sao-paulo-2025-1")).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            assertNotNull(saved.getSlug());
            assertTrue(saved.getSlug().equals("corrida-de-sao-paulo-2025-1"));
            return saved;
        });

        // When
        Event result = eventService.create(event);

        // Then
        verify(eventRepository).existsBySlug("corrida-de-sao-paulo-2025");
        verify(eventRepository).existsBySlug("corrida-de-sao-paulo-2025-1");
        verify(eventRepository).save(any(Event.class));
    }
}