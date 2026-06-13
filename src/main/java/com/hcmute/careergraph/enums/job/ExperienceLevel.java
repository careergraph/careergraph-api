package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExperienceLevel {
    ENTRY("Mới vào nghề"),
    INTERN("Thực tập sinh"),
    MIDDLE("Chuyên viên"),
    FRESHER("Mới tốt nghiệp"),
    JUNIOR("Nhân viên Junior"),
    SENIOR("Nhân viên Senior"),
    LEADER("Trưởng nhóm"),
    CTO("Giám đốc công nghệ"),
    CFO("Giám đốc tài chính");

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
