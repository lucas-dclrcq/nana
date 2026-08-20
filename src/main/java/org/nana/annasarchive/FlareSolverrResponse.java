package org.nana.annasarchive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlareSolverrResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("solution") Solution solution) {

    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }

    public String html() {
        if (!isOk() || solution == null || solution.response() == null) {
            throw new AnnaArchiveException(message != null && !message.isBlank()
                    ? message
                    : "FlareSolverr returned no page content");
        }
        return solution.response();
    }

    /** Cookie header value from the solved challenge, or null when the solution carries no cookies. */
    public String cookieHeader() {
        if (solution == null || solution.cookies() == null || solution.cookies().isEmpty()) {
            return null;
        }
        return solution.cookies().stream()
                .map(cookie -> cookie.name() + "=" + cookie.value())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Solution(
            @JsonProperty("url") String url,
            @JsonProperty("status") int status,
            @JsonProperty("response") String response,
            @JsonProperty("userAgent") String userAgent,
            @JsonProperty("cookies") java.util.List<Cookie> cookies) {}

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cookie(
            @JsonProperty("name") String name,
            @JsonProperty("value") String value) {}
}
