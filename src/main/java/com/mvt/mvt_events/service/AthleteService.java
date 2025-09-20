package com.mvt.mvt_events.service;

import com.mvt.mvt_events.jpa.Athlete;
import com.mvt.mvt_events.repository.AthleteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AthleteService {

    private final AthleteRepository repository;

    public AthleteService(AthleteRepository repository) {
        this.repository = repository;
    }

    public Athlete create(Athlete athlete) {
        // Validate unique constraints
        if (repository.existsByEmail(athlete.getEmail())) {
            throw new IllegalArgumentException("Já existe um atleta com este email");
        }

        if (athlete.getDocument() != null && repository.existsByDocument(athlete.getDocument())) {
            throw new IllegalArgumentException("Já existe um atleta com este documento");
        }

        return repository.save(athlete);
    }

    public List<Athlete> findAll() {
        return repository.findAll();
    }

    public Optional<Athlete> findById(Long id) {
        return repository.findById(id);
    }

    public Athlete update(Long id, Athlete athleteData) {
        Athlete existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atleta não encontrado"));

        // Validate unique constraints for update
        if (athleteData.getEmail() != null && !athleteData.getEmail().equals(existing.getEmail())) {
            if (repository.existsByEmailAndIdNot(athleteData.getEmail(), id)) {
                throw new IllegalArgumentException("Já existe um atleta com este email");
            }
        }

        if (athleteData.getDocument() != null && !athleteData.getDocument().equals(existing.getDocument())) {
            if (repository.existsByDocumentAndIdNot(athleteData.getDocument(), id)) {
                throw new IllegalArgumentException("Já existe um atleta com este documento");
            }
        }

        // Update fields
        if (athleteData.getName() != null)
            existing.setName(athleteData.getName());
        if (athleteData.getEmail() != null)
            existing.setEmail(athleteData.getEmail());
        if (athleteData.getPhone() != null)
            existing.setPhone(athleteData.getPhone());
        if (athleteData.getDocument() != null)
            existing.setDocument(athleteData.getDocument());
        if (athleteData.getDateOfBirth() != null)
            existing.setDateOfBirth(athleteData.getDateOfBirth());
        if (athleteData.getGender() != null)
            existing.setGender(athleteData.getGender());
        if (athleteData.getEmergencyContact() != null)
            existing.setEmergencyContact(athleteData.getEmergencyContact());
        if (athleteData.getAddress() != null)
            existing.setAddress(athleteData.getAddress());

        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Atleta não encontrado");
        }
        repository.deleteById(id);
    }

    // Legacy methods for compatibility
    public List<Athlete> list() {
        return findAll();
    }

    public Athlete get(Long id) {
        return findById(id)
                .orElseThrow(() -> new RuntimeException("Athlete not found with id: " + id));
    }

    public Athlete getByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Athlete not found with email: " + email));
    }
}