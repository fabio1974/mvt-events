package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Athlete;
import com.mvt.mvt_events.service.AthleteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/athletes")
public class AthleteController {

    private final AthleteService service;

    public AthleteController(AthleteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Athlete> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Athlete get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/email/{email}")
    public Athlete getByEmail(@PathVariable String email) {
        return service.getByEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Athlete create(@RequestBody @Valid Athlete payload) {
        return service.create(payload);
    }

    @PutMapping("/{id}")
    public Athlete update(@PathVariable Long id, @RequestBody @Valid Athlete payload) {
        return service.update(id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}