package com.hcmute.careergraph.persistence.dtos.request;

import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.enums.job.EmploymentType;
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
public class JobFilterRequest {

    private List<Status> statuses;

    private List<JobCategory> jobCategories;

    private List<EmploymentType> employmentTypes;
}
