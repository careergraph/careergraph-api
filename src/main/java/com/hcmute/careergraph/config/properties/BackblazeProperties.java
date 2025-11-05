package com.hcmute.careergraph.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "integration.s3", ignoreUnknownFields = false)
public class BackblazeProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;
}
