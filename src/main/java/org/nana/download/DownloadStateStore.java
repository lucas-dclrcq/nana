package org.nana.download;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Locale;
import org.nana.api.ApiDtos.DownloadDto;

@ApplicationScoped
public class DownloadStateStore {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2000;

    @Inject
    DownloadRepository repository;

    public record DownloadJob(long id, String md5, String title, String extension) {}

    @Transactional
    public Download createPending(String md5, String title, String author, String extension, String requestedBy) {
        Download download = new Download();
        download.md5 = md5.toLowerCase(Locale.ROOT);
        download.title = title;
        download.author = author;
        download.extension = extension;
        download.requestedBy = requestedBy;
        download.status = DownloadStatus.PENDING;
        download.requestedAt = Instant.now();
        repository.persist(download);
        return download;
    }

    @Transactional
    public DownloadJob markDownloading(long id) {
        Download download = require(id);
        download.status = DownloadStatus.DOWNLOADING;
        download.startedAt = Instant.now();
        return new DownloadJob(download.id, download.md5, download.title, download.extension);
    }

    @Transactional
    public DownloadDto markSuccess(long id, String filePath, long sizeBytes, int domainIndexUsed) {
        Download download = require(id);
        download.status = DownloadStatus.SUCCESS;
        download.finishedAt = Instant.now();
        download.filePath = filePath;
        download.sizeBytes = sizeBytes;
        download.domainIndexUsed = domainIndexUsed;
        download.errorMessage = null;
        return DownloadDto.of(download);
    }

    @Transactional
    public DownloadDto markFailed(long id, String errorMessage) {
        Download download = require(id);
        download.status = DownloadStatus.FAILED;
        download.finishedAt = Instant.now();
        download.errorMessage = truncate(errorMessage);
        return DownloadDto.of(download);
    }

    private Download require(long id) {
        Download download = repository.findById(id);
        if (download == null) {
            throw new IllegalStateException("Download " + id + " not found");
        }
        return download;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= ERROR_MESSAGE_MAX_LENGTH ? value : value.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
