package com.hcmute.careergraph.config.app;

import com.hcmute.careergraph.config.properties.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class EmailConfig {

    private final EmailProperties emailProperties;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailProperties.getHost());
        mailSender.setPort(emailProperties.getPort());
        mailSender.setUsername(emailProperties.getUsername());
        mailSender.setPassword(emailProperties.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(emailProperties.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(emailProperties.isStarttlsEnable()));
        props.put("mail.transport.protocol", emailProperties.getTransportProtocol());
        props.put("mail.debug", String.valueOf(emailProperties.isDebug()));

        return mailSender;
    }
}
