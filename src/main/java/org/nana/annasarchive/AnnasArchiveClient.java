package org.nana.annasarchive;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.rest.client.reactive.Url;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "annasarchive")
@RegisterProvider(TextJsonContentTypeConverter.class)
public interface AnnasArchiveClient {

    @GET
    @Path("/dyn/api/fast_download.json")
    @Produces(MediaType.APPLICATION_JSON)
    @ClientQueryParam(name = "key", value = "${nana.annas-archive.secret-key}")
    Uni<FastDownloadResponse> fastDownload(
            @QueryParam("md5") String md5,
            @QueryParam("path_index") int pathIndex,
            @QueryParam("domain_index") int domainIndex);

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_HTML)
    Uni<String> search(
            @QueryParam("q") String query,
            @QueryParam("lang") String language,
            @QueryParam("ext") String extension,
            @QueryParam("content") String content);

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Multi<byte[]> download(@Url String url);
}
