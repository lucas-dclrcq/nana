package org.nana.annasarchive;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class FastDownloadQuotaStore {

    private final FastDownloadQuotaRepository repository;

    FastDownloadQuotaStore(FastDownloadQuotaRepository repository) {
        this.repository = repository;
    }

    @WithTransaction
    public Uni<Void> save(int remaining, int total) {
        return repository.all().flatMap(list -> {
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
        });
    }

    @WithSession
    public Uni<Optional<FastDownloadQuota>> current() {
        return repository.all().map(list -> list.stream().findFirst());
    }
}
