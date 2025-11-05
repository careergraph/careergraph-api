package com.hcmute.careergraph.config.app;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hcmute.careergraph.config.properties.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final CloudinaryProperties cloudinaryProperties;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> cloudinaryInstance = Map.of(
                "cloud_name", cloudinaryProperties.getCloudName(),
                "api_key", cloudinaryProperties.getApiKey(),
                "api_secret", cloudinaryProperties.getApiSecret()
        );
        return new Cloudinary(cloudinaryInstance);
    }
}
