package com.hcmute.careergraph.exception;


import org.springframework.http.HttpStatus;

public class AuthenticationException extends RuntimeException {
    private final HttpStatus code;

    public AuthenticationException(HttpStatus code) {
        super(code.toString());
        this.code = code;
    }

    public AuthenticationException(String message, HttpStatus code) {
        super(message);
        this.code = code;
    }

    public AuthenticationException(String message, Throwable cause, HttpStatus code) {
        super(message, cause);
        this.code = code;
    }

    public HttpStatus getCode() {
        return code;
    }

}
