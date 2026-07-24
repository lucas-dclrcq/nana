package org.nana.download;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.nana.shared.ApiDtos.DownloadDto;
import org.nana.shared.ApiDtos.DownloadPage;
import org.nana.shared.ApiException;

import java.time.Instant;
import java.util.Locale;

@ApplicationScoped
public class DownloadStateStore {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    private final DownloadRepository repository;
    
    DownloadStateStore(DownloadRepository repository) {
        this.repository = repository;
    }
    
    @WithTransaction
    public Uni<Download> createPending(String md5, String title, String author, String extension, String requestedBy) {
        Download download = new Download();
        download.md5 = md5.toLowerCase(Locale.ROOT);
        download.title = title;
        download.author = author;
        download.extension = extension;
        download.requestedBy = requestedBy;
        download.status = DownloadStatus.PENDING;
        download.requestedAt = Instant.now();
        return repository.persist(download).replaceWith(download);
    }

    @WithTransaction
    public Uni<DownloadDto> markDownloading(long id) {
        return require(id).map(download -> {
            download.status = DownloadStatus.DOWNLOADING;
            download.startedAt = Instant.now();
            return DownloadDto.of(download);
        });
    }

    @WithTransaction
    public Uni<DownloadDto> markSuccess(long id, String filePath, long sizeBytes, int domainIndexUsed) {
        return require(id).map(download -> {
            download.status = DownloadStatus.SUCCESS;
            download.finishedAt = Instant.now();
            download.filePath = filePath;
            download.sizeBytes = sizeBytes;
            download.domainIndexUsed = domainIndexUsed;
            download.errorMessage = null;
            return DownloadDto.of(download);
        });
    }

    @WithTransaction
    public Uni<DownloadDto> markFailed(long id, String errorMessage) {
        return require(id).map(download -> {
            download.status = DownloadStatus.FAILED;
            download.finishedAt = Instant.now();
            download.errorMessage = truncate(errorMessage);
            return DownloadDto.of(download);
        });
    }

    // Reads need an ambient reactive session; the @HQL/find methods do not open one themselves.
    @WithSession
    public Uni<Boolean> activeExists(String md5) {
        return repository.activeExists(md5);
    }

    // TODO : use token pagination to avoid fetching total
    // The reactive @HQL Page query does not populate the total (unlike the blocking one), so the
    // count is fetched explicitly within the same session.
    @WithSession
    public Uni<DownloadPage> history(int page, int size) {
        return repository.history(PageRequest.ofPage(page + 1L, size, false))
                .flatMap(result -> repository.count().map(total -> new DownloadPage(
                        result.content().stream().map(DownloadDto::of).toList(),
                        total,
                        total == 0 ? 0 : (total + size - 1) / size,
                        page,
                        size)));
    }

    @WithSession
    public Uni<DownloadDto> find(long id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(() -> ApiException.notFound("Download " + id + " not found"))
                .map(DownloadDto::of);
    }

    private Uni<Download> require(long id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(() -> new IllegalStateException("Download " + id + " not found"));
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= ERROR_MESSAGE_MAX_LENGTH ? value : value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
