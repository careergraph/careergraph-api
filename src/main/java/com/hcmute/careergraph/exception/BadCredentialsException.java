package com.hcmute.careergraph.exception;

public class BadCredentialsException extends RuntimeException{

   private final Object data;

   public BadCredentialsException(String message, Object data) {
      super(message);
      this.data = data;
   }

   public Object getData() {
      return data;
   }
}
