package com.hcmute.careergraph.config.app;

public record ElasticsearchSyncResult(
    String target,
    boolean skipped,
    boolean force,
    int batchSize,
    int indexed,
    int unchanged,
    int pending,
    String message
) {
}
