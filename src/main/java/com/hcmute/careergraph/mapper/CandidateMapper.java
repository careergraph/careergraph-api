package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.enums.candidate.AddressType;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.persistence.dtos.response.CandidateClientResponse;
import com.hcmute.careergraph.persistence.dtos.response.CandidateResponse;
import com.hcmute.careergraph.persistence.models.Address;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.persistence.models.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CandidateMapper {

    @Autowired
    private AddressMaper addressMaper;
    @Autowired
    private ContactMapper contactMapper;
    @Autowired
    private CandidateSkillMapper candidateSkillMapper;
    @Autowired
    private CandidateExperienceMapper candidateExperienceMapper;

    @Autowired
    private CandidateEducationMapper candidateEducationMapper;

    public CandidateResponse toResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        return CandidateResponse.builder()
                .candidateId(candidate.getId())
                .email(candidate.getAccount() != null ? candidate.getAccount().getEmail() : null)
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .dateOfBirth(candidate.getDateOfBirth())
                .gender(candidate.getGender())
                .currentJobTitle(candidate.getCurrentJobTitle())
                .currentCompany(candidate.getCurrentCompany())
                .industry(candidate.getIndustry())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .workLocation(candidate.getWorkLocation())
                .isOpenToWork(candidate.getIsOpenToWork())
                .summary(candidate.getSummary())
                .resume(primaryResume(candidate.getResumes()))
                .tagname(candidate.getTagname())
                .avatar(candidate.getAvatar())
                .cover(candidate.getCover())
                .noOfFollowers(candidate.getNoOfFollowers())
                .noOfFollowing(candidate.getNoOfFollowing())
                .noOfConnections(candidate.getNoOfConnections())
                .currentPosition(candidate.getCurrentPosition())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .educationLevel(candidate.getEducationLevel())
                .desiredPosition(candidate.getDesiredPosition())
                .industries(
                        candidate.getIndustries() != null
                                ? candidate.getIndustries()
                                : List.of())
                .locations(
                        candidate.getLocations() != null
                                ? candidate.getLocations()
                                : List.of())
                .salaryExpectationMax(candidate.getSalaryExpectationMax())
                .salaryExpectationMin(candidate.getSalaryExpectationMin())
                .workTypes(
                        candidate.getWorkTypes() != null
                                ? candidate.getWorkTypes()
                                : List.of()
                )
                .addresses(addressMaper.toResponses(candidate.getAddresses()))
                .contacts(contactMapper.toResponses(candidate.getContacts()))
                .skills(candidateSkillMapper.toResponseList(candidate.getSkills()))
                .experiences(candidateExperienceMapper.toResponses(candidate.getExperiences()))
                .educations(candidateEducationMapper.toResponses(candidate.getEducations()))
                .build();


    }
    private List<CandidateClientResponse.CandidateExperienceResponse> sort(Set<CandidateClientResponse.CandidateExperienceResponse> candidateExperiences) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        return candidateExperiences.stream()
                .sorted(Comparator.comparing(
                        (CandidateClientResponse.CandidateExperienceResponse exp) -> YearMonth.parse(exp.startDate(), formatter)
                ).reversed())
                .collect(Collectors.toList());
    }

    private String primaryResume(List<String> resumes) {
        if (resumes == null || resumes.isEmpty()) {
            return null;
        }
        return resumes.get(0);
    }

    public CandidateClientResponse.CandidateJobCriteriaResponse toJobCriteriaResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        return CandidateClientResponse.CandidateJobCriteriaResponse.builder()
                .desiredPosition(candidate.getDesiredPosition())
                .industries(
                        candidate.getIndustries() != null
                        ? candidate.getIndustries()
                        : List.of())
                .locations(
                        candidate.getLocations() != null
                        ? candidate.getLocations()
                        : List.of())
                .salaryExpectationMax(candidate.getSalaryExpectationMax())
                .salaryExpectationMin(candidate.getSalaryExpectationMin())
                .workTypes(
                        candidate.getWorkTypes() != null
                                ? candidate.getWorkTypes()
                                : List.of()
                )
                .build();
    }


    public CandidateClientResponse.GeneralInfoResponse toGeneralInfoResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        return CandidateClientResponse.GeneralInfoResponse.builder()
                .currentPosition(candidate.getCurrentPosition())
                .yearsOfExperience(candidate.getYearsOfExperience())
                .educationLevel(candidate.getEducationLevel())
                .build();
    }





    // -------------------------------
    // Helpers
    // -------------------------------

    /**
     * Ưu tiên contact type PHONE và isPrimary = true.
     * Nếu có nhiều, chọn verified=true trước. Nếu vẫn nhiều, lấy thằng tạo sớm nhất/ bất kỳ.
     */
    private Optional<Contact> findPrimaryPhone(Candidate candidate) {
        Set<Contact> contacts = getContactsSafe(candidate);

        return contacts.stream()
                .filter(c -> c.getContactType() == ContactType.PHONE) // enum ContactType.PHONE
                .sorted((a, b) -> {
                    // sort để ưu tiên primary trước, verified trước
                    int primaryCompare = boolDesc(a.getIsPrimary()).compareTo(boolDesc(b.getIsPrimary()));
                    if (primaryCompare != 0) return primaryCompare;

                    int verifiedCompare = boolDesc(a.getVerified()).compareTo(boolDesc(b.getVerified()));
                    if (verifiedCompare != 0) return verifiedCompare;

                    // fallback: có thể so sánh createdAt nếu BaseEntity có trường đó
                    // hoặc cứ return 0
                    return 0;
                })
                .findFirst();
    }

    /**
     * Nếu bạn muốn fallback email qua Contact type EMAIL.
     */
    private Optional<String> findPrimaryEmail(Candidate candidate) {
        Set<Contact> contacts = getContactsSafe(candidate);

        return contacts.stream()
                .filter(c -> c.getContactType() == ContactType.EMAIL)
                .sorted((a, b) -> {
                    int primaryCompare = boolDesc(a.getIsPrimary()).compareTo(boolDesc(b.getIsPrimary()));
                    if (primaryCompare != 0) return primaryCompare;

                    int verifiedCompare = boolDesc(a.getVerified()).compareTo(boolDesc(b.getVerified()));
                    if (verifiedCompare != 0) return verifiedCompare;

                    return 0;
                })
                .map(Contact::getValue)
                .findFirst();
    }

    /**
     * Ưu tiên:
     * - Address.name == HOME_ADDRESS
     * - nếu không có, thì address.isPrimary == true
     * - nếu vẫn nhiều, lấy cái đầu.
     */
    private Optional<Address> findPrimaryAddress(Candidate candidate) {
        Set<Address> addresses = getAddressesSafe(candidate);

        // 1. tìm HOME_ADDRESS trước
        Optional<Address> homeAddress = addresses.stream()
                .filter(a -> AddressType.HOME_ADDRESS.name().equalsIgnoreCase(a.getName()))
                .findFirst();
        if (homeAddress.isPresent()) return homeAddress;

        // 2. fallback: isPrimary == true
        Optional<Address> primaryAddr = addresses.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsPrimary()))
                .findFirst();
        if (primaryAddr.isPresent()) return primaryAddr;

        // 3. fallback: first whatever
        return addresses.stream().findFirst();
    }

    private CandidateClientResponse.ContactDTO mapContactToDTO(Contact contact) {
        if (contact == null) return null;
        return CandidateClientResponse.ContactDTO.builder()
                .type(contact.getContactType() != null ? contact.getContactType().name() : null)
                .value(contact.getValue())
                .verified(contact.getVerified())
                .isPrimary(contact.getIsPrimary())
                .build();
    }

    private CandidateClientResponse.AddressDTO mapAddressToDTO(Address address) {
        if (address == null) return null;
        return CandidateClientResponse.AddressDTO.builder()
                .country(address.getCountry())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .isPrimary(address.getIsPrimary())
                .build();
    }

    // -------------------------------
    // Small utils
    // -------------------------------

    // Lấy contacts từ Candidate -> Party
    private Set<Contact> getContactsSafe(Candidate candidate) {
        // giả định Candidate kế thừa Party nên có getContacts()
        // nếu Party là lớp cha với field contacts thì gọi candidate.getContacts()
        // nếu tên khác thì đổi ở đây
        if (candidate.getContacts() == null) {
            return Set.of();
        }
        return candidate.getContacts();
    }

    private Set<Address> getAddressesSafe(Candidate candidate) {
        if (candidate.getAddresses() == null) {
            return Set.of();
        }
        return candidate.getAddresses();
    }

    // sort helper: true > false
    private Integer boolDesc(Boolean b) {
        // true -> 1, false/null -> 0
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }

    public CandidateClientResponse.CandidateProfileResponse toProfileResponse(Candidate candidate) {
        if (candidate == null) {
            return null;
        }
        // ----- lấy email -----
        // Ưu tiên từ Account nếu có, fallback qua Contact type EMAIL primary (nếu bạn muốn)
        String email = null;
        if (candidate.getAccount() != null && candidate.getAccount().getEmail() != null) {
            email = candidate.getAccount().getEmail();
        } else {
            email = findPrimaryEmail(candidate).orElse(null);
        }

        // ----- contact chính (ví dụ số điện thoại) -----
        CandidateClientResponse.ContactDTO primaryContactDto = findPrimaryPhone(candidate)
                .map(this::mapContactToDTO)
                .orElse(null);

        // ----- địa chỉ chính (HOME_ADDRESS hoặc isPrimary == true) -----
        CandidateClientResponse.AddressDTO primaryAddressDto = findPrimaryAddress(candidate)
                .map(this::mapAddressToDTO)
                .orElse(null);

        return CandidateClientResponse.CandidateProfileResponse.builder()
                .candidateId(candidate.getId())                 // giả sử Party/Candidate có getId()
                .firstName(candidate.getFirstName())
                .lastName(candidate.getLastName())
                .email(email)
                .gender(candidate.getGender())
                .dateOfBirth(candidate.getDateOfBirth())
                .isMarried(candidate.getIsMarried())
                .addresses(addressMaper.toResponses(candidate.getAddresses()))
                .contacts(contactMapper.toResponses(candidate.getContacts()))
                .build();
    }
}
