package com.hcmute.careergraph.persistence.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("related_jobs")
    private List<RelatedJobResponse> relatedJobs;

    public static class RelatedJobResponse {
        @JsonProperty("job_id")
        private String jobId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("company")
        private String company;

        @JsonProperty("location")
        private String location;

        @JsonProperty("salary")
        private String salary;

        @JsonProperty("description")
        private String description;

        @JsonProperty("requirements")
        private List<String> requirements;

        @JsonProperty("relevance_score")
        private Double relevanceScore;
    }
}
