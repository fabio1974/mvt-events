package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Athlete;
import com.mvt.mvt_events.repository.AthleteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private AthleteService athleteService;

    private Athlete athlete;

    @BeforeEach
    void setUp() {
        athlete = new Athlete();
        athlete.setId(1L);
        athlete.setName("Carlos Silva");
        athlete.setEmail("carlos.silva@email.com");
        athlete.setDocument("123.456.789-00");
        athlete.setPhone("+55 11 99999-1234");
        athlete.setDateOfBirth(LocalDate.of(1990, 5, 15));
        athlete.setGender(Athlete.Gender.MALE);
        athlete.setEmergencyContact("Maria Silva - +55 11 88888-1234");
        athlete.setAddress("Rua das Flores, 123 - São Paulo, SP");
    }

    @Test
    void shouldCreateAthleteSuccessfully() {
        // Given
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        when(athleteRepository.existsByDocument(anyString())).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenReturn(athlete);

        // When
        Athlete result = athleteService.create(athlete);

        // Then
        assertNotNull(result);
        assertEquals("Carlos Silva", result.getName());
        assertEquals("carlos.silva@email.com", result.getEmail());
        assertEquals("123.456.789-00", result.getDocument());

        verify(athleteRepository).existsByEmail("carlos.silva@email.com");
        verify(athleteRepository).existsByDocument("123.456.789-00");
        verify(athleteRepository).save(athlete);
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(athleteRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> athleteService.create(athlete));

        assertEquals("Já existe um atleta com este email", exception.getMessage());
        verify(athleteRepository).existsByEmail("carlos.silva@email.com");
        verify(athleteRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDocumentAlreadyExists() {
        // Given
        when(athleteRepository.existsByEmail(anyString())).thenReturn(false);
        when(athleteRepository.existsByDocument(anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> athleteService.create(athlete));

        assertEquals("Já existe um atleta com este documento", exception.getMessage());
        verify(athleteRepository).existsByEmail("carlos.silva@email.com");
        verify(athleteRepository).existsByDocument("123.456.789-00");
        verify(athleteRepository, never()).save(any());
    }

    @Test
    void shouldFindAllAthletes() {
        // Given
        List<Athlete> athletes = Arrays.asList(athlete, new Athlete());
        when(athleteRepository.findAll()).thenReturn(athletes);

        // When
        List<Athlete> result = athleteService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(athleteRepository).findAll();
    }

    @Test
    void shouldFindAthleteById() {
        // Given
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(athlete));

        // When
        Optional<Athlete> result = athleteService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Carlos Silva", result.get().getName());
        verify(athleteRepository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenAthleteNotFound() {
        // Given
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<Athlete> result = athleteService.findById(99L);

        // Then
        assertTrue(result.isEmpty());
        verify(athleteRepository).findById(99L);
    }

    @Test
    void shouldUpdateAthleteSuccessfully() {
        // Given
        Athlete existingAthlete = new Athlete();
        existingAthlete.setId(1L);
        existingAthlete.setEmail("old@email.com");
        existingAthlete.setDocument("000.000.000-00");

        when(athleteRepository.findById(1L)).thenReturn(Optional.of(existingAthlete));
        when(athleteRepository.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);
        when(athleteRepository.existsByDocumentAndIdNot(anyString(), any())).thenReturn(false);
        when(athleteRepository.save(any(Athlete.class))).thenReturn(athlete);

        // When
        Athlete result = athleteService.update(1L, athlete);

        // Then
        assertNotNull(result);
        assertEquals("Carlos Silva", result.getName());
        verify(athleteRepository).findById(1L);
        verify(athleteRepository).save(any(Athlete.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentAthlete() {
        // Given
        when(athleteRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> athleteService.update(99L, athlete));

        assertEquals("Atleta não encontrado", exception.getMessage());
        verify(athleteRepository).findById(99L);
        verify(athleteRepository, never()).save(any());
    }

    @Test
    void shouldDeleteAthleteSuccessfully() {
        // Given
        when(athleteRepository.existsById(1L)).thenReturn(true);

        // When
        athleteService.delete(1L);

        // Then
        verify(athleteRepository).existsById(1L);
        verify(athleteRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentAthlete() {
        // Given
        when(athleteRepository.existsById(99L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> athleteService.delete(99L));

        assertEquals("Atleta não encontrado", exception.getMessage());
        verify(athleteRepository).existsById(99L);
        verify(athleteRepository, never()).deleteById(any());
    }
}