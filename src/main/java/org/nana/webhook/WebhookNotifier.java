package org.nana.webhook;

import io.quarkus.logging.Log;
import io.quarkus.signals.Receives;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.nana.download.DownloadFailed;
import org.nana.download.DownloadSucceeded;
import org.nana.shared.ApiDtos.DownloadDto;

import java.util.Optional;

@ApplicationScoped
public class WebhookNotifier {
    // TODO: une seule classe de config pour toutes les properties
    private final boolean enabled;
    // TODO: configuration validation : url doit être présente si enabled
    private final Optional<String> url;
    private final WebhookClient webhookClient;

    public WebhookNotifier(@ConfigProperty(name = "nana.webhook.enabled") boolean enabled, 
                           @ConfigProperty(name = "nana.webhook.url") Optional<String> url, 
                           @RestClient WebhookClient webhookClient) {
        this.enabled = enabled;
        this.url = url;
        this.webhookClient = webhookClient;
    }

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
        
        WebhookPayload payload = new WebhookPayload(event, toWebhookDownload(download));

        return webhookClient.notify(target, payload)
                .invoke(() -> Log.infof("Webhook %s sent for download %d", event, download.id()))
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
