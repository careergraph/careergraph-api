package com.hcmute.careergraph.persistence.event;

/**
 * Fired after a new job application is persisted and the transaction commits.
 * Dùng để chạy sàng lọc AI bất đồng bộ (không chặn response nộp đơn).
 */
public record ApplicationCreatedEvent(String applicationId) {
}
