package com.hcmute.careergraph.persistence.models;


import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.PartyType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true, exclude = {})
@EqualsAndHashCode(callSuper = true, exclude = {})
@Entity
@Table(name = "file")
public class File extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type")
    private FileType fileType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "thumbnail")
    private String thumbnail;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "share_to_find_job")
    private Boolean shareToFindJob = false; // để kiểm tra là cv này có share để hr tìm không

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private PartyType ownerType;

    @Column (name = "owner_id")
    private String ownerId;

    @Column (name = "public_id")
    private String publicId;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "mime_type")
    private String mimeType;   // ví dụ application/pdf

    @Column(name = "resource_type")
    private String resourceType; // image / video / raw...

    @Column(name = "original_file_name")
    private String originalFileName;

    /**
     * Plain text trích từ file CV (PDF/DOCX), đã truncate để lưu DB — dùng cho AI match JD.
     */
    @Column(name = "resume_extracted_text", columnDefinition = "TEXT")
    private String resumeExtractedText;

    /**
     * Lỗi trích xuất gần nhất (nếu có); null khi thành công hoặc chưa chạy.
     */
    @Column(name = "resume_extraction_error", columnDefinition = "TEXT")
    private String resumeExtractionError;

    @Column(name = "resume_content_hash")
    private String resumeContentHash;

    /**
     * JSON chứa keywords được extract từ CV bởi AI (Gemini) hoặc null nếu chưa extract.
     * Format: {"jobTitle":"...","skills":[...],"searchKeywords":"..."}
     */
    @Column(name = "cv_keywords_json", columnDefinition = "TEXT")
    private String cvKeywordsJson;

    @Column(name = "cv_chunks_json", columnDefinition = "TEXT")
    private String cvChunksJson;

}
