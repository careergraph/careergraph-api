package com.hcmute.careergraph.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * Exception thrown when access is forbidden
 * 
 * This exception is used when a user tries to access a resource
 * they don't have permission to access.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
