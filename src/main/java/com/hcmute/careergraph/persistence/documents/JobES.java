package com.hcmute.careergraph.persistence.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "jobs_es")
public class JobES {

    private String id;
//    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

//    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

//    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    @Field(type = FieldType.Text, analyzer = "standard")
    private String state;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate expiredDate;
}
