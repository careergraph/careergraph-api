package com.hcmute.careergraph.services;

/**
 * Sàng lọc hồ sơ sau khi ứng viên nộp đơn (gọi AI, có thể chuyển REJECTED).
 */
public interface ApplicationAiScreeningService {

    void screenApplication(String applicationId);
}
