package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.enums.application.ApplicationStage;
import com.hcmute.careergraph.enums.interview.InterviewType;
import com.hcmute.careergraph.services.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
    private static final String PLATFORM_NAME = "CareerGraph";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${support.email:support@careergraph.vn}")
    private String supportEmail;

    @Value("${application.web.help-center-url:}")
    private String helpCenterUrl;

    @Value("${application.web.company-address:1 Vo Van Ngan, Thu Duc, TP. Ho Chi Minh, Viet Nam}")
    private String companyAddress;

    @Override
    @Async("mailTaskExecutor")
    public void sendOtp(String toEmail, String otp) {
        sendOtp(toEmail, otp, "Ứng viên", PLATFORM_NAME);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendOtp(String toEmail, String otp, String recipientLabel, String platformName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            String normalizedPlatformName = StringUtils.hasText(platformName) ? platformName.trim() : PLATFORM_NAME;
            helper.setSubject(normalizedPlatformName + " | Mã xác thực OTP");

            Context context = new Context();
            context.setVariable("username", StringUtils.hasText(recipientLabel) ? recipientLabel.trim() : "Người dùng");
            context.setVariable("otp", otp);
            context.setVariable("companyName", normalizedPlatformName);
            context.setVariable("logoUrl", null);
            context.setVariable("date", LocalDate.now().format(DATE_FORMATTER));
            applySupportContext(context);
            context.setVariable("year", Year.now().getValue());
            context.setVariable("companyAddress", companyAddress);

            String htmlContent = templateEngine.process("otp-template", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
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
            helper.setSubject(String.format("CareerGraph | Cập nhật hồ sơ: %s", stage.getLabel()));

            Context context = new Context();
            context.setVariable("candidateName", candidateName);
            context.setVariable("jobTitle", jobTitle);
            context.setVariable("companyName", companyName);
            context.setVariable("stageLabel", stage.getLabel());
            context.setVariable("stageKey", stage.name());
            context.setVariable("note", note);
            context.setVariable("terminal", stage.isTerminal());
            context.setVariable("changedDate", LocalDate.now().format(DATE_FORMATTER));
            applySupportContext(context);
            context.setVariable("year", Year.now().getValue());
            context.setVariable("companyLogo", null);

            String htmlContent = templateEngine.process("application-stage-update", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send application stage update email", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendInterviewScheduleEmail(String toEmail,
                                           String candidateName,
                                           String jobTitle,
                                           String companyName,
                                           LocalDateTime scheduledAt,
                                           Integer durationMinutes,
                                           InterviewType interviewType,
                                           String location,
                                           String interviewRoomUrl,
                                           String note,
                                           boolean rescheduled) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(rescheduled
                    ? "CareerGraph | Lịch phỏng vấn đã được cập nhật"
                    : "CareerGraph | Bạn có lịch phỏng vấn mới");

            Context context = new Context();
            context.setVariable("candidateName", candidateName);
            context.setVariable("jobTitle", jobTitle);
            context.setVariable("companyName", companyName);
            context.setVariable("scheduleLabel",
                    scheduledAt != null ? scheduledAt.format(DATE_TIME_FORMATTER) : "Sẽ được cập nhật");
            context.setVariable("durationMinutes", durationMinutes);
            context.setVariable("interviewTypeLabel", interviewType == InterviewType.ONLINE ? "Phỏng vấn trực tuyến" : "Phỏng vấn trực tiếp");
            context.setVariable("location", location);
            context.setVariable("interviewRoomUrl", interviewRoomUrl);
            context.setVariable("hasInterviewRoomUrl", StringUtils.hasText(interviewRoomUrl));
            context.setVariable("note", note);
            context.setVariable("rescheduled", rescheduled);
            context.setVariable("date", LocalDate.now().format(DATE_FORMATTER));
            context.setVariable("year", Year.now().getValue());
            applySupportContext(context);

            String htmlContent = templateEngine.process("interview-status-update", context);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send interview schedule email", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendInterviewCancellationEmail(String toEmail,
                                               String candidateName,
                                               String jobTitle,
                                               String companyName,
                                               LocalDateTime scheduledAt,
                                               InterviewType interviewType,
                                               String cancellationReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("CareerGraph | Thông báo hủy lịch phỏng vấn");

            Context context = new Context();
            context.setVariable("candidateName", candidateName);
            context.setVariable("jobTitle", jobTitle);
            context.setVariable("companyName", companyName);
            context.setVariable("scheduleLabel",
                    scheduledAt != null ? scheduledAt.format(DATE_TIME_FORMATTER) : "Sẽ được cập nhật");
            context.setVariable("durationMinutes", null);
            context.setVariable("interviewTypeLabel", interviewType == InterviewType.ONLINE ? "Phỏng vấn trực tuyến" : "Phỏng vấn trực tiếp");
            context.setVariable("location", null);
            context.setVariable("interviewRoomUrl", null);
            context.setVariable("hasInterviewRoomUrl", false);
            context.setVariable("note", cancellationReason);
            context.setVariable("rescheduled", false);
            context.setVariable("cancelled", true);
            context.setVariable("date", LocalDate.now().format(DATE_FORMATTER));
            context.setVariable("year", Year.now().getValue());
            applySupportContext(context);

            String htmlContent = templateEngine.process("interview-status-update", context);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send interview cancellation email", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendHtml(String to, String subject, String html) {
        try {
            sendHtmlSync(to, subject, html);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendHtmlSync(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    private void applySupportContext(Context context) {
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("helpCenterUrl", helpCenterUrl);
        context.setVariable("hasHelpCenterUrl", StringUtils.hasText(helpCenterUrl));
    }
}
