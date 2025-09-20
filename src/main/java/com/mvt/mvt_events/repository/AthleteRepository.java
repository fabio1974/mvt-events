package com.mvt.mvt_events.repository;

import com.mvt.mvt_events.jpa.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    Optional<Athlete> findByEmail(String email);

    Optional<Athlete> findByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDocumentAndIdNot(String document, Long id);
}