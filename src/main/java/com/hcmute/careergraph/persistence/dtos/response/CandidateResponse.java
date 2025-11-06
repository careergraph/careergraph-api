package com.hcmute.careergraph.persistence.dtos.response;

import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record CandidateResponse(
        String candidateId,
        String email,
        String firstName,
        String lastName,
        String dateOfBirth,
        String gender,
        String currentJobTitle,
        String currentCompany,
        String industry,
        Integer yearsOfExperience,
        String workLocation,
        Boolean isOpenToWork,
        String summary,
        String resume,
        String tagname,
        String avatar,
        String cover,
        String currentPosition,
        String educationLevel,
        int noOfFollowers,
        int noOfFollowing,
        int noOfConnections,
        String desiredPosition,
        List<String> industries,
        List<String> locations,
        Integer salaryExpectationMin,
        Integer salaryExpectationMax,
        List<String> workTypes,
        Set<ContactResponse> contacts,
        Set<AddressResponse> addresses,
        Set<ConnectionResponse> connections,
        List<CandidateClientResponse.CandidateEducationResponse> educations,
        List<CandidateClientResponse.CandidateExperienceResponse> experiences,
        List<CandidateSkillResponse> skills,
        Set<ApplicationResponse> applications
) {
    public CandidateResponse {
        contacts = contacts != null ? contacts : Set.of();
        addresses = addresses != null ? addresses : Set.of();
        connections = connections != null ? connections : Set.of();
        educations = educations != null ? educations : List.of();
        experiences = experiences != null ? experiences : List.of();
        skills = skills != null ? skills : List.of();
        applications = applications != null ? applications : Set.of();
        industries = industries != null ? industries: List.of();
        locations = locations != null ? locations : List.of();
        workTypes  = workTypes != null ? workTypes: List.of();
    }
}
