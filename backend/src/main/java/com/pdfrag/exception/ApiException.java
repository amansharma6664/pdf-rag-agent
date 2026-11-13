package com.pdfrag.exception;

import org.springframework.http.HttpStatus;

/**
 * An error we want to show to the user with a specific HTTP status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
