package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.persistence.dtos.response.*;
import com.hcmute.careergraph.persistence.models.Address;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.persistence.models.Contact;
import com.hcmute.careergraph.persistence.models.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompanyMapper {

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private AddressMaper addressMaper;

    @Autowired
    private ContactMapper contactMapper;

    /**
     * Convert Company Entity sang CompanyResponse DTO
     */
    public CompanyResponse toResponse(Company company, boolean isDetail) {
        if (company == null) {
            return null;
        }

        Set<ContactResponse> contacts = new HashSet<>();
        Set<AddressResponse> addresses = new HashSet<>();
        Set<JobResponse> jobs = new HashSet<>();

        if (isDetail) {
            jobs = toJobResponseSet(company.getJobs());
            addresses = toAddressResponseSet(company.getAddresses());
            contacts = toContactResponseSet(company.getContacts());
        }

        // Extract account info (role, email) if available
        String role = null;
        String email = null;
        if (company.getAccount() != null) {
            role = company.getAccount().getRole() != null
                    ? company.getAccount().getRole().name()
                    : null;
            email = company.getAccount().getEmail();
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

                // Account info
                .role(role)
                .email(email)

                // Company-specific fields
                .size(company.getSize())
                .website(company.getWebsite())
                .ceoName(company.getCeoName())
                .noOfMembers(company.getNoOfMembers())
                .yearFounded(company.getYearFounded())

                // Nested objects mapping
                .contacts(contacts)
                .addresses(addresses)
                .companyConnections(toConnectionResponseSet(company.getCompanyConnections()))
                .jobs(jobs)

                .build();
    }

    /**
     * Convert Set<Contact> sang Set<ContactResponse>
     * (Stub mapper - chỉnh sửa khi có ContactMapper)
     */
    private Set<ContactResponse> toContactResponseSet(Set<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return Collections.emptySet();
        }

        return contacts.stream()
                .map(contact -> contactMapper.toResponse(contact))
                .collect(Collectors.toSet());
    }

    /**
     * Convert Set<Address> sang Set<AddressResponse>
     * (Stub mapper - chỉnh sửa khi có AddressMapper)
     */
    private Set<AddressResponse> toAddressResponseSet(Set<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptySet();
        }

        return addresses.stream()
                .map(address -> addressMaper.toResponse(address))
                .collect(Collectors.toSet());
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
    private Set<JobResponse> toJobResponseSet(Set<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptySet();
        }

        return jobs.stream()
                .map(job -> jobMapper.toResponse(job))
                .collect(Collectors.toSet());
    }

    /**
     * Convert list Company sang list CompanyResponse
     */
    public List<CompanyResponse> toResponseList(List<Company> companies, boolean idDetail) {
        if (companies == null || companies.isEmpty()) {
            return Collections.emptyList();
        }

        return companies.stream()
                .map(company -> toResponse(company, idDetail))
                .collect(Collectors.toList());
    }
}
