package com.hcmute.careergraph.exception;

import com.hcmute.careergraph.enums.common.ErrorType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AppException extends RuntimeException {

    private ErrorType errorCode;
    private String errorMessage;
    private Map<String, Object> data;

    public AppException(ErrorType errorCode, String string) {
        this.setErrorMessage(string);
        this.errorCode = errorCode;
    }
    public AppException(ErrorType errorCode, String string, Map<String, Object> data) {
        this.setErrorMessage(string);
        this.errorCode = errorCode;
        this.data = data != null ? data : Map.of();
    }
}
