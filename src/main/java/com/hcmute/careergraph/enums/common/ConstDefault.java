package com.hcmute.careergraph.enums.common;

import lombok.Getter;

@Getter
public enum ConstDefault {
    EMPTY_STRING("");

    private final String value;

    ConstDefault(String value) {
        this.value = value;
    }
}
