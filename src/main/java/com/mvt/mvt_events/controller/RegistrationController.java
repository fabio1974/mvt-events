package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService service;

    public RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<Registration> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Registration get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/event/{eventId}")
    public List<Registration> getByEventId(@PathVariable Long eventId) {
        return service.getByEventId(eventId);
    }

    @GetMapping("/athlete/{athleteId}")
    public List<Registration> getByAthleteId(@PathVariable Long athleteId) {
        return service.getByAthleteId(athleteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Registration create(@RequestBody @Valid Registration payload) {
        return service.create(payload);
    }

    @PutMapping("/{id}")
    public Registration update(@PathVariable Long id, @RequestBody @Valid Registration payload) {
        return service.update(id, payload);
    }

    @PatchMapping("/{id}/payment-status")
    public Registration updatePaymentStatus(@PathVariable Long id,
            @RequestParam Registration.PaymentStatus paymentStatus) {
        return service.updatePaymentStatus(id, paymentStatus);
    }

    @PatchMapping("/{id}/status")
    public Registration updateStatus(@PathVariable Long id,
            @RequestParam Registration.RegistrationStatus status) {
        return service.updateStatus(id, status);
    }

    @PatchMapping("/{id}/cancel")
    public void cancelRegistration(@PathVariable Long id) {
        service.cancelRegistration(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}