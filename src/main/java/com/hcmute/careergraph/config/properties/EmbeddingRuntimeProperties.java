package com.hcmute.careergraph.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.embed", ignoreUnknownFields = false)
public class EmbeddingRuntimeProperties {

    /**
     * true: local FastAPI first, false: Gemini first.
     */
    private boolean useLocalFirst = true;

    /**
     * Allow fallback to Gemini when local embedding is unavailable.
     */
    private boolean allowGeminiFallback = false;
}
