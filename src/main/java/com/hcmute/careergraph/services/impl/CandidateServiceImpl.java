package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.candidate.AddressType;
import com.hcmute.careergraph.enums.candidate.ContactType;
import com.hcmute.careergraph.enums.common.FileType;
import com.hcmute.careergraph.enums.common.Status;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.mapper.CandidateEducationMapper;
import com.hcmute.careergraph.mapper.CandidateExperienceMapper;
import com.hcmute.careergraph.persistence.dtos.request.CandidateRequest;
import com.hcmute.careergraph.persistence.models.*;
import com.hcmute.careergraph.repositories.*;
import com.hcmute.careergraph.services.CandidateService;
import com.hcmute.careergraph.services.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

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
    private final CandidateEducationMapper  candidateEducationMapper;

    private final EducationRepository educationRepository;
    private final CandidateEducationRepository candidateEducationRepository;

    private static final int MAX = 20;
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;


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

        return null;
    }
    @Override
    @Transactional(readOnly = true)
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
        CandidateExperience candidateExperience = candidateExperienceMapper.toEntity(candidateRequest);
        if(candidateRequest.companyId() != null) {
                company = companyRepository.findById(candidateRequest.companyId()).get();
                candidateExperience.setCompany(company);
            }


        if(company == null){
            company = new Company();
            company.setName(candidateRequest.companyName());
            company.setStatus(Status.ACTIVE);
            companyRepository.save(company);

        }

        candidateExperience.setCompany(company);
        candidateExperience.setCandidate(candidate);
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
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        CandidateExperience candidateExperience = candidateExperienceRepository.findById(experienceId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidateExperience = candidateExperienceMapper.toUpdateEntity(candidateRequest, candidateExperience);
        if(candidateRequest.companyId() != null) {

            if(candidate.getExperiences().stream()
                    .anyMatch(
                            ex -> ex.getCompany().getId()
                                    .equals(candidateRequest.companyId())
                    )) {
                Company company = companyRepository.findById(candidateRequest.companyId()).get();
                candidateExperience.setCompany(company);
            }
        }else{
            Company company = new Company();
            company.setName(candidateRequest.companyName());
            company.setStatus(Status.ACTIVE);
            companyRepository.save(company);
            candidateExperience.setCompany(company);
        }
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

    @Override
    public Candidate addEducation(String candidateId, CandidateRequest.CandidateEducationRequest candidateRequest) throws ChangeSetPersister.NotFoundException {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        CandidateEducation candidateEducation = candidateEducationMapper.toEntity(candidateRequest);
        Education education=null;
        if(candidateRequest.universityId() != null) {
            if(!candidateRequest.universityId().equals(candidateEducation.getEducation().getId())) {
                education = educationRepository.findById(candidateRequest.universityId())
                        .orElseThrow(ChangeSetPersister.NotFoundException::new);
            }
        }
        if(education == null){
            education = new Education();
            education.setOfficialName(candidateRequest.officialName());
            education.setStatus(Status.ACTIVE);
            educationRepository.save(education);
            candidateEducation.setEducation(education);
        }
        candidateEducation.setCandidate(candidate);
        candidateEducation.setStatus(Status.ACTIVE);
        if(candidate.getExperiences()==null){
            Set<CandidateExperience> candidateExperiences = new HashSet<>();
            candidate.setExperiences(candidateExperiences);
        }
        candidate.getEducations().add(candidateEducation);

        return candidateRepository.save(candidate);
    }

    @Override
    public Candidate updateEducation(String candidateId, String educationId, CandidateRequest.CandidateEducationRequest candidateRequest) throws ChangeSetPersister.NotFoundException {
        CandidateEducation candidateEducation = candidateEducationRepository.findById(educationId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidateEducation = candidateEducationMapper.toEntity(candidateRequest,candidateEducation);
        if(candidateRequest.universityId() != null) {
            if(!candidateEducation.getEducation().getId().equals(candidateRequest.universityId())) {
                Education education = educationRepository.findById(candidateRequest.universityId()).get();
                candidateEducation.setEducation(education);
            }
        }else{
            Education education = new Education();
            education.setOfficialName(candidateRequest.officialName());
            education.setStatus(Status.ACTIVE);
            educationRepository.save(education);
            candidateEducation.setEducation(education);
        }
        candidateEducationRepository.save(candidateEducation);
        return candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }

    @Override
    public Candidate deleteEducation(String candidateId, String educationId) throws ChangeSetPersister.NotFoundException {
        CandidateEducation candidateEducation = candidateEducationRepository.findById(educationId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
        candidateEducation.softDelete();
        candidateEducationRepository.save(candidateEducation);
        return candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);
    }

    @Transactional
    @Override
    public Candidate replaceSkillsForUser(String candidateId, CandidateRequest.ReplaceSkillsRequest request)
            throws ChangeSetPersister.NotFoundException {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(ChangeSetPersister.NotFoundException::new);

        // Lấy collection managed (không copy)
        Set<CandidateSkill> managed = Optional.ofNullable(candidate.getSkills())
                .orElseGet(() -> {
                    Set<CandidateSkill> init = new HashSet<>();
                    candidate.setSkills(init);
                    return init;
                });

        // Nếu request rỗng => xoá sạch thông qua orphanRemoval
        if (request == null || request.getSkills() == null || request.getSkills().isEmpty()) {
            managed.clear();           // orphanRemoval sẽ DELETE từng row
            return candidate;
        }

        // Chuẩn hoá tên
        List<String> normalized = request.getSkills().stream()
                .filter(Objects::nonNull)
                .map(s -> s.replaceAll("\\s+", " ").trim())
                .filter(s -> !s.isEmpty())
                .map(s -> s.length() > 120 ? s.substring(0, 120) : s)
                .distinct()
                .toList();

        // 1) Xoá những skill KHÔNG còn trong normalized (chỉ thao tác collection)
        managed.removeIf(cs -> {
            String existName = cs.getSkill() != null ? cs.getSkill().getName() : null;
            return existName != null && normalized.stream().noneMatch(existName::equals);
        });
        // -> orphanRemoval tạo DELETE theo id cho những phần tử bị remove

        // 2) Tên cần thêm
        List<String> toAddNames = normalized.stream()
                .filter(name -> managed.stream()
                        .noneMatch(cs -> Objects.equals(cs.getSkill().getName(), name)))
                .toList();

        if (!toAddNames.isEmpty()) {
            List<Skill> existed = skillRepository.findByNameIn(toAddNames);
            Map<String, Skill> byExactName = existed.stream()
                    .filter(s -> s.getName() != null)
                    .collect(Collectors.toMap(Skill::getName, s -> s, (a, b) -> a, LinkedHashMap::new));

            for (String name : toAddNames) {
                byExactName.computeIfAbsent(name, n -> {
                    Skill created = new Skill();
                    created.setName(n);
                    return skillRepository.save(created);
                });
            }

            for (String name : toAddNames) {
                Skill skill = byExactName.get(name);
                CandidateSkill cs = new CandidateSkill();
                cs.setCandidate(candidate);
                cs.setSkill(skill);
                cs.setIsVerified(false);
                cs.setEndorsementCount(0);
                managed.add(cs); // add vào collection là đủ; cascade persist sẽ lo
            }
        }

        return candidate;
    }




}
