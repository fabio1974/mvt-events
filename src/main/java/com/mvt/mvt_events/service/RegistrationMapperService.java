package com.mvt.mvt_events.service;

import com.mvt.mvt_events.dto.MyRegistrationResponse;
import com.mvt.mvt_events.jpa.Registration;
import com.mvt.mvt_events.jpa.Payment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
            eventSummary.setStartsAt(registration.getEvent().getStartsAt());
            eventSummary.setEventDate(registration.getEvent().getEventDate());
            eventSummary.setEventTime(registration.getEvent().getEventTime());
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

        // Payments summary
        if (registration.getPayments() != null) {
            List<MyRegistrationResponse.PaymentSummary> paymentSummaries = registration.getPayments().stream()
                    .map(this::toPaymentSummary)
                    .collect(Collectors.toList());
            response.setPayments(paymentSummaries);
        }

        return response;
    }

    private MyRegistrationResponse.PaymentSummary toPaymentSummary(Payment payment) {
        MyRegistrationResponse.PaymentSummary summary = new MyRegistrationResponse.PaymentSummary();
        summary.setId(payment.getId());
        summary.setAmount(payment.getAmount());
        summary.setPaymentMethod(payment.getPaymentMethod().getDisplayName());
        summary.setStatus(payment.getStatus().name());
        summary.setCreatedAt(payment.getCreatedAt());
        return summary;
    }
}