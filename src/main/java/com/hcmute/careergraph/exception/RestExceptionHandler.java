package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.helper.RestResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String message = "Unsupported Content-Type: " + ex.getContentType();
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(RestResponse.builder().status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).message(message).data(null).build());
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    String message = "Malformed JSON request: " + ex.getMostSpecificCause().getMessage();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(RestResponse.builder().status(HttpStatus.BAD_REQUEST).message(message).data(null).build());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    String message = ex.getBindingResult().getFieldErrors().stream().findFirst().map(e -> e.getDefaultMessage())
        .orElse("Validation error");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(RestResponse.builder().status(HttpStatus.BAD_REQUEST).message(message).data(null).build());
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Object> handleAll(Exception ex) {
    String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(RestResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).message(message).data(null).build());
  }
}
