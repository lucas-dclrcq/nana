package org.nana.annasarchive;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executor;
import org.nana.shared.config.NanaConfiguration;

/**
 * Fetches the mirror's search page directly with cached DDOS-Guard cookies. Uses the JDK HTTP
 * client rather than the Vert.x-based REST client because DDOS-Guard fingerprints and rejects
 * the latter's TLS handshake (403 even with valid cookies). Redirects are followed manually so
 * the Cookie header is guaranteed to be re-sent on the DDOS-Guard check hop
 * (/search?...&check=1).
 */
@ApplicationScoped
public class DirectSearchClient {

    private static final int MAX_REDIRECTS = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final NanaConfiguration config;

    public DirectSearchClient(NanaConfiguration config) {
        this.config = config;
    }

    public Uni<String> search(String url, String cookies) {
        // The JDK client completes on its own threads; hop back to the caller's Vert.x context so
        // downstream stages (Hibernate Reactive session, response emission) find it.
        Context context = Vertx.currentContext();
        Executor executor = context == null ? Runnable::run : command -> context.runOnContext(v -> command.run());
        return fetch(URI.create(url), cookies, 0).emitOn(executor);
    }

    private Uni<String> fetch(URI uri, String cookies, int redirects) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Cookie", cookies)
                .header("User-Agent", config.annasArchive().userAgent())
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return Uni.createFrom()
                .completionStage(() -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .flatMap(response -> {
                    int status = response.statusCode();
                    if (status >= 300 && status < 400) {
                        String location = response.headers().firstValue("location").orElse(null);
                        if (location == null || redirects >= MAX_REDIRECTS) {
                            return Uni.createFrom().failure(
                                    new AnnaArchiveException("Mirror redirect loop (status " + status + ")"));
                        }
                        return fetch(uri.resolve(location), cookies, redirects + 1);
                    }
                    if (status != 200) {
                        return Uni.createFrom().failure(
                                new AnnaArchiveException("Mirror rejected direct search (status " + status + ")"));
                    }
                    return Uni.createFrom().item(response.body());
                });
    }
}
