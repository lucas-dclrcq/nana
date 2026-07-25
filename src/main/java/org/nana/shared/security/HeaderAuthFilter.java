package org.nana.shared.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.nana.shared.config.NanaConfiguration;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class HeaderAuthFilter implements ContainerRequestFilter {

    private final CurrentUser currentUser;
    private final NanaConfiguration config;

    public HeaderAuthFilter(CurrentUser currentUser, NanaConfiguration config) {
        this.currentUser = currentUser;
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = "/" + stripLeadingSlashes(requestContext.getUriInfo().getPath());

        if (!path.equals("/api") && !path.startsWith("/api/")) {
            return;
        }

        String username = blankToNull(requestContext.getHeaderString(config.auth().headerName()));
        if (username == null) {
            username = config.auth().fallbackUsername().map(HeaderAuthFilter::blankToNull).orElse(null);
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
