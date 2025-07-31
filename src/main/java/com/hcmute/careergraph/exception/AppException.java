package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.enums.EErrorCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {

    private EErrorCode errorCode;

    public AppException(EErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
