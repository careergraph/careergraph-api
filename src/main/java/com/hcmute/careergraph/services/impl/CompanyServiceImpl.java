package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.exception.NotFoundException;
import com.hcmute.careergraph.enums.candidate.AddressType;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.persistence.dtos.request.CompanyRequests;
import com.hcmute.careergraph.persistence.models.Address;
import com.hcmute.careergraph.persistence.models.Contact;
import com.hcmute.careergraph.persistence.models.Company;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyById(String companyId) {

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    @Override
    public Company updateMyProfile(String companyId, CompanyRequests.UpdateMyProfileRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (request.name() != null) {
            company.setName(request.name().trim());
        }
        if (request.ceoName() != null) {
            company.setCeoName(request.ceoName().trim());
        }
        if (request.website() != null) {
            company.setWebsite(request.website().trim());
        }
        if (request.size() != null) {
            company.setSize(request.size().trim());
        }
        if (request.noOfMembers() != null) {
            company.setNoOfMembers(request.noOfMembers());
        }
        if (request.yearFounded() != null) {
            company.setYearFounded(request.yearFounded());
        }
        if (request.avatar() != null) {
            company.setAvatar(request.avatar().trim());
        }
        if (request.cover() != null) {
            company.setCover(request.cover().trim());
        }

        if (request.contact() != null && request.contact().value() != null) {
            ContactType contactType = "EMAIL".equalsIgnoreCase(request.contact().type())
                    ? ContactType.EMAIL
                    : ContactType.PHONE;

            Contact primaryContact = company.getContacts().stream()
                    .filter(c -> c.getContactType() == contactType && Boolean.TRUE.equals(c.getIsPrimary()))
                    .findFirst()
                    .orElse(null);

            if (primaryContact == null) {
                primaryContact = new Contact();
                primaryContact.setParty(company);
                primaryContact.setContactType(contactType);
                company.getContacts().add(primaryContact);
            }

            primaryContact.setValue(request.contact().value().trim());
            primaryContact.setIsPrimary(Boolean.TRUE.equals(request.contact().isPrimary()));
            if (primaryContact.getVerified() == null) {
                primaryContact.setVerified(false);
            }
        }

        if (request.address() != null) {
            Address primaryAddress = company.getAddresses().stream()
                    .filter(a -> a.getAddressType() == AddressType.HEADQUARTERS && Boolean.TRUE.equals(a.getIsPrimary()))
                    .findFirst()
                    .orElse(null);

            if (primaryAddress == null) {
                primaryAddress = new Address();
                primaryAddress.setParty(company);
                primaryAddress.setAddressType(AddressType.HEADQUARTERS);
                primaryAddress.setName(AddressType.HEADQUARTERS.name());
                company.getAddresses().add(primaryAddress);
            }

            primaryAddress.setCountry(request.address().country());
            primaryAddress.setProvince(request.address().province());
            primaryAddress.setDistrict(request.address().district());
            primaryAddress.setWard(request.address().ward());
            primaryAddress.setIsPrimary(Boolean.TRUE.equals(request.address().isPrimary()));
        }

        return companyRepository.save(company);
    }

    @Override
    public List<HashMap<String, String>> lookup(String query) {
        List<Object[]> raw = companyRepository.lookup(query);
        List<HashMap<String, String>> result = new ArrayList<>();
        for (Object[] row : raw) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(row[0]));
            map.put("name", String.valueOf(row[1]));
            result.add(map);
        }
        return result;
    }
}
