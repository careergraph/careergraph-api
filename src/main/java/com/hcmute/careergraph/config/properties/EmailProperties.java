package com.hcmute.careergraph.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mail", ignoreUnknownFields = false)
public class EmailProperties {

    private String host = "smtp.gmail.com";
    private int port = 587;
    private String username;
    private String password;
    private String transportProtocol = "smtp";
    private boolean smtpAuth = true;
    private boolean starttlsEnable = true;
    private boolean debug = false;
}
