package com.hcmute.careergraph.persistence.event;

/**
 * Sau khi lưu bản ghi {@code file} (upload Cloudinary). Listener async sẽ trích text CV.
 */
public record ResumeFilePersistedEvent(String fileId) {
}
