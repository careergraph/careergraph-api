package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.enums.ErrorType;
import com.hcmute.careergraph.helper.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(value = Exception.class)
    ResponseEntity<RestResponse> handlingRuntimeException(RuntimeException exception) {
        log.error("Exception: ", exception);
        RestResponse restResponse = new RestResponse();

        restResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        restResponse.setMessage(exception.getMessage());

        return ResponseEntity.internalServerError().body(restResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<RestResponse> handlingAppException(AppException exception) {
        ErrorType errorCode = exception.getErrorCode();
        RestResponse restResponse = new RestResponse();

        restResponse.setCode(errorCode.getCode());
        restResponse.setMessage(exception.getErrorMessage());

        return ResponseEntity.status(errorCode.getCode()).body(restResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<RestResponse> handlingAccessDeniedException(AccessDeniedException exception) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.builder()
                        .code(HttpStatus.UNAUTHORIZED.value())
                        .message(exception.getMessage())
                        .build());
    }
}
