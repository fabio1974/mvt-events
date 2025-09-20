package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Tech Conference Brasil");
        organization.setContactEmail("contato@techconf.com.br");
        organization.setPhone("+55 11 99999-9999");
        organization.setDescription("Organizadora de eventos tech");
        organization.setWebsite("https://techconf.com.br");
        organization.setSlug("tech-conference-brasil");
    }

    @Test
    void shouldCreateOrganizationSuccessfully() {
        // Given
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        // When
        Organization result = organizationService.create(organization);

        // Then
        assertNotNull(result);
        assertEquals("Tech Conference Brasil", result.getName());
        assertEquals("contato@techconf.com.br", result.getContactEmail());
        assertEquals("tech-conference-brasil", result.getSlug());

        verify(organizationRepository).existsBySlug(anyString());
        verify(organizationRepository).save(organization);
    }

    @Test
    void shouldGenerateSlugAutomaticallyFromName() {
        // Given
        organization.setSlug(null); // Slug não definido
        when(organizationRepository.existsBySlug(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            assertNotNull(saved.getSlug());
            assertTrue(saved.getSlug().contains("tech-conference-brasil"));
            return saved;
        });

        // When
        Organization result = organizationService.create(organization);

        // Then
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void shouldThrowExceptionWhenSlugAlreadyExists() {
        // Given
        when(organizationRepository.existsBySlug(anyString())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> organizationService.create(organization));

        assertEquals("Já existe uma organização com este slug", exception.getMessage());
        verify(organizationRepository).existsBySlug(anyString());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldFindAllOrganizations() {
        // Given
        List<Organization> organizations = Arrays.asList(organization, new Organization());
        when(organizationRepository.findAll()).thenReturn(organizations);

        // When
        List<Organization> result = organizationService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(organizationRepository).findAll();
    }

    @Test
    void shouldFindOrganizationById() {
        // Given
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));

        // When
        Optional<Organization> result = organizationService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Tech Conference Brasil", result.get().getName());
        verify(organizationRepository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenOrganizationNotFound() {
        // Given
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<Organization> result = organizationService.findById(99L);

        // Then
        assertTrue(result.isEmpty());
        verify(organizationRepository).findById(99L);
    }

    @Test
    void shouldFindOrganizationBySlug() {
        // Given
        when(organizationRepository.findBySlug("tech-conference-brasil")).thenReturn(Optional.of(organization));

        // When
        Optional<Organization> result = organizationService.findBySlug("tech-conference-brasil");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Tech Conference Brasil", result.get().getName());
        verify(organizationRepository).findBySlug("tech-conference-brasil");
    }

    @Test
    void shouldUpdateOrganizationSuccessfully() {
        // Given
        Organization existingOrg = new Organization();
        existingOrg.setId(1L);
        existingOrg.setSlug("old-slug");

        Organization updateData = new Organization();
        updateData.setName("Updated Tech Conference");
        updateData.setContactEmail("new@techconf.com.br");
        updateData.setSlug("updated-tech-conference");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.existsBySlugAndIdNot(anyString(), any())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(updateData);

        // When
        Organization result = organizationService.update(1L, updateData);

        // Then
        assertNotNull(result);
        assertEquals("Updated Tech Conference", result.getName());
        verify(organizationRepository).findById(1L);
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentOrganization() {
        // Given
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> organizationService.update(99L, organization));

        assertEquals("Organização não encontrada", exception.getMessage());
        verify(organizationRepository).findById(99L);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithExistingSlug() {
        // Given
        Organization existingOrg = new Organization();
        existingOrg.setId(1L);
        existingOrg.setSlug("old-slug");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.existsBySlugAndIdNot(anyString(), any())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> organizationService.update(1L, organization));

        assertEquals("Já existe uma organização com este slug", exception.getMessage());
        verify(organizationRepository).existsBySlugAndIdNot(anyString(), any());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void shouldDeleteOrganizationSuccessfully() {
        // Given
        when(organizationRepository.existsById(1L)).thenReturn(true);

        // When
        organizationService.delete(1L);

        // Then
        verify(organizationRepository).existsById(1L);
        verify(organizationRepository).deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentOrganization() {
        // Given
        when(organizationRepository.existsById(99L)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> organizationService.delete(99L));

        assertEquals("Organização não encontrada", exception.getMessage());
        verify(organizationRepository).existsById(99L);
        verify(organizationRepository, never()).deleteById(any());
    }

    @Test
    void shouldGenerateUniqueSlugWhenDuplicateExists() {
        // Given
        organization.setSlug(null);
        when(organizationRepository.existsBySlug("tech-conference-brasil")).thenReturn(true);
        when(organizationRepository.existsBySlug("tech-conference-brasil-1")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization saved = invocation.getArgument(0);
            assertNotNull(saved.getSlug());
            assertTrue(saved.getSlug().equals("tech-conference-brasil-1"));
            return saved;
        });

        // When
        Organization result = organizationService.create(organization);

        // Then
        verify(organizationRepository).existsBySlug("tech-conference-brasil");
        verify(organizationRepository).existsBySlug("tech-conference-brasil-1");
        verify(organizationRepository).save(any(Organization.class));
    }
}