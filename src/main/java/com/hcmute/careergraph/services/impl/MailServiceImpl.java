package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.services.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
}
