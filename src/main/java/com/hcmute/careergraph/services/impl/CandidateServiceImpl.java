package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.candidate.AddressType;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CandidateExperienceMapper;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.CandidateExperienceRepository;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.repositories.CompanyRepository;
import com.hcmute.careergraph.services.CandidateService;
import com.hcmute.careergraph.services.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final S3StorageService storageService;

    private final CandidateRepository candidateRepository;
    private final SecurityUtils securityUtils;

    private final CompanyRepository companyRepository;
    private final CandidateExperienceMapper candidateExperienceMapper;

    private final CandidateExperienceRepository candidateExperienceRepository;


    @Override
    public String updateResource(String candidateId, MultipartFile file, FileType fileType) {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new InternalException("Candidate not found"));

        // Check permission
        if (!securityUtils.getCandidateId().get().equals(candidateId)) {
            throw new InternalException("You do not have permission to update this candidate");
        }
        S3StorageService.StoredFile storedFile;
        try {
            storedFile = storageService.uploadCandidateFile(candidateId, fileType, file);
        } catch (Exception e) {
            throw new InternalException("Unable to upload candidate resource");
        }

        String objectKey = storedFile.key();

        if (fileType == FileType.RESUME) {
            List<String> resumes = candidate.getResumes();
            if (resumes == null) {
                resumes = new java.util.ArrayList<>();
            }
            resumes.add(objectKey);
            candidate.setResumes(resumes);
        }
        if (fileType == FileType.AVATAR) {
            candidate.setAvatar(objectKey);
        }
        if (fileType == FileType.COVER) {
            candidate.setCover(objectKey);
        }

        candidateRepository.save(candidate);

        return storedFile.url();
    }

    @Override
    public String getResource(String candidateId, FileType fileType)
            throws ChangeSetPersister.NotFoundException {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        // Check permission
        if (!securityUtils.getCandidateId().get().equals(candidateId)) {
            throw new InternalException("You do not have permission to get this this resource");
        }

        String objectKey = switch (fileType) {
            case AVATAR -> candidate.getAvatar();
            case COVER  -> candidate.getCover();
            case RESUME -> {
                List<String> resumes = candidate.getResumes();
                if (resumes == null || resumes.isEmpty()) {
                    throw new ChangeSetPersister.NotFoundException();
                }
                yield resumes.getFirst();
            }
        };

        if (objectKey == null)
            throw new ChangeSetPersister.NotFoundException();

        return storageService.getFileUrl(objectKey);
    }
    @Override
    public Candidate getMyProfile(String candidateId) throws ChangeSetPersister.NotFoundException {
        return candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }

    @Override
    public Candidate updateInformation(String candidateId, CandidateRequest.UpdateInformationRequest candidateRequest) throws ChangeSetPersister.NotFoundException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        // ----- Basic fields -----
        candidate.setFirstName(candidateRequest.firstName());
        candidate.setLastName(candidateRequest.lastName());
        candidate.setDateOfBirth(candidateRequest.dateOfBirth());
        candidate.setGender(candidateRequest.gender());
        candidate.setIsMarried(candidateRequest.isMarried());

        Set<Address> addresses = candidate.getAddresses();
        Address homeAddress = addresses.stream()
                .filter(a -> a != null && a.getAddressType() != null)
                .filter(a -> AddressType.HOME_ADDRESS.name().equals(a.getAddressType().name()))
                .findFirst()
                .orElse(null);

        if(homeAddress == null){
            homeAddress = new Address();
            homeAddress.setName(AddressType.HOME_ADDRESS.name());
            homeAddress.setAddressType(AddressType.HOME_ADDRESS);
            homeAddress.setParty(candidate);
            addresses.add(homeAddress);
        }

        CandidateRequest.AddressDTO adr = candidateRequest.address();
        if (adr != null) {
            homeAddress.setCountry(adr.country());
            homeAddress.setProvince(adr.province());
            homeAddress.setDistrict(adr.district());
            homeAddress.setWard(adr.ward());
            homeAddress.setIsPrimary(Boolean.TRUE.equals(adr.isPrimary()));
        }


        Set<Contact> contacts = candidate.getContacts();

        CandidateRequest.ContactDTO contactReq = candidateRequest.contact();
        if (contactReq != null && "PHONE".equalsIgnoreCase(contactReq.type())) {
            Contact primaryPhone = contacts.stream()
                    .filter(c -> c.getContactType() == ContactType.PHONE && Boolean.TRUE.equals(c.getIsPrimary()))
                    .findFirst()
                    .orElse(null);

            if (primaryPhone == null) {
                primaryPhone = new Contact();
                primaryPhone.setParty(candidate);
                primaryPhone.setContactType(ContactType.PHONE);
                contacts.add(primaryPhone);
            }

            primaryPhone.setValue(contactReq.value());
            primaryPhone.setIsPrimary(Boolean.TRUE.equals(contactReq.isPrimary()));
            // giữ verified như cũ, đừng tự set true ở đây trừ khi BE cho phép
        }
        candidateRepository.save(candidate);
        return candidate;
    }

    @Override
    public Candidate updateJobFindCriteriaInfo(String candidateId, CandidateRequest.UpdateJobCriteriaRequest candidateRequest) throws ChangeSetPersister.NotFoundException {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);

        candidate.setDesiredPosition(candidateRequest.desiredPosition());
        candidate.setIndustries(candidateRequest.industries());
        candidate.setWorkTypes(candidateRequest.workTypes());
        candidate.setSalaryExpectationMin(candidateRequest.salaryExpectationMin());
        candidate.setSalaryExpectationMax(candidateRequest.salaryExpectationMax());
        candidate.setLocations(candidateRequest.locations());
        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate updateGeneralInfo(String candidateId, CandidateRequest.UpdateGeneralInfo candidateRequest) throws ChangeSetPersister.NotFoundException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidate.setYearsOfExperience(candidateRequest.yearsOfExperience());
        candidate.setEducationLevel(candidateRequest.educationLevel());
        candidate.setCurrentPosition(candidate.getCurrentPosition());
        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate addExperience(String candidateId, CandidateRequest.CandidateExperienceRequest candidateRequest) throws ChangeSetPersister.NotFoundException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        Company company=null;
        if(candidateRequest.companyId() != null) {
            company = companyRepository.findById(candidateRequest.companyId())
                    .orElseThrow(ChangeSetPersister.NotFoundException::new);
        }
        if(company == null){
            company = new Company();
            company.setName(candidateRequest.companyName());
            company.setStatus(Status.ACTIVE);
            companyRepository.save(company);

        }
        CandidateExperience candidateExperience = candidateExperienceMapper.toEntity(candidateRequest);
        candidateExperience.setCompany(company);
        candidateExperience.setCandidate( candidate );
        candidateExperience.setStatus(Status.ACTIVE);
        if(candidate.getExperiences()==null){
            Set<CandidateExperience> candidateExperiences = new HashSet<>();
            candidate.setExperiences(candidateExperiences);
        }
        candidate.getExperiences().add(candidateExperience);

        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate updateExperience(String candidateId, String experienceId, CandidateRequest.CandidateExperienceRequest candidateRequest) throws ChangeSetPersister.NotFoundException {
        CandidateExperience candidateExperience = candidateExperienceRepository.findById(experienceId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidateExperience = candidateExperienceMapper.toUpdateEntity(candidateRequest, candidateExperience);
        candidateExperienceRepository.save(candidateExperience);
        return candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }

    @Override
    public Candidate deleteExperience(String candidateId, String experienceId) throws ChangeSetPersister.NotFoundException {
        CandidateExperience candidateExperience = candidateExperienceRepository.findById(experienceId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidateExperience.softDelete();
        candidateExperienceRepository.save(candidateExperience);
        return candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }


}
