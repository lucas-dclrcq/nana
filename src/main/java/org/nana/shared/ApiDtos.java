package org.nana.shared;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import org.nana.download.Download;
import org.nana.download.DownloadStatus;

public final class ApiDtos {

    private ApiDtos() {}

    public record SearchResult(
            String md5,
            String title,
            String author,
            String extension,
            Long sizeBytes,
            String language,
            Integer year,
            String coverUrl) {}

    public record DownloadRequest(
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{32}") String md5,
            @NotBlank String title,
            String author,
            String extension) {}

    public record DownloadDto(
            Long id,
            String md5,
            String title,
            String author,
            String extension,
            String requestedBy,
            DownloadStatus status,
            Instant requestedAt,
            Instant startedAt,
            Instant finishedAt,
            String filePath,
            Long sizeBytes,
            String errorMessage,
            Integer domainIndexUsed) {

        public static DownloadDto of(Download download) {
            return new DownloadDto(
                    download.id,
                    download.md5,
                    download.title,
                    download.author,
                    download.extension,
                    download.requestedBy,
                    download.status,
                    download.requestedAt,
                    download.startedAt,
                    download.finishedAt,
                    download.filePath,
                    download.sizeBytes,
                    download.errorMessage,
                    download.domainIndexUsed);
        }
    }

    public record DownloadPage(
            List<DownloadDto> content,
            long totalElements,
            long totalPages,
            int page,
            int size) {}

    // Serialized only through Response entities (auth filter, exception mapper), which
    // build-time analysis cannot see; without this the native image 500s instead of 401/404.
    @RegisterForReflection
    public record ErrorResponse(String message) {}
}
