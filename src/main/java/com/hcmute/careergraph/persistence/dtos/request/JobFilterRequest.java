package com.hcmute.careergraph.persistence.dtos.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EducationType;
import com.hcmute.careergraph.enums.job.EmploymentType;
import com.hcmute.careergraph.enums.job.ExperienceLevel;
import com.hcmute.careergraph.enums.job.JobCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobFilterRequest {

    private List<Status> statuses;

    /**
     * Tên tỉnh/thành (shortName), ví dụ: "Hà Nội", "Tuyên Quang".
     * {@code city} được giữ làm alias để tương thích client cũ.
     */
    @JsonAlias("city")
    private String location;

    /**
     * Slug URL-friendly, ví dụ: "ha-noi", "tuyen-quang".
     */
    private String provinceSlug;

    /**
     * Mã tỉnh/thành từ API địa giới hành chính, ví dụ: "01", "08".
     */
    private String provinceCode;

    private List<JobCategory> jobCategories;

    private List<EmploymentType> employmentTypes;

    private List<ExperienceLevel> experienceLevels;

    private List<EducationType> educationTypes;
}
