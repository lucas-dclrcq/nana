package org.nana.search;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.nana.annasarchive.AnnaArchiveHtmlParser;
import org.nana.annasarchive.AnnasArchiveClient;
import org.nana.annasarchive.DdosGuardCookieStore;
import org.nana.annasarchive.DirectSearchClient;
import org.nana.annasarchive.FlareSolverrClient;
import org.nana.annasarchive.FlareSolverrRequest;
import org.nana.annasarchive.SearchHit;
import org.nana.shared.ApiDtos.SearchResult;
import org.nana.shared.ApiException;
import org.nana.shared.config.FormatPolicy;
import org.nana.shared.config.NanaConfiguration;
import org.nana.shared.metrics.NanaMetrics;

import java.util.List;

@ApplicationScoped
public class SearchService {

    private final AnnasArchiveClient searchClient;
    private final FlareSolverrClient flareSolverr;
    private final DirectSearchClient directClient;
    private final DdosGuardCookieStore cookieStore;
    private final FormatPolicy formatPolicy;
    private final NanaMetrics metrics;
    private final NanaConfiguration config;

    public SearchService(@RestClient AnnasArchiveClient searchClient, @RestClient FlareSolverrClient flareSolverr,
            DirectSearchClient directClient, DdosGuardCookieStore cookieStore, FormatPolicy formatPolicy,
            NanaMetrics metrics, NanaConfiguration config) {
        this.searchClient = searchClient;
        this.flareSolverr = flareSolverr;
        this.directClient = directClient;
        this.cookieStore = cookieStore;
        this.formatPolicy = formatPolicy;
        this.metrics = metrics;
        this.config = config;
    }

    public Uni<List<SearchResult>> search(String query, String language, String extension, String content) {
        String q = query.trim();
        String lang = blankToNull(language);
        String ext = blankToNull(extension);
        String cont = blankToNull(content);
        return metrics.timeSearch(() -> fetchHtml(q, lang, ext, cont)
                .onFailure().invoke(e -> Log.errorf(e, "Anna's Archive search failed (query '%s')", query))
                .onFailure().transform(e -> ApiException.badGateway("Anna's Archive search failed"))
                .map(html -> AnnaArchiveHtmlParser.parse(html).stream()
                        .filter(hit -> formatPolicy.isAllowed(hit.extension()))
                        .map(SearchService::toDto)
                        .toList()));
    }

    private Uni<String> fetchHtml(String query, String language, String extension, String content) {
        if (!config.flaresolverr().enabled()) {
            return searchClient.search(query, language, extension, content);
        }
        return cookieStore.get().flatMap(cookies -> {
            if (cookies == null) {
                return solveAndStoreCookies(query, language, extension, content);
            }
            return directClient.search(searchUrl(query, language, extension, content), cookies)
                    .onFailure().recoverWithUni(failure -> {
                        // DDOS-Guard rejected the cached cookies (expired or IP change): solve again.
                        Log.infof("Direct search with cached DDOS-Guard cookies failed (%s); re-solving the challenge",
                                failure.getMessage());
                        return cookieStore.invalidate()
                                .chain(() -> solveAndStoreCookies(query, language, extension, content));
                    });
        });
    }

    private Uni<String> solveAndStoreCookies(String query, String language, String extension, String content) {
        String url = searchUrl(query, language, extension, content);
        return flareSolverr.command(FlareSolverrRequest.get(url, config.flaresolverr().maxTimeout()))
                .flatMap(response -> {
                    String html = response.html();
                    return cookieStore.store(response.cookieHeader()).replaceWith(html);
                });
    }

    private String searchUrl(String query, String language, String extension, String content) {
        UriBuilder builder = UriBuilder.fromUri(config.annasArchive().mirrorUrl())
                .path("/search")
                .queryParam("q", query);
        if (language != null) {
            builder.queryParam("lang", language);
        }
        if (extension != null) {
            builder.queryParam("ext", extension);
        }
        if (content != null) {
            builder.queryParam("content", content);
        }
        return builder.build().toString();
    }

    private static SearchResult toDto(SearchHit hit) {
        return new SearchResult(
                hit.md5(),
                hit.title(),
                hit.author(),
                hit.extension(),
                hit.sizeBytes(),
                hit.language(),
                hit.year(),
                hit.coverUrl());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
