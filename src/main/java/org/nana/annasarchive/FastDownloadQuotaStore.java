package org.nana.annasarchive;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.nana.shared.metrics.NanaMetrics;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class FastDownloadQuotaStore {

    private final FastDownloadQuotaRepository repository;
    private final NanaMetrics metrics;
    private final FastDownloadQuotaStore self;

    FastDownloadQuotaStore(FastDownloadQuotaRepository repository, NanaMetrics metrics, FastDownloadQuotaStore self) {
        this.repository = repository;
        this.metrics = metrics;
        this.self = self;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            VertxContextSupport.subscribeAndAwait(() -> self.current()
                    .invoke(quota -> quota.ifPresent(value -> metrics.updateQuota(value.remaining, value.total))));
        } catch (Throwable t) {
            Log.warnf(t, "Could not seed fast download quota gauges at startup");
        }
    }

    @WithTransaction
    public Uni<Void> save(int remaining, int total) {
        return repository.all().<Void>flatMap(list -> {
            if (list.isEmpty()) {
                FastDownloadQuota quota = new FastDownloadQuota();
                quota.remaining = remaining;
                quota.total = total;
                quota.updatedAt = Instant.now();
                return repository.persist(quota).replaceWithVoid();
            }
            FastDownloadQuota quota = list.get(0);
            quota.remaining = remaining;
            quota.total = total;
            quota.updatedAt = Instant.now();
            return Uni.createFrom().voidItem();
        }).invoke(() -> metrics.updateQuota(remaining, total));
    }

    @WithSession
    public Uni<Optional<FastDownloadQuota>> current() {
        return repository.all().map(list -> list.stream().findFirst());
    }
}
