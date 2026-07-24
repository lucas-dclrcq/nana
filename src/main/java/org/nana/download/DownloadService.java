package org.nana.download;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Locale;
import org.nana.api.ApiDtos.DownloadDto;
import org.nana.api.ApiDtos.DownloadPage;
import org.nana.api.ApiDtos.DownloadRequest;
import org.nana.api.ApiException;

@ApplicationScoped
public class DownloadService {

    @Inject
    DownloadStateStore stateStore;

    @Inject
    DownloadJobRunner jobRunner;

    private static final String ACTIVE_MD5_INDEX = "download_active_md5_idx";

    public Uni<DownloadDto> create(DownloadRequest request, String requestedBy) {
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
