package com.hcmute.careergraph.enums.work;

public enum JobFunction {
    ENGINEERING("Engineering"),
    DESIGN("Design"),
    PRODUCT("Product"),
    MARKETING("Marketing");

    private final String label;

    JobFunction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
