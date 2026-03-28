package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.models.Account;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("accounts")
@RequiredArgsConstructor
public class AccountController {

    private final SecurityUtils securityUtils;

    @Builder
    public record AccountMeResponse(
            String id,
            String email,
            String role,
            String companyId,
            String candidateId
    ) {}

    @GetMapping("/me")
    public RestResponse<AccountMeResponse> getCurrentAccount() {
        Account account = securityUtils.getCurrentAccount()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        return RestResponse.<AccountMeResponse>builder()
                .status(HttpStatus.OK)
                .data(AccountMeResponse.builder()
                        .id(account.getId())
                        .email(account.getEmail())
                        .role(account.getRole() != null ? account.getRole().name() : null)
                        .companyId(account.getCompany() != null ? account.getCompany().getId() : null)
                        .candidateId(account.getCandidate() != null ? account.getCandidate().getId() : null)
                        .build())
                .build();
    }
}
