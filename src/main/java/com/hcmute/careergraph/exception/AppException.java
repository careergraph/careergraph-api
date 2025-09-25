package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.enums.ErrorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {

    private ErrorType errorCode;
    private String errorMessage;

    public AppException(ErrorType errorCode, String string) {
        this.setErrorMessage(string);
        this.errorCode = errorCode;
    }
}
