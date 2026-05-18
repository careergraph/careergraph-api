package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmploymentType {
    FULL_TIME("Full-time"),
    PART_TIME("Part-time"),
    CONTRACT("Contract"),
    INTERNSHIP("Internship"),
    FREELANCE("Freelance"),
    TEMPORARY("Temporary");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    @JsonCreator
    public static EmploymentType fromValue(String value) {
        if (value == null)
            return null;
        try {
            return EmploymentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (EmploymentType type : EmploymentType.values()) {
                if (type.label.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid EmploymentType: " + value);
        }
    }
}
