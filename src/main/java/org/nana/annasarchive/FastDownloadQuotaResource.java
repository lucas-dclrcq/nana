package org.nana.annasarchive;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.nana.shared.ApiDtos.FastDownloadQuotaDto;

@Path("/api/quota")
@Produces(MediaType.APPLICATION_JSON)
public class FastDownloadQuotaResource {

    @Inject
    FastDownloadQuotaStore store;

    @GET
    @Operation(operationId = "getFastDownloadQuota", summary = "Last known Anna's Archive fast download quota")
    public Uni<FastDownloadQuotaDto> get() {
        return store.current().map(opt -> opt
                .map(quota -> new FastDownloadQuotaDto(quota.remaining, quota.total, quota.updatedAt))
                .orElse(new FastDownloadQuotaDto(null, null, null)));
    }
}
