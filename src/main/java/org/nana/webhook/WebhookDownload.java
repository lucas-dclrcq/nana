package org.nana.webhook;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.nana.download.DownloadStatus;

import java.time.Instant;

@RegisterForReflection
public record WebhookDownload(
        Long id,
        String md5,
        String title,
        String extension,
        String requestedBy,
        DownloadStatus status,
        String filePath,
        Long sizeBytes,
        String errorMessage,
        Instant requestedAt,
        Instant finishedAt) {
}