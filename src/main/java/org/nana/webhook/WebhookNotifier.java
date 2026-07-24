package org.nana.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.signals.Receives;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nana.download.DownloadFailed;
import org.nana.download.DownloadSucceeded;
import org.nana.shared.ApiDtos.DownloadDto;

import java.util.Optional;

@ApplicationScoped
public class WebhookNotifier {

    @ConfigProperty(name = "nana.webhook.enabled")
    boolean enabled;

    @ConfigProperty(name = "nana.webhook.url")
    Optional<String> url;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WebClient webClient;

    Uni<Void> onDownloadSucceeded(@Receives DownloadSucceeded event) {
        return send("download.succeeded", event.download());
    }

    Uni<Void> onDownloadFailed(@Receives DownloadFailed event) {
        return send("download.failed", event.download());
    }

    private Uni<Void> send(String event, DownloadDto download) {
        if (!enabled) {
            return Uni.createFrom().voidItem();
        }
        String target = url.filter(value -> !value.isBlank()).orElse(null);
        if (target == null) {
            Log.warnf("Webhook %s for download %d skipped: nana.webhook.url is not set", event, download.id());
            return Uni.createFrom().voidItem();
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(new WebhookPayload(event, toWebhookDownload(download)));
        } catch (JsonProcessingException e) {
            Log.warnf(e, "Webhook %s for download %d failed", event, download.id());
            return Uni.createFrom().voidItem();
        }
        return webClient.postAbs(target)
                .putHeader("Content-Type", "application/json")
                .timeout(10_000)
                .sendBuffer(Buffer.buffer(body))
                .invoke(response -> {
                    if (response.statusCode() / 100 == 2) {
                        Log.infof("Webhook %s sent for download %d", event, download.id());
                    } else {
                        Log.warnf("Webhook %s for download %d failed with HTTP %d",
                                event, download.id(), response.statusCode());
                    }
                })
                .replaceWithVoid()
                .onFailure().invoke(e -> Log.warnf(e, "Webhook %s for download %d failed", event, download.id()))
                .onFailure().recoverWithItem((Void) null);
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
