package com.hcmute.careergraph.persistence.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch Document for Candidate - used for semantic search and fuzzy
 * matching
 * 
 * Mapping:
 * - desiredPosition: text với Vietnamese analyzer + keyword (cho exact match)
 * - skills: text array cho matching
 * - isOpenToWork: boolean filter
 * - embedding: dense_vector 768 dims cho semantic search
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "candidates_es")
@Setting(settingPath = "elasticsearch/candidates-es-settings.json")
@Mapping(mappingPath = "elasticsearch/candidates-es-mappings.json")
public class CandidateES {

  @Id
  private String id;

  /* ========= PERSONAL INFO ========= */
  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private String firstName;

  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private String lastName;

  @Field(type = FieldType.Keyword)
  private String email;

  @Field(type = FieldType.Keyword)
  private String phone;

  @Field(type = FieldType.Keyword)
  private String avatar;

  @Field(type = FieldType.Keyword)
  private String gender;

  @Field(type = FieldType.Integer)
  private Integer yearsOfExperience;

  /* ========= JOB CRITERIA - SEARCH TEXT ========= */
  @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vi_analyzer"), otherFields = {
      @InnerField(suffix = "keyword", type = FieldType.Keyword)
  })
  private String desiredPosition;

  @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vi_analyzer"), otherFields = {
      @InnerField(suffix = "keyword", type = FieldType.Keyword)
  })
  private String currentJobTitle;

  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private String summary;

  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private String resumeText;

  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private String cvKeywords;  // Extracted keywords từ CV (~300 chars, clean)

  @Field(type = FieldType.Keyword)
  private String resumeFileId;

  @Field(type = FieldType.Keyword)
  private String resumeFileName;

  @Field(type = FieldType.Keyword)
  private String resumeUrl;

  @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
  private LocalDateTime resumeUpdatedAt;

  @Field(type = FieldType.Keyword)
  private String resumeContentHash;

  /* ========= FILTER KEYWORDS ========= */
  @Field(type = FieldType.Boolean)
  private Boolean isOpenToWork;

  @Field(type = FieldType.Keyword)
  private String educationLevel;

  @Field(type = FieldType.Keyword)
  private String experienceLevel;

  @Field(type = FieldType.Keyword)
  private List<String> industries;

  @Field(type = FieldType.Keyword)
  private List<String> locations;

  @Field(type = FieldType.Keyword)
  private List<String> workTypes;

  @Field(type = FieldType.Integer)
  private Integer salaryExpectationMin;

  @Field(type = FieldType.Integer)
  private Integer salaryExpectationMax;

  /* ========= SKILLS - for matching ========= */
  @Field(type = FieldType.Text, analyzer = "vi_analyzer")
  private List<String> skills;

  /* ========= TIMESTAMPS ========= */
  @Field(type = FieldType.Date, format = DateFormat.date)
  private LocalDate createdAt;

  @Field(type = FieldType.Date, format = DateFormat.date)
  private LocalDate lastActive;

  @Field(type = FieldType.Keyword)
  private String contentHash;

  /* ========= VECTOR EMBEDDING ========= */
  @Field(type = FieldType.Dense_Vector, dims = 3072, index = true, similarity = "cosine")
  private float[] embedding;

  /**
   * Build search text for embedding generation
   * V2: Dùng cvKeywords thay vì raw resumeText để tập trung professional signals
   */
  public String buildSearchText() {
    StringBuilder sb = new StringBuilder();
    
    // Intent signals (ưu tiên cao — repeat 2x cho embedding weight)
    if (desiredPosition != null && !desiredPosition.isBlank()) {
      sb.append(desiredPosition).append(" ").append(desiredPosition).append(" ");
    }
    if (currentJobTitle != null && !currentJobTitle.isBlank()) {
      sb.append(currentJobTitle).append(" ");
    }
    if (skills != null && !skills.isEmpty()) {
      sb.append(String.join(" ", skills)).append(" ");
    }
    
    // CV keywords (clean, focused) thay vì raw resumeText
    if (cvKeywords != null && !cvKeywords.isBlank()) {
      sb.append(cvKeywords);
    } else if (resumeText != null && !resumeText.isBlank()) {
      // Fallback: chỉ 500 chars đầu nếu chưa có cvKeywords
      String snippet = resumeText.length() > 500 ? resumeText.substring(0, 500) : resumeText;
      sb.append(snippet);
    }
    
    return sb.toString().trim();
  }
}
