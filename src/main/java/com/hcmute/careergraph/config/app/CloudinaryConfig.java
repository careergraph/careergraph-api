package com.hcmute.careergraph.config.app;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dyimxnbb8",
                "api_key", "828882965973278",
                "api_secret", "dsNG5xuNz08714z3N6Eew0vaK9s"
        ));
    }
}
