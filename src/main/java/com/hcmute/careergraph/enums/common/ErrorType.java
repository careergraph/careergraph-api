package com.hcmute.careergraph.enums.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorType {

    // --- AUTHENTICATION & AUTHORIZATION ---
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, HttpStatus.FORBIDDEN),

    // --- CLIENT ERRORS ---
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST),
    NOT_FOUND(404, HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(409, HttpStatus.CONFLICT),
    VALIDATION_FAILED(422, HttpStatus.UNPROCESSABLE_ENTITY),

    // --- SERVER ERRORS ---
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR),
    UNSAVED_DATA(501, HttpStatus.INTERNAL_SERVER_ERROR),
    API_CALL_FAILED(502, HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(503, HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT(504, HttpStatus.GATEWAY_TIMEOUT),
    UNVERIFIED(505, HttpStatus.FORBIDDEN),

    ;

    int code;
    HttpStatusCode httpStatusCode;

    ErrorType(int code, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.httpStatusCode = httpStatusCode;
    }
}

