package com.hcmute.careergraph.persistence.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {

    private String candidateId;

    private String firstName;

    private String lastName;

    private String dateOfBirth;

    private String gender;

    private String currentJobTitle;

    private String currentCompany;

    private String industry;

    private Integer yearsOfExperience;

    private String workLocation;

    private Boolean isOpenToWork;

    private String summary;

    private String resume;

    private String tagname;

    private String avatar;

    private String cover;

    private int noOfFollowers;

    private int noOfFollowing;

    private int noOfConnections;

    private Set<ContactDto> contacts = new HashSet<>();

    private Set<AddressDto> addresses = new HashSet<>();

    private Set<ConnectionDto> connections;

    private Set<CandidateEducationDto> educations;

    private Set<CandidateExperienceDto> experiences;

    private Set<CandidateSkillDto> skills;

    private Set<ApplicationDto> applications;
}
