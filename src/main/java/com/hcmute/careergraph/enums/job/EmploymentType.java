package com.hcmute.careergraph.enums.job;

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

    public String getLabel() {
        return label;
    }
}
