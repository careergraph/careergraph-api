package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.services.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    @Async
    public void sendOtp(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Confirm your OTP code");

            Context context = new Context();
            context.setVariable("username", "Candidate");
            context.setVariable("otp", otp);
            context.setVariable("companyName", "CareerGraph");
            context.setVariable("logoUrl", "https://link-to-logo.png");
            context.setVariable("date", LocalDate.now().toString());
            context.setVariable("supportEmail", "support@careergraph.com");
            context.setVariable("helpCenterUrl", "https://careergraph.com/help");
            context.setVariable("year", Year.now().getValue());
            context.setVariable("companyAddress", "123 Street, City, Country");

            String htmlContent = templateEngine.process("otp-template", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    @Async
    public void sendApplicationStageUpdateEmail(String toEmail,
                                                String candidateName,
                                                String jobTitle,
                                                String companyName,
                                                ApplicationStage stage,
                                                String note) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(String.format("%s | Application update", stage.getLabel()));

            Context context = new Context();
            context.setVariable("candidateName", candidateName);
            context.setVariable("jobTitle", jobTitle);
            context.setVariable("companyName", companyName);
            context.setVariable("stageLabel", stage.getLabel());
            context.setVariable("stageKey", stage.name());
            context.setVariable("note", note);
            context.setVariable("terminal", stage.isTerminal());
            context.setVariable("changedDate", LocalDate.now());
            context.setVariable("supportEmail", "support@careergraph.com");
            context.setVariable("helpCenterUrl", "https://careergraph.com/help");
            context.setVariable("year", Year.now().getValue());

            String htmlContent = templateEngine.process("application-stage-update", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send application stage update email", e);
        }
    }
}
