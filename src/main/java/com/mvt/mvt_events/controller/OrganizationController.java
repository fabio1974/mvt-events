package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Organization;
import com.mvt.mvt_events.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Organization> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Organization get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization create(@RequestBody @Valid Organization payload) {
        return service.create(payload);
    }

    @PutMapping("/{id}")
    public Organization update(@PathVariable Long id, @RequestBody @Valid Organization payload) {
        return service.update(id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}