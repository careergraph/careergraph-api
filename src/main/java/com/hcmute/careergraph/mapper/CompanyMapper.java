package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.*;
import com.hcmute.careergraph.persistence.models.Company;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompanyMapper {

    /**
     * Convert Company Entity sang CompanyResponse DTO
     */
    public CompanyResponse toResponse(Company company) {
        if (company == null) {
            return null;
        }

        return CompanyResponse.builder()
                .companyId(company.getId())
                .name(company.getName())
                .tagname(company.getTagname())
                .avatar(company.getAvatar())
                .cover(company.getCover())
                .noOfFollowers(company.getNoOfFollowers())
                .noOfFollowing(company.getNoOfFollowing())
                .noOfConnections(company.getNoOfConnections())

                // Company-specific fields
                .size(company.getSize())
                .website(company.getWebsite())
                .ceoName(company.getCeoName())
                .noOfMembers(company.getNoOfMembers())
                .yearFounded(company.getYearFounded())

                // Nested objects mapping
                .contacts(toContactResponseSet(company.getContacts()))
                .addresses(toAddressResponseSet(company.getAddresses()))
                .companyConnections(toConnectionResponseSet(company.getCompanyConnections()))
                .jobs(toJobResponseSet(company.getJobs()))

                .build();
    }

    /**
     * Convert Set<Contact> sang Set<ContactResponse>
     * (Stub mapper - chỉnh sửa khi có ContactMapper)
     */
    private Set<ContactResponse> toContactResponseSet(Set<?> contacts) {
        return Collections.emptySet();
    }

    /**
     * Convert Set<Address> sang Set<AddressResponse>
     * (Stub mapper - chỉnh sửa khi có AddressMapper)
     */
    private Set<AddressResponse> toAddressResponseSet(Set<?> addresses) {
        return Collections.emptySet();
    }

    /**
     * Convert Set<Connection> sang Set<ConnectionResponse>
     * (Stub mapper - chỉnh sửa khi có ConnectionMapper)
     */
    private Set<ConnectionResponse> toConnectionResponseSet(Set<?> connections) {
        return Collections.emptySet();
    }

    /**
     * Convert Set<Job> sang Set<JobResponse>
     * (Stub mapper - chỉnh sửa khi có JobMapper)
     */
    private Set<JobResponse> toJobResponseSet(Set<?> jobs) {
        return Collections.emptySet();
    }

    /**
     * Convert list Company sang list CompanyResponse
     */
    public List<CompanyResponse> toResponseList(List<Company> companies) {
        if (companies == null || companies.isEmpty()) {
            return Collections.emptyList();
        }

        return companies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
