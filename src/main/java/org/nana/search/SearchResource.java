package org.nana.search;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.nana.api.ApiDtos.SearchResult;
import org.nana.api.ApiException;

@Path("/api/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    SearchService searchService;

    @GET
    @Operation(operationId = "searchBooks", summary = "Search ebooks on Anna's Archive")
    public Uni<List<SearchResult>> search(
            @QueryParam("q") String query,
            @QueryParam("lang") String language,
            @QueryParam("ext") String extension,
            @QueryParam("content") String content) {
        if (query == null || query.isBlank()) {
            return Uni.createFrom().failure(ApiException.badRequest("q is required"));
        }
        return searchService.search(query, language, extension, content);
    }
}
