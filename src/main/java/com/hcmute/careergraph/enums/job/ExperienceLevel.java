package com.hcmute.careergraph.enums.job;

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

    public String getLabel() {
        return label;
    }
}
