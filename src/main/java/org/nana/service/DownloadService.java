package org.nana.service;

import io.quarkus.logging.Log;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Locale;
import org.nana.api.ApiDtos.DownloadDto;
import org.nana.api.ApiDtos.DownloadPage;
import org.nana.api.ApiDtos.DownloadRequest;
import org.nana.api.ApiException;
import org.nana.domain.Download;
import org.nana.domain.DownloadRepository;

@ApplicationScoped
public class DownloadService {

    @Inject
    DownloadRepository repository;

    @Inject
    DownloadStateStore stateStore;

    @Inject
    DownloadJobRunner jobRunner;

    private static final String ACTIVE_MD5_INDEX = "download_active_md5_idx";

    public DownloadDto create(DownloadRequest request, String requestedBy) {
        String md5 = request.md5().toLowerCase(Locale.ROOT);
        if (repository.activeExists(md5)) {
            throw ApiException.conflict("A download for this book is already pending or running");
        }
        Download download;
        try {
            download = stateStore.createPending(
                    md5,
                    request.title().trim(),
                    blankToNull(request.author()),
                    blankToNull(request.extension()),
                    requestedBy);
        } catch (RuntimeException e) {
            // the check above races with concurrent requests; the partial unique index is the
            // authoritative guard
            if (isActiveDuplicate(e)) {
                throw ApiException.conflict("A download for this book is already pending or running");
            }
            throw e;
        }
        Log.infof("Download %d requested by %s (md5 %s, title \"%s\")",
                download.id, requestedBy, md5, download.title);
        jobRunner.start(download.id);
        return DownloadDto.of(download);
    }

    public DownloadPage history(int page, int size) {
        Page<Download> result = repository.history(PageRequest.ofPage(page + 1L, size, true));
        return new DownloadPage(
                result.content().stream().map(DownloadDto::of).toList(),
                result.totalElements(),
                result.totalPages(),
                page,
                size);
    }

    public DownloadDto get(long id) {
        Download download = repository.findById(id);
        if (download == null) {
            throw ApiException.notFound("Download " + id + " not found");
        }
        return DownloadDto.of(download);
    }

    private static boolean isActiveDuplicate(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && ACTIVE_MD5_INDEX.equalsIgnoreCase(String.valueOf(violation.getConstraintName()))) {
                return true;
            }
            if (cause instanceof java.sql.SQLException sql
                    && "23505".equals(sql.getSQLState())
                    && String.valueOf(sql.getMessage()).contains(ACTIVE_MD5_INDEX)) {
                return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
