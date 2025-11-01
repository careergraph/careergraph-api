package com.hcmute.careergraph.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * This exception is thrown in case of a not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

   @Serial
   private static final long serialVersionUID = -1126699074574529145L;

   public NotFoundException(String message) {
      super(message);
   }
   public NotFoundException() {super();}
}
