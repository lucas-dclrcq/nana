package org.nana.annasarchive;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.util.List;

@RegisterRestClient(configKey = "annasarchive")
@RegisterProvider(TextJsonContentTypeConverter.class)
public interface AnnasArchiveClient {

    @GET
    @Path("/dyn/api/fast_download.json")
    @Produces(MediaType.APPLICATION_JSON)
    @ClientQueryParam(name = "key", value = "${nana.annas-archive.secret-key}")
    FastDownloadResponse fastDownload(
            @QueryParam("md5") String md5,
            @QueryParam("path_index") int pathIndex,
            @QueryParam("domain_index") int domainIndex);

    @GET
    @Path("/search")
    @Produces(MediaType.TEXT_HTML)
    String search(
            @QueryParam("q") String query,
            @QueryParam("lang") String language,
            @QueryParam("ext") String extension,
            @QueryParam("content") String content);
}
