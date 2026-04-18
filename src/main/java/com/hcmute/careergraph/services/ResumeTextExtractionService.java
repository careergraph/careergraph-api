package com.hcmute.careergraph.services;

/**
 * Trích text từ file CV đã upload (Cloudinary URL) và lưu vào {@link com.hcmute.careergraph.persistence.models.File}.
 */
public interface ResumeTextExtractionService {

    void extractAndPersistByFileId(String fileId);

    /**
     * Tìm bản ghi file theo owner + URL; nếu chưa có text thì trích và lưu.
     */
    void extractAndPersistForCandidateResumeUrl(String candidateId, String resumeUrl);
}
