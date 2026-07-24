package org.nana.api.security;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nana.api.ApiDtos.ErrorResponse;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class HeaderAuthFilter implements ContainerRequestFilter {

    @Inject
    CurrentUser currentUser;

    @ConfigProperty(name = "nana.auth.header-name")
    String headerName;

    @ConfigProperty(name = "nana.auth.fallback-username")
    Optional<String> fallbackUsername;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = "/" + stripLeadingSlashes(requestContext.getUriInfo().getPath());
        if (!path.equals("/api") && !path.startsWith("/api/")) {
            return;
        }
        String username = blankToNull(requestContext.getHeaderString(headerName));
        if (username == null) {
            username = fallbackUsername.map(HeaderAuthFilter::blankToNull).orElse(null);
        }
        if (username == null) {
            Log.warnf("Rejected unauthenticated request to %s: missing %s header", path, headerName);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse("Missing authentication header " + headerName))
                    .build());
            return;
        }
        currentUser.set(username);
    }

    private static String stripLeadingSlashes(String path) {
        int i = 0;
        while (i < path.length() && path.charAt(i) == '/') {
            i++;
        }
        return path.substring(i);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
