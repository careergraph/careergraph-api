package com.hcmute.careergraph.enums;

public enum PartyType {

    COMPANY, CANDIDATE, EDUCATION
    ;

    public boolean isCompany() {
        return this == PartyType.COMPANY;
    }

    public boolean isCandidate() {
        return this == PartyType.CANDIDATE;
    }

    public boolean isEducation() {
        return this == PartyType.EDUCATION;
    }

}
