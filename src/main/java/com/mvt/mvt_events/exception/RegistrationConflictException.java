package com.mvt.mvt_events.exception;

/**
 * Exception thrown when a user tries to register for an event
 * but is already registered for that event.
 */
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException(String message) {
        super(message);
    }

    public RegistrationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}