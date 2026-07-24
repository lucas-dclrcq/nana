package org.nana.annasarchive;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class TextJsonContentTypeConverter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        responseContext.getHeaders().replaceAll((name, values) -> {
            if (name.equals("Content-Type") && values.contains("text/json; charset=utf-8")) {
                return List.of("application/json");
            }
            return values;
        });
    }
}