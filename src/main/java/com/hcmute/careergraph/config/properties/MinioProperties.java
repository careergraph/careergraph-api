package com.hcmute.careergraph.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(
        prefix = "integration.minio",
        ignoreUnknownFields = false
)
public class MinioProperties {

    private String accessKey;
    private String secretKey;
    private String url;
}
