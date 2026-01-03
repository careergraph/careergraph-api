package com.hcmute.careergraph.enums.common;

public enum PartyType {

    COMPANY("company"),
    CANDIDATE("candidate"),
    EDUCATION("education"),
    CORPORATE("corporate");

    private final String label;

    public boolean isCompany() {
        return this == PartyType.COMPANY;
    }

    public boolean isCandidate() {
        return this == PartyType.CANDIDATE;
    }

    public boolean isEducation() {
        return this == PartyType.EDUCATION;
    }

    public String getLabel() {
        return label;
    }

    PartyType(String label) {
        this.label = label;
    }

    public static PartyType fromLabel(String label) {
        for (PartyType t : PartyType.values()) {
            if (t.getLabel().equalsIgnoreCase(label)) {
                return t;
            }
        }
        return null;
    }
}

