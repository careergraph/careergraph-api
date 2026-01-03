package com.hcmute.careergraph.persistence.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for searching/filtering candidates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateFilterRequest {

  /**
   * Search keyword - matches against desiredPosition, currentJobTitle, skills
   */
  private String keyword;

  /**
   * Filter by education levels (e.g., BACHELOR, MASTER, PHD)
   */
  private List<String> educationLevels;

  /**
   * Filter by experience levels (e.g., ENTRY, MID, SENIOR)
   */
  private List<String> experienceLevels;

  /**
   * Filter by industries
   */
  private List<String> industries;

  /**
   * Filter by work locations
   */
  private List<String> locations;

  /**
   * Filter by work types (e.g., FULL_TIME, PART_TIME, REMOTE)
   */
  private List<String> workTypes;

  /**
   * Filter by minimum years of experience
   */
  private Integer minYearsOfExperience;

  /**
   * Filter by maximum years of experience
   */
  private Integer maxYearsOfExperience;

  /**
   * Filter by salary range min
   */
  private Integer salaryMin;

  /**
   * Filter by salary range max
   */
  private Integer salaryMax;

  /**
   * Filter by specific skills
   */
  private List<String> skills;
}
