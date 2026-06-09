package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for candidate suggestion/search results
 * Optimized for listing candidates with essential info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSuggestionResponse {

  private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String phone;
  private String avatar;
  private String gender;

  // Job criteria
  private String desiredPosition;
  private String currentJobTitle;
  private String currentCompany;
  private Integer yearsOfExperience;
  private String experienceLevel;
  private String educationLevel;

  // Work preferences
  private List<String> industries;
  private List<String> locations;
  private List<String> workTypes;
  private Integer salaryExpectationMin;
  private Integer salaryExpectationMax;

  // Skills
  private List<String> skills;

  // Summary
  private String summary;

  // Status
  private Boolean isOpenToWork;
  private LocalDate lastActive;

  // Shared resume/profile links for HR actions
  private String resumeFileId;
  private String resumeFileName;
  private String resumeUrl;
  private String profileUrl;

  // Search relevance score (from Elasticsearch)
  private Float score;

  /**
   * Helper to get full name
   */
  public String getFullName() {
    if (firstName == null && lastName == null)
      return "";
    if (firstName == null)
      return lastName;
    if (lastName == null)
      return firstName;
    return firstName + " " + lastName;
  }
}
