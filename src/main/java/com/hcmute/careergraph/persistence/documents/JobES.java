package com.hcmute.careergraph.persistence.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "jobs_es")
@Setting(settingPath = "elasticsearch/jobs-es-settings.json")
@Mapping(mappingPath = "elasticsearch/jobs-es-mappings.json")
public class JobES {

    @Id
    private String id;
    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
/* ========= SEARCH TEXT ========= */
//    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

//    @Field(type = FieldType.Text, analyzer = "standard")
    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private String description;

    /* ========= FILTER KEYWORDS ========= */
    @Field(type = FieldType.Keyword)
    private String status;

//    @Field(type = FieldType.Keyword)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vi_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String jobCategory;

    @Field(type = FieldType.Keyword)
    private String employmentType;

    @Field(type = FieldType.Keyword)
    private String experienceLevel;

    @Field(type = FieldType.Keyword)
    private String education;

//    @Field(type = FieldType.Keyword)
    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private String state;

    @Field(type = FieldType.Keyword)
    private String provinceSlug;

    @Field(type = FieldType.Keyword)
    private String provinceCode;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String companyId;

    @Field(type = FieldType.Keyword)
    private String companyVerificationStatus;

    @Field(type = FieldType.Keyword)
    private String companyOperationalStatus;

    @Field(type = FieldType.Boolean)
    private Boolean companyBlocked;

    @Field(type = FieldType.Boolean)
    private Boolean jobSearchable;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private List<String> qualifications;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private List<String> minimumQualifications;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private List<String> responsibilities;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private List<String> skills;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate createdAt;

    @Field(type = FieldType.Keyword)
    private String contentHash;

    /* ========= VECTOR ========= */
    @Field(
            type = FieldType.Dense_Vector,
            dims = 3072,
            index = true,
            similarity = "cosine"
    )
    private float[] embedding;
}
