package com.hcmute.careergraph.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.application.ApplicationStage;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "company_recruitment_stages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "stage"})
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true, exclude = {"company"})
@EqualsAndHashCode(callSuper = true, exclude = {"company"})
public class CompanyRecruitmentStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private ApplicationStage stage;

    @Column(name = "label")
    private String label;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
