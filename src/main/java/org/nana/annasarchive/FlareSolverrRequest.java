package org.nana.annasarchive;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FlareSolverrRequest(
        String cmd,
        String url,
        long maxTimeout) {

    public static FlareSolverrRequest get(String url, long maxTimeout) {
        return new FlareSolverrRequest("request.get", url, maxTimeout);
    }
}
