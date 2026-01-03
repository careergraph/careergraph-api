package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.candidate.SearchType;
import com.hcmute.careergraph.helper.JsonUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_search_history")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"candidate", "education"})
@EqualsAndHashCode(callSuper = true, exclude = {"candidate", "education"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateSearchHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    @Convert(converter = JsonUtils.StringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", columnDefinition = "TEXT")
    private List<String> filters = new ArrayList<>();

    // Id of job selected of user into job
    @Column(name = "selected_job_id")
    private String selectedJobId;

    // Embedding vector for keyword (use to RAG)
    // @Column(name = "embedding", columnDefinition = "vector(1536)")
    // private float[] embedding;

    @Column(name = "search_type")
    @Enumerated(EnumType.STRING)
    private SearchType searchType;
}
