package org.nana.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nana.api.ApiDtos.DownloadDto;
import org.nana.download.DownloadStatus;

@ApplicationScoped
public class WebhookNotifier {

    @ConfigProperty(name = "nana.webhook.enabled")
    boolean enabled;

    @ConfigProperty(name = "nana.webhook.url")
    Optional<String> url;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @RegisterForReflection
    public record WebhookPayload(String event, WebhookDownload download) {}

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
            Instant finishedAt) {}

    public void downloadSucceeded(DownloadDto download) {
        send("download.succeeded", download);
    }

    public void downloadFailed(DownloadDto download) {
        send("download.failed", download);
    }

    private void send(String event, DownloadDto download) {
        if (!enabled) {
            return;
        }
        String target = url.filter(value -> !value.isBlank()).orElse(null);
        if (target == null) {
            Log.warnf("Webhook %s for download %d skipped: nana.webhook.url is not set", event, download.id());
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(new WebhookPayload(event, toWebhookDownload(download)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 == 2) {
                Log.infof("Webhook %s sent for download %d", event, download.id());
            } else {
                Log.warnf("Webhook %s for download %d failed with HTTP %d", event, download.id(), response.statusCode());
            }
        } catch (IOException | RuntimeException e) {
            Log.warnf(e, "Webhook %s for download %d failed", event, download.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.warnf("Webhook %s for download %d interrupted", event, download.id());
        }
    }

    private static WebhookDownload toWebhookDownload(DownloadDto download) {
        return new WebhookDownload(
                download.id(),
                download.md5(),
                download.title(),
                download.extension(),
                download.requestedBy(),
                download.status(),
                download.filePath(),
                download.sizeBytes(),
                download.errorMessage(),
                download.requestedAt(),
                download.finishedAt());
    }
}
