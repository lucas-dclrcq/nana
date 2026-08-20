package org.nana.annasarchive;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

/**
 * Holds the DDOS-Guard cookies captured from the last successful FlareSolverr solve so
 * subsequent searches can hit the mirror directly (fast) instead of re-solving the
 * challenge in a browser on every request. Cookies are persisted in the database so a
 * restart of the application does not pay the challenge again, and cached with Quarkus
 * Cache so only the first search after startup touches the database. Invalidated when
 * the mirror rejects them, which triggers a fresh solve.
 */
@ApplicationScoped
public class DdosGuardCookieStore {

    public static final String CACHE_NAME = "ddos-guard-cookies";

    private final DdosGuardCookiesRepository repository;
    private final DdosGuardCookieStore self;

    DdosGuardCookieStore(DdosGuardCookiesRepository repository, DdosGuardCookieStore self) {
        this.repository = repository;
        this.self = self;
    }

    /** Resolves to the cookie header to send to the mirror, or null when a solve is needed. */
    @CacheResult(cacheName = CACHE_NAME)
    public Uni<String> get() {
        return self.load();
    }

    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public Uni<Void> store(String cookieHeader) {
        return cookieHeader == null ? self.clear() : self.save(cookieHeader);
    }

    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public Uni<Void> invalidate() {
        return self.clear();
    }

    @WithSession
    public Uni<String> load() {
        return repository.all().map(list -> list.isEmpty() ? null : list.get(0).cookieHeader);
    }

    @WithTransaction
    public Uni<Void> save(String cookieHeader) {
        return repository.all().flatMap(list -> {
            if (list.isEmpty()) {
                DdosGuardCookies cookies = new DdosGuardCookies();
                cookies.cookieHeader = cookieHeader;
                cookies.updatedAt = Instant.now();
                return repository.persist(cookies).replaceWithVoid();
            }
            DdosGuardCookies cookies = list.get(0);
            cookies.cookieHeader = cookieHeader;
            cookies.updatedAt = Instant.now();
            return Uni.createFrom().voidItem();
        });
    }

    @WithTransaction
    public Uni<Void> clear() {
        return repository.deleteAll().replaceWithVoid();
    }
}
