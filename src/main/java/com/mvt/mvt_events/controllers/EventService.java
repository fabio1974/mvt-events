package com.mvt.mvt_events.controllers;

import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.repositories.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository repo;

    public EventService(EventRepository repo) {
        this.repo = repo;
    }

    public List<Event> list() {
        return repo.findAll();
    }

    public Event get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));
    }

    public Event create(Event e) {
        return repo.save(e);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }
}