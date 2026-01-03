package com.hcmute.careergraph.persistence.models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity lưu danh sách job mới đăng.
 * Khi HR tạo job → thêm vào bảng này.
 * Khi buildQueue() chạy → query ES với filter job IDs từ bảng này.
 * Sau khi sendDailyDigest() gửi xong cho tất cả candidates → xóa toàn bộ bảng
 * này.
 */
@Entity
@Table(name = "newly_posted_jobs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NewlyPostedJob extends BaseEntity {

  @Column(name = "job_id", nullable = false, unique = true)
  private String jobId;

  @Column(name = "posted_at")
  private LocalDateTime postedAt;
}
