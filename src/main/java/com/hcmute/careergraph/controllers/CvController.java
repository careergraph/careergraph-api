package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.RenameFileRequest;
import com.hcmute.careergraph.persistence.dtos.response.FileResponse;
import com.hcmute.careergraph.services.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/cv")
@RequiredArgsConstructor
public class CvController {

    private final CandidateService candidateService;
    private final SecurityUtils securityUtils;

    @PatchMapping("/{id}/rename")
    public ResponseEntity<FileResponse> renameCv(
            @PathVariable("id") String fileId,
            @Valid @RequestBody RenameFileRequest request
    ) throws ChangeSetPersister.NotFoundException {
        String candidateId = securityUtils.getCandidateId()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        FileResponse response = candidateService.renameFile(candidateId, fileId, request.newName());
        return ResponseEntity.ok(response);
    }
}
