package com.hcmute.careergraph.config.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${socket.http.pool.max-connections:50}")
    private int socketHttpPoolMaxConnections;

    @Value("${socket.http.pool.pending-acquire-timeout-ms:10000}")
    private long socketHttpPoolPendingAcquireTimeoutMs;

    @Value("${socket.http.pool.max-idle-time-ms:20000}")
    private long socketHttpPoolMaxIdleTimeMs;

    @Value("${socket.http.pool.max-life-time-ms:120000}")
    private long socketHttpPoolMaxLifeTimeMs;

    @Value("${socket.http.connect-timeout-ms:5000}")
    private int socketHttpConnectTimeoutMs;

    @Value("${socket.http.response-timeout-ms:10000}")
    private long socketHttpResponseTimeoutMs;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("careergraph-webclient")
                .maxConnections(socketHttpPoolMaxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(socketHttpPoolPendingAcquireTimeoutMs))
                .maxIdleTime(Duration.ofMillis(socketHttpPoolMaxIdleTimeMs))
                .maxLifeTime(Duration.ofMillis(socketHttpPoolMaxLifeTimeMs))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMillis(socketHttpResponseTimeoutMs))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, socketHttpConnectTimeoutMs);

        return builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
