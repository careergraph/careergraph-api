package com.hcmute.careergraph.config.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableAsync
public class AppConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(30000)) // Timeout for reading
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000); // Timeout for connection

        return builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
