package com.hcmute.careergraph.config.app;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.hcmute.careergraph.config.properties.BackblazeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final BackblazeProperties backblazeProperties;

    @Bean
    public AmazonS3 amazonS3() {

        // Config credentials
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                backblazeProperties.getAccessKey(),
                backblazeProperties.getSecretKey()
        );

        // Config client
        AwsClientBuilder.EndpointConfiguration client = new AmazonS3ClientBuilder.EndpointConfiguration(
                backblazeProperties.getEndpoint(),
                backblazeProperties.getRegion()
        );

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(client)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
