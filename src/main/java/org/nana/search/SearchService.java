package org.nana.search;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.nana.annasarchive.AnnaArchiveHtmlParser;
import org.nana.annasarchive.AnnasArchiveClient;
import org.nana.annasarchive.SearchHit;
import org.nana.api.ApiDtos.SearchResult;
import org.nana.api.ApiException;

@ApplicationScoped
public class SearchService {

    @Inject
    @RestClient
    AnnasArchiveClient searchClient;

    public Uni<List<SearchResult>> search(String query, String language, String extension, String content) {
        return searchClient.search(
                        query.trim(),
                        blankToNull(language),
                        blankToNull(extension),
                        blankToNull(content))
                .onFailure().transform(e -> ApiException.badGateway("Anna's Archive search failed"))
                .map(html -> AnnaArchiveHtmlParser.parse(html).stream().map(SearchService::toDto).toList());
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
