package com.hcmute.careergraph.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.es", ignoreUnknownFields = false)
public class ElasticsearchSyncProperties {

    private Target jobs = new Target();
    private Target candidates = new Target();
    private Cron cron = new Cron();

    @Getter
    @Setter
    public static class Target {
        private boolean syncEnabled = false;
        private boolean forceFullSync = false;
        private int maxEmbeddingsPerRun = 50;
    }

    @Getter
    @Setter
    public static class Cron {
        private boolean enabled = false;
        private Schedule jobs = new Schedule();
        private Schedule candidates = new Schedule();
    }

    @Getter
    @Setter
    public static class Schedule {
        private boolean enabled = true;
        private long fixedDelayMs = 120000;
        private long initialDelayMs = 120000;
        private int batchSize = 10;
    }
}
