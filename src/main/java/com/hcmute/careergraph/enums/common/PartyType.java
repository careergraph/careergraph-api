package com.hcmute.careergraph.enums.common;

public enum PartyType {

    COMPANY, CANDIDATE, EDUCATION, CORPORATE
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
