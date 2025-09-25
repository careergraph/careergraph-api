package com.hcmute.careergraph.persistence.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {

    private String companyId;

    private String tagname;

    private String avatar;

    private String cover;

    private String size;

    private String website;

    private String ceoName;

    private int noOfMembers;

    private int yearFounded;

    private Set<JobDto> jobs;

    // private Set<ConnectionDto> companyConnections;
}
