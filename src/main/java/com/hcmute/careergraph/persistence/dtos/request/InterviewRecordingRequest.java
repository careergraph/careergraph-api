package com.hcmute.careergraph.persistence.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRecordingRequest {

    @NotBlank(message = "File key is required")
    private String fileKey;

    private Long fileSize;

    private Integer durationSeconds;

    private String mimeType;

    private String roomParticipantId;
}
