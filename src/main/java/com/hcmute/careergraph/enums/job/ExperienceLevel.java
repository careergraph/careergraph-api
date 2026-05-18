package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExperienceLevel {
    ENTRY("Entry"),
    INTERN("Intern"),
    MIDDLE("Middle"),
    FRESHER("Fresher"),
    JUNIOR("Junior"),
    SENIOR("Senior"),
    LEADER("Leader"),
    CTO("CTO"),
    CFO("CFO");

    private final String label;

    ExperienceLevel(String label) {
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
    public static ExperienceLevel fromValue(String value) {
        if (value == null)
            return null;
        try {
            return ExperienceLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (ExperienceLevel level : ExperienceLevel.values()) {
                if (level.label.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Invalid ExperienceLevel: " + value);
        }
    }
}
