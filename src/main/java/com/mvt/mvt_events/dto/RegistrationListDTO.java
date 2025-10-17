package com.mvt.mvt_events.dto;

import com.mvt.mvt_events.jpa.Registration;

import java.time.LocalDateTime;

/**
 * DTO leve para listagem de registrations
 * Evita carregar entidades completas com lazy loading
 */
public class RegistrationListDTO {

    private Long id;
    private LocalDateTime registrationDate;
    private Registration.RegistrationStatus status;
    private String notes;

    // Dados essenciais do User
    private String userId;
    private String userName;
    private String userUsername;

    // Dados essenciais do Event
    private Long eventId;
    private String eventName;
    private LocalDateTime eventDate;

    // Dados essenciais da Category
    private Long categoryId;
    private String categoryName;

    public RegistrationListDTO() {
    }

    // Construtor usado para conversão manual no Service
    public RegistrationListDTO(Long id, LocalDateTime registrationDate,
            Registration.RegistrationStatus status, String notes,
            String userId, String userName, String userUsername,
            Long eventId, String eventName, LocalDateTime eventDate,
            Long categoryId, String categoryName) {
        this.id = id;
        this.registrationDate = registrationDate;
        this.status = status;
        this.notes = notes;
        this.userId = userId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Registration.RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(Registration.RegistrationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
