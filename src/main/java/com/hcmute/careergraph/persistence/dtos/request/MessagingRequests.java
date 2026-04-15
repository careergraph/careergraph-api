package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.message.MessageContentType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class MessagingRequests {

  @Data
  public static class CreateThreadRequest {
    private String candidateId;
    private String companyId;
    private String applicationId;
  }

  @Data
  public static class SendMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;

    private MessageContentType contentType;

    private String fileUrl;
    private String fileName;
    private Long fileSize;
  }

  @Data
  public static class BlockUserRequest {
    private String reason;
  }
}
