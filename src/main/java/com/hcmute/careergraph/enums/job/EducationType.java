package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EducationType {
  HIGH_SCHOOL("High School"),
  ASSOCIATE_DEGREE("Associate"),
  ASSOCIATE("Associate"),
  BACHELORS_DEGREE("Bachelor"),
  BACHELOR("Bachelor"),
  MASTERS_DEGREE("Master"),
  MASTER("Master"),
  DOCTORATE("Doctorate"),
  VOCATIONAL("Vocational"),
  CERTIFICATION("Certification"),
  OTHER("No Formal Education"),
  NONE("No Formal Education");

  private final String label;

  EducationType(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  public String getCode() {
    return switch (this) {
      case ASSOCIATE, ASSOCIATE_DEGREE -> "ASSOCIATE_DEGREE";
      case BACHELOR, BACHELORS_DEGREE -> "BACHELORS_DEGREE";
      case MASTER, MASTERS_DEGREE -> "MASTERS_DEGREE";
      case NONE, OTHER -> "OTHER";
      default -> name();
    };
  }

  @JsonCreator
  public static EducationType fromValue(String value) {
    if (value == null)
      return null;

    String normalized = value.trim().toUpperCase();
    // Canonicalize to DB-legacy values to satisfy existing check constraints
    if ("ASSOCIATE".equals(normalized) || "ASSOCIATE_DEGREE".equals(normalized))
      return ASSOCIATE_DEGREE;
    if ("BACHELOR".equals(normalized) || "BACHELORS_DEGREE".equals(normalized))
      return BACHELORS_DEGREE;
    if ("MASTER".equals(normalized) || "MASTERS_DEGREE".equals(normalized))
      return MASTERS_DEGREE;
    if ("NONE".equals(normalized) || "OTHER".equals(normalized))
      return OTHER;

    try {
      return EducationType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      for (EducationType type : EducationType.values()) {
        if (type.label.equalsIgnoreCase(value)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Invalid EducationType: " + value);
    }
  }
}
