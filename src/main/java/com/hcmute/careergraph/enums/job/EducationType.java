package com.hcmute.careergraph.enums.job;

public enum EducationType {
    HIGH_SCHOOL("High School"),
    ASSOCIATE_DEGREE("Associate Degree"),
    BACHELORS_DEGREE("Bachelor's Degree"),
    MASTERS_DEGREE("Master's Degree"),
    DOCTORATE("Doctorate"),
    OTHER("Other");

    private final String label;

    EducationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
