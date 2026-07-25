package org.nana.download;

import io.quarkus.logging.Log;
import io.quarkus.signals.Signal;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.nana.shared.ApiDtos.DownloadDto;
import org.nana.shared.ApiDtos.DownloadPage;
import org.nana.shared.ApiDtos.DownloadRequest;
import org.nana.shared.ApiException;
import org.nana.shared.config.FormatPolicy;

import java.util.Locale;

@ApplicationScoped
public class DownloadService {
    private static final String ACTIVE_MD5_INDEX = "download_active_md5_idx";

    private final DownloadStateStore stateStore;
    private final DownloadJobRunner jobRunner;
    private final Signal<DownloadPending> downloadPending;
    private final FormatPolicy formatPolicy;

    public DownloadService(DownloadStateStore stateStore, DownloadJobRunner jobRunner,
                           Signal<DownloadPending> downloadPending, FormatPolicy formatPolicy) {
        this.stateStore = stateStore;
        this.jobRunner = jobRunner;
        this.downloadPending = downloadPending;
        this.formatPolicy = formatPolicy;
    }

    public Uni<DownloadDto> create(DownloadRequest request, String requestedBy) {
        if (!formatPolicy.isAllowed(request.extension())) {
            return Uni.createFrom().failure(ApiException.badRequest(
                    "Format not allowed. Allowed formats: " + String.join(", ", formatPolicy.allowedFormats())));
        }
        String md5 = request.md5().toLowerCase(Locale.ROOT);
        return stateStore.activeExists(md5).flatMap(exists -> {
            if (exists) {
                return Uni.createFrom().failure(
                        ApiException.conflict("A download for this book is already pending or running"));
            }
            return stateStore.createPending(
                            md5,
                            request.title().trim(),
                            blankToNull(request.author()),
                            blankToNull(request.extension()),
                            requestedBy)
                    // the check above races with concurrent requests; the partial unique index is
                    // the authoritative guard
                    .onFailure().transform(e -> isActiveDuplicate(e)
                            ? ApiException.conflict("A download for this book is already pending or running")
                            : e)
                    .invoke(download -> {
                        Log.infof("Download %d requested by %s (md5 %s, title \"%s\")",
                                download.id, requestedBy, md5, download.title);
                        downloadPending.publish(new DownloadPending(DownloadDto.of(download)));
                        jobRunner.start(download.id);
                    })
                    .map(DownloadDto::of);
        });
    }

    public Uni<DownloadPage> history(int page, int size) {
        return stateStore.history(page, size);
    }

    public Uni<DownloadDto> get(long id) {
        return stateStore.find(id);
    }

    private static boolean isActiveDuplicate(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && ACTIVE_MD5_INDEX.equalsIgnoreCase(String.valueOf(violation.getConstraintName()))) {
                return true;
            }
            if (cause instanceof io.vertx.pgclient.PgException pg
                    && "23505".equals(pg.getSqlState())
                    && String.valueOf(pg.getMessage()).contains(ACTIVE_MD5_INDEX)) {
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
