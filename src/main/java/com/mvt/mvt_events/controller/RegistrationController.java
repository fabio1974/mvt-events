package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.dto.MyRegistrationResponse;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.jpa.User;
import com.mvt.mvt_events.service.RegistrationService;
import com.mvt.mvt_events.service.RegistrationMapperService;
import com.mvt.mvt_events.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService service;
    private final UserService userService;
    private final RegistrationMapperService mapperService;

    public RegistrationController(RegistrationService service, UserService userService,
            RegistrationMapperService mapperService) {
        this.service = service;
        this.userService = userService;
        this.mapperService = mapperService;
    }

    @GetMapping
    public List<Registration> list() {
        return service.list();
    }

    @GetMapping("/my-registrations")
    public List<MyRegistrationResponse> getMyRegistrations(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername);
        List<Registration> registrations = service.findByUserId(currentUser.getId());
        return mapperService.toMyRegistrationResponse(registrations);
    }

    @GetMapping("/{id}")
    public Registration get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/event/{eventId}")
    public List<Registration> getByEventId(@PathVariable Long eventId) {
        return service.getByEventId(eventId);
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