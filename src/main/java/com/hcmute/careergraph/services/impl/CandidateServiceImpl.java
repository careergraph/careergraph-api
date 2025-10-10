package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.enums.FileType;
import com.hcmute.careergraph.helper.StringHelper;
import com.hcmute.careergraph.mapper.CandidateMapper;
import com.hcmute.careergraph.persistence.dtos.response.CandidateDto;
import com.hcmute.careergraph.persistence.models.Candidate;
import com.hcmute.careergraph.repositories.CandidateRepository;
import com.hcmute.careergraph.services.CandidateService;
import com.hcmute.careergraph.services.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final MinioService minioService;

    private final CandidateRepository candidateRepository;

    private final CandidateMapper candidateMapper;
    private final SecurityUtils securityUtils;

    @Value("${integration.minio.bucket}")
    private String bucketName;

    @Override
    public String updateResource(String candidateId, MultipartFile file, FileType fileType) {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new InternalException("Candidate not found"));

        // Check permission
        if (!securityUtils.getCandidateId().get().equals(candidateId)) {
            throw new InternalException("You do not have permission to update this candidate");
        }

        // Update minio
        String objectName = StringHelper.buildObjectName(candidateId, fileType, file.getOriginalFilename());
        minioService.uploadFile(objectName, file);

        // Update DB
        switch (fileType) {
            case AVATAR -> candidate.setAvatar(objectName);
            case COVER  -> candidate.setCover(objectName);
            case RESUME -> candidate.setResumes(List.of(objectName));
        }
        candidateRepository.save(candidate);

        return objectName;
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
            case RESUME -> candidate.getResumes().get(0);
        };

        if (objectKey == null)
            throw new ChangeSetPersister.NotFoundException();

        return minioService.getFileUrl(objectKey);
    }
    @Override
    public CandidateDto getMyProfile() throws ChangeSetPersister.NotFoundException {
        String candidateId =  securityUtils.getCandidateId().get();
        Candidate candidate = candidateRepository.findById(candidateId).
                orElseThrow(ChangeSetPersister.NotFoundException::new);
        return candidateMapper.toDto(candidate);

    }
}
