package com.hcmute.careergraph.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorType {
    UNAUTHORIZED(401, "Token invalid or expiry time", HttpStatus.UNAUTHORIZED),
    UNSAVED_DATA(501, "The process of saving the object is invalid", HttpStatus.INTERNAL_SERVER_ERROR),
    API_CALL_FAILED(502, "Call API fail", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    ErrorType(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    int code;
    String message;
    HttpStatusCode httpStatusCode;
}
