package org.nana.download;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.nana.api.ApiDtos.DownloadDto;
import org.nana.api.ApiDtos.DownloadPage;
import org.nana.api.ApiDtos.DownloadRequest;
import org.nana.api.security.CurrentUser;

@Path("/api/downloads")
@Produces(MediaType.APPLICATION_JSON)
public class DownloadResource {

    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    DownloadService downloadService;

    @Inject
    CurrentUser currentUser;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(202)
    @APIResponse(responseCode = "202", description = "Download accepted and queued",
            content = @Content(schema = @Schema(implementation = DownloadDto.class)))
    @Operation(operationId = "createDownload", summary = "Queue a server-side ebook download")
    public Uni<DownloadDto> create(@Valid @NotNull DownloadRequest request) {
        String requestedBy = currentUser.username();
        return downloadService.create(request, requestedBy);
    }

    @GET
    @Operation(operationId = "listDownloads", summary = "List downloads, most recent first")
    public Uni<DownloadPage> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return downloadService.history(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @GET
    @Path("/{id}")
    @Operation(operationId = "getDownload", summary = "Get one download")
    public Uni<DownloadDto> get(@PathParam("id") long id) {
        return downloadService.get(id);
    }
}
