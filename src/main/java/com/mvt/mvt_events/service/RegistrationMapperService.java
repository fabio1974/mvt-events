package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.MyRegistrationResponse;
import com.mvt.mvt_events.jpa.Registration;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for mapping Registration entities to DTOs
 * to avoid circular references and provide clean API responses
 */
@Service
public class RegistrationMapperService {

    public List<MyRegistrationResponse> toMyRegistrationResponse(List<Registration> registrations) {
        return registrations.stream()
                .map(this::toMyRegistrationResponse)
                .collect(Collectors.toList());
    }

    public MyRegistrationResponse toMyRegistrationResponse(Registration registration) {
        MyRegistrationResponse response = new MyRegistrationResponse();
        response.setId(registration.getId());
        response.setRegistrationDate(registration.getRegistrationDate());
        response.setStatus(registration.getStatus());

        // Event summary
        if (registration.getEvent() != null) {
            MyRegistrationResponse.EventSummary eventSummary = new MyRegistrationResponse.EventSummary();
            eventSummary.setId(registration.getEvent().getId());
            eventSummary.setName(registration.getEvent().getName());
            eventSummary.setDescription(registration.getEvent().getDescription());
            eventSummary.setEventDate(registration.getEvent().getEventDate());
            eventSummary.setLocation(registration.getEvent().getLocation());
            eventSummary.setPrice(registration.getEvent().getPrice());
            response.setEvent(eventSummary);
        }

        // User summary
        if (registration.getUser() != null) {
            MyRegistrationResponse.UserSummary userSummary = new MyRegistrationResponse.UserSummary();
            userSummary.setId(registration.getUser().getId());
            userSummary.setName(registration.getUser().getName());
            response.setUser(userSummary);
        }

        // Category summary
        if (registration.getCategory() != null) {
            MyRegistrationResponse.CategorySummary categorySummary = new MyRegistrationResponse.CategorySummary();
            categorySummary.setId(registration.getCategory().getId());
            categorySummary.setName(registration.getCategory().getName());
            categorySummary.setDistance(registration.getCategory().getDistance());
            categorySummary.setGender(registration.getCategory().getGender() != null
                    ? registration.getCategory().getGender().getDisplayName()
                    : null);
            categorySummary.setMinAge(registration.getCategory().getMinAge());
            categorySummary.setMaxAge(registration.getCategory().getMaxAge());
            categorySummary.setPrice(registration.getCategory().getPrice());
            response.setCategory(categorySummary);
        }

        // Payments removido - não necessário neste endpoint

        return response;
    }
}