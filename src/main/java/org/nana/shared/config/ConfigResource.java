package org.nana.shared.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.nana.shared.ApiDtos.ConfigDto;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

    @Inject
    FormatPolicy formatPolicy;

    @GET
    @Operation(operationId = "getConfig", summary = "Expose server configuration to the UI")
    public ConfigDto get() {
        return new ConfigDto(formatPolicy.allowedFormats());
    }
}
