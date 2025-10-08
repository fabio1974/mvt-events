package com.mvt.mvt_events.exception;

/**
 * Exception thrown when trying to register with an email that already exists
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public EmailAlreadyExistsException(String email, String field) {
        super(String.format("%s '%s' já está cadastrado no sistema", field, email));
    }
}
