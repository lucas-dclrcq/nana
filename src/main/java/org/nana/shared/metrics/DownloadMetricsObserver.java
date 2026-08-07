package org.nana.shared.metrics;

import io.quarkus.signals.Receives;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.nana.download.DownloadFailed;
import org.nana.download.DownloadPending;
import org.nana.download.DownloadSucceeded;
import org.nana.shared.ApiDtos.DownloadDto;

import java.time.Duration;
import java.time.Instant;

@ApplicationScoped
public class DownloadMetricsObserver {

    private final NanaMetrics metrics;

    public DownloadMetricsObserver(NanaMetrics metrics) {
        this.metrics = metrics;
    }

    Uni<Void> onDownloadPending(@Receives DownloadPending event) {
        metrics.recordDownloadRequested();
        return Uni.createFrom().voidItem();
    }

    Uni<Void> onDownloadSucceeded(@Receives DownloadSucceeded event) {
        metrics.recordDownloadSucceeded(duration(event.download()));
        return Uni.createFrom().voidItem();
    }

    Uni<Void> onDownloadFailed(@Receives DownloadFailed event) {
        metrics.recordDownloadFailed(duration(event.download()));
        return Uni.createFrom().voidItem();
    }

    // startedAt is null when a download fails before markDownloading (startup crash path);
    // fall back to requestedAt so the failure still gets a duration.
    private static Duration duration(DownloadDto download) {
        Instant start = download.startedAt() != null ? download.startedAt() : download.requestedAt();
        if (start == null || download.finishedAt() == null) {
            return null;
        }
        return Duration.between(start, download.finishedAt());
    }
}
