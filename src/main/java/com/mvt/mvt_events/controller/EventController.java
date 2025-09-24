package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.EventCreateRequest;
import com.mvt.mvt_events.jpa.Event;
import com.mvt.mvt_events.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @GetMapping
    public List<Event> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Event get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/organization/{organizationId}")
    public List<Event> getByOrganizationId(@PathVariable Long organizationId) {
        return service.findByOrganizationId(organizationId);
    }

    @GetMapping("/public")
    public List<Event> getPublicEvents() {
        return service.findPublishedEvents();
    }

    @GetMapping("/public/{id}")
    public Event getPublicEventById(@PathVariable Long id) {
        return service.findPublishedEventById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Event create(@RequestBody @Valid EventCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody @Valid Event payload) {
        return service.update(id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}