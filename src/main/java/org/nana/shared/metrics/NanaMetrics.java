package org.nana.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@ApplicationScoped
public class NanaMetrics {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_REQUESTED = "requested";

    // Unknown until the first quota refresh; dashboards filter with `>= 0`.
    private static final long QUOTA_UNKNOWN = -1;

    private final MeterRegistry registry;
    private final Timer searchSuccess;
    private final Timer searchError;
    private final Timer downloadSuccess;
    private final Timer downloadFailed;
    private final Counter downloadsRequested;
    private final Counter downloadsSucceeded;
    private final Counter downloadsFailed;
    private final AtomicLong quotaRemaining = new AtomicLong(QUOTA_UNKNOWN);
    private final AtomicLong quotaLimit = new AtomicLong(QUOTA_UNKNOWN);

    public NanaMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.searchSuccess = durationTimer("nana.search.duration",
                "Duration of Anna's Archive searches", STATUS_SUCCESS);
        this.searchError = durationTimer("nana.search.duration",
                "Duration of Anna's Archive searches", STATUS_ERROR);
        this.downloadSuccess = durationTimer("nana.download.duration",
                "Duration of downloads from start to terminal state", STATUS_SUCCESS);
        this.downloadFailed = durationTimer("nana.download.duration",
                "Duration of downloads from start to terminal state", STATUS_FAILED);
        this.downloadsRequested = downloadsCounter(STATUS_REQUESTED);
        this.downloadsSucceeded = downloadsCounter(STATUS_SUCCESS);
        this.downloadsFailed = downloadsCounter(STATUS_FAILED);
        Gauge.builder("nana.annas.quota.remaining", quotaRemaining, AtomicLong::get)
                .description("Anna's Archive fast downloads remaining today (-1 until known)")
                .register(registry);
        Gauge.builder("nana.annas.quota.limit", quotaLimit, AtomicLong::get)
                .description("Anna's Archive fast downloads allowed per day (-1 until known)")
                .register(registry);
    }

    public <T> Uni<T> timeSearch(Supplier<Uni<T>> pipeline) {
        // Deferred so the clock starts at subscription, not at pipeline assembly.
        return Uni.createFrom().deferred(() -> {
            Timer.Sample sample = Timer.start(registry);
            return pipeline.get()
                    .onItemOrFailure().invoke((item, failure) ->
                            sample.stop(failure == null ? searchSuccess : searchError));
        });
    }

    public void recordDownloadRequested() {
        downloadsRequested.increment();
    }

    public void recordDownloadSucceeded(Duration duration) {
        downloadsSucceeded.increment();
        if (duration != null) {
            downloadSuccess.record(duration);
        }
    }

    public void recordDownloadFailed(Duration duration) {
        downloadsFailed.increment();
        if (duration != null) {
            downloadFailed.record(duration);
        }
    }

    public void updateQuota(long remaining, long limit) {
        quotaRemaining.set(remaining);
        quotaLimit.set(limit);
    }

    private Timer durationTimer(String name, String description, String status) {
        return Timer.builder(name)
                .description(description)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(registry);
    }

    private Counter downloadsCounter(String status) {
        return Counter.builder("nana.downloads")
                .description("Downloads by lifecycle status")
                .tag("status", status)
                .register(registry);
    }
}
