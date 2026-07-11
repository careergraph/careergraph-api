package com.hcmute.careergraph.services;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.interview.InterviewType;

import java.time.LocalDateTime;

public interface MailService {

    void sendOtp(String toEmail, String otp);

    void sendOtp(String toEmail, String otp, String recipientLabel, String platformName);

    void sendApplicationStageUpdateEmail(String toEmail,
                                         String candidateName,
                                         String jobTitle,
                                         String companyName,
                                         ApplicationStage stage,
                                         String stageLabel,
                                         boolean terminal,
                                         String note);

    void sendInterviewScheduleEmail(String toEmail,
                                    String candidateName,
                                    String jobTitle,
                                    String companyName,
                                    LocalDateTime scheduledAt,
                                    Integer durationMinutes,
                                    InterviewType interviewType,
                                    String location,
                                    String interviewRoomUrl,
                                    String note,
                                    boolean rescheduled);

    void sendInterviewCancellationEmail(String toEmail,
                                        String candidateName,
                                        String jobTitle,
                                        String companyName,
                                        LocalDateTime scheduledAt,
                                        InterviewType interviewType,
                                        String cancellationReason);

    void sendHtml(String to, String subject, String html);

    void sendHtmlSync(String to, String subject, String html);
}
