package com.hcmute.careergraph.enums.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EducationType {
  HIGH_SCHOOL("Trung học phổ thông"),
  ASSOCIATE_DEGREE("Cao đẳng"),
  ASSOCIATE("Cao đẳng"),
  BACHELORS_DEGREE("Đại học"),
  BACHELOR("Đại học"),
  MASTERS_DEGREE("Thạc sĩ"),
  MASTER("Thạc sĩ"),
  DOCTORATE("Tiến sĩ"),
  VOCATIONAL("Đào tạo nghề"),
  CERTIFICATION("Chứng chỉ chuyên môn"),
  OTHER("Không yêu cầu"),
  NONE("Không yêu cầu");

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
