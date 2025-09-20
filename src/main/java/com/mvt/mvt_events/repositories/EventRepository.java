package com.mvt.mvt_events.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mvt.mvt_events.jpa.Event;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}