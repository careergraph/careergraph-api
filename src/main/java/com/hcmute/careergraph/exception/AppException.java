package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.enums.ErrorType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {

    private ErrorType errorCode;

    public AppException(ErrorType errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
