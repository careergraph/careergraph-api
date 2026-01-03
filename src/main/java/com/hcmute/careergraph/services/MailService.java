package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import org.springframework.scheduling.annotation.Async;

public interface MailService {

    void sendOtp(String toEmail, String otp);

    void sendApplicationStageUpdateEmail(String toEmail,
                                         String candidateName,
                                         String jobTitle,
                                         String companyName,
                                         ApplicationStage stage,
                                         String note);

    @Async
    void sendHtml(String to, String subject, String html);
}


