package org.nana.webhook;

import io.quarkus.rest.client.reactive.Url;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "webhook")
public interface WebhookClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Response> notify(@Url String url, WebhookPayload payload);
}
