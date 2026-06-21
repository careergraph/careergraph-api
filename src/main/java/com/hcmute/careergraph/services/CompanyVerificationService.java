package com.hcmute.careergraph.services;

import com.hcmute.careergraph.persistence.dtos.request.CompanyVerificationRequests;
import com.hcmute.careergraph.persistence.dtos.response.CompanyVerificationResponses;
import java.util.List;

public interface CompanyVerificationService {

    CompanyVerificationResponses.CompanyVerificationStatusResponse getMyVerification(String companyId);

    CompanyVerificationResponses.CompanyVerificationStatusResponse submitVerification(
            String companyId,
            String accountId,
            CompanyVerificationRequests.SubmitVerificationRequest request);

    CompanyVerificationResponses.CompanyVerificationStatusResponse updateVerification(
            String companyId,
            String requestId,
            String accountId,
            CompanyVerificationRequests.SubmitVerificationRequest request);

    List<CompanyVerificationResponses.VerificationRequestSummaryResponse> listMyVerificationRequests(String companyId);
}
