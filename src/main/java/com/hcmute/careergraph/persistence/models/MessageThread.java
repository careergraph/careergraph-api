package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_threads", uniqueConstraints = {
    @UniqueConstraint(name = "uk_message_thread_company_candidate", columnNames = { "company_id", "candidate_id" })
}, indexes = {
    @Index(name = "idx_message_thread_company", columnList = "company_id"),
    @Index(name = "idx_message_thread_candidate", columnList = "candidate_id"),
    @Index(name = "idx_message_thread_last_message", columnList = "last_message_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "company", "candidate", "application" })
@EqualsAndHashCode(callSuper = true, exclude = { "company", "candidate", "application" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageThread extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "candidate_id", nullable = false)
  private Candidate candidate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  private Application application;

  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  @Column(name = "last_message_preview", length = 255)
  private String lastMessagePreview;

  @Column(name = "archived_by_company", nullable = false)
  private boolean archivedByCompany;

  @Column(name = "archived_by_company_at")
  private LocalDateTime archivedByCompanyAt;

  @Column(name = "archived_by_candidate", nullable = false)
  private boolean archivedByCandidate;

  @Column(name = "archived_by_candidate_at")
  private LocalDateTime archivedByCandidateAt;
}
