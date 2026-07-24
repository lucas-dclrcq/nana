package org.nana.annasarchive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastDownloadResponse(
        @JsonProperty("download_url") String downloadUrl,
        @JsonProperty("error") String error) {}
