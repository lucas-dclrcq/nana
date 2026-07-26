package org.nana.testsupport;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.function.Supplier;
import org.nana.annasarchive.FastDownloadQuotaRepository;
import org.nana.download.DownloadRepository;
import org.nana.download.DownloadStateStore;

@ApplicationScoped
public class TestDataSupport {

    @Inject
    DownloadRepository repository;

    @Inject
    FastDownloadQuotaRepository quotaRepository;

    @Inject
    DownloadStateStore stateStore;

    @Inject
    TestDataSupport self;

    public void deleteAll() {
        await(() -> self.deleteAllReactive());
    }

    public long quotaCount() {
        return await(() -> self.quotaCountReactive());
    }

    public long createPending(String md5, String title, String author, String extension, String requestedBy) {
        return await(() -> stateStore.createPending(md5, title, author, extension, requestedBy)).id;
    }

    public void setRequestedAt(long id, Instant requestedAt) {
        await(() -> self.setRequestedAtReactive(id, requestedAt));
    }

    @WithTransaction
    public Uni<Void> deleteAllReactive() {
        return repository.deleteAll().flatMap(ignored -> quotaRepository.deleteAll()).replaceWithVoid();
    }

    @WithSession
    public Uni<Long> quotaCountReactive() {
        return quotaRepository.count();
    }

    @WithTransaction
    public Uni<Void> setRequestedAtReactive(long id, Instant requestedAt) {
        return repository.findById(id).invoke(download -> download.requestedAt = requestedAt).replaceWithVoid();
    }

    private static <T> T await(Supplier<Uni<T>> uni) {
        try {
            return VertxContextSupport.subscribeAndAwait(uni);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
