package org.nana.download;

import io.quarkus.logging.Log;
import io.quarkus.signals.Signal;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.nana.annasarchive.AnnaArchiveException;
import org.nana.annasarchive.AnnaArchiveGateway;
import org.nana.shared.config.NanaConfiguration;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

@ApplicationScoped
public class DownloadJobRunner {

    private static final int SLUG_MAX_LENGTH = 80;

    private final DownloadStateStore stateStore;
    private final AnnaArchiveGateway gateway;
    private final Signal<DownloadSucceeded> downloadSucceeded;
    private final Signal<DownloadFailed> downloadFailed;
    private final Signal<DownloadDownloading> downloadDownloading;
    private final NanaConfiguration config;

    public DownloadJobRunner(DownloadStateStore stateStore, AnnaArchiveGateway gateway, Signal<DownloadSucceeded> downloadSucceeded, Signal<DownloadFailed> downloadFailed, Signal<DownloadDownloading> downloadDownloading, NanaConfiguration config) {
        this.stateStore = stateStore;
        this.gateway = gateway;
        this.downloadSucceeded = downloadSucceeded;
        this.downloadFailed = downloadFailed;
        this.downloadDownloading = downloadDownloading;
        this.config = config;
    }

    public void start(long downloadId) {
        run(downloadId).subscribe().with(
                ignored -> {},
                error -> {
                    Log.errorf(error, "Download %d crashed", downloadId);
                    stateStore.markFailed(downloadId, "internal error: " + error.getMessage())
                            .invoke(result -> downloadFailed.publish(new DownloadFailed(result)))
                            .subscribe().with(ignored -> {}, ignored -> {});
                });
    }

    private Uni<Void> run(long downloadId) {
        return stateStore.markDownloading(downloadId)
                .invoke(dto -> downloadDownloading.publish(new DownloadDownloading(dto)))
                .invoke(dto -> Log.infof("Download %d started (md5 %s)", downloadId, dto.md5()))
                .map(dto -> new DownloadJob(dto.id(), dto.md5(), dto.title(), dto.extension()))
                .flatMap(job -> attempt(downloadId, job, 0, "no download attempt was made"));
    }

    private Uni<Void> attempt(long downloadId, DownloadJob job, int domainIndex, String lastError) {
        if (domainIndex > config.annasArchive().maxDomainIndex()) {
            return stateStore.markFailed(downloadId, lastError)
                    .invoke(result -> Log.errorf("Download %d failed: %s", downloadId, lastError))
                    .invoke(result -> downloadFailed.publish(new DownloadFailed(result)))
                    .replaceWithVoid();
        }
        Path target = config.download().directory().resolve(fileName(job));
        Path partFile = target.resolveSibling(target.getFileName() + ".part");
        return fetch(job.md5(), domainIndex, partFile, target)
                .onItemOrFailure().transformToUni((sizeBytes, failure) -> {
                    if (failure == null) {
                        return stateStore.markSuccess(downloadId, target.toString(), sizeBytes, domainIndex)
                                .invoke(result -> Log.infof("Download %d succeeded: %s (%d bytes, domain index %d)",
                                        downloadId, target, sizeBytes, domainIndex))
                                .invoke(result -> downloadSucceeded.publish(new DownloadSucceeded(result)))
                                .replaceWithVoid();
                    }
      
                    if (!(failure instanceof AnnaArchiveException)) {
                        return Uni.createFrom().failure(failure);
                    }
                    
                    String error = failure.getMessage();
                    Log.warnf(failure, "Download %d attempt with domain index %d failed: %s", downloadId, domainIndex, error);
                    return gateway.deleteQuietly(partFile)
                            .flatMap(ignored -> attempt(downloadId, job, domainIndex + 1, error));
                });
    }

    private Uni<Long> fetch(String md5, int domainIndex, Path partFile, Path target) {
        return gateway.resolveDownloadUrl(md5, domainIndex)
                .flatMap(url -> gateway.streamToFile(url, partFile))
                .flatMap(sizeBytes -> gateway.move(partFile, target)
                        .onFailure().transform(e ->
                                new AnnaArchiveException("could not move downloaded file: " + e.getMessage()))
                        .replaceWith(sizeBytes));
    }

    private static String fileName(DownloadJob job) {
        String extension = job.extension() == null
                ? null
                : job.extension().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return slug(job.title())
                + "-" + job.md5()
                + (extension == null || extension.isEmpty() ? "" : "." + extension);
    }

    private static String slug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.length() > SLUG_MAX_LENGTH) {
            normalized = normalized.substring(0, SLUG_MAX_LENGTH).replaceAll("-+$", "");
        }
        return normalized.isEmpty() ? "book" : normalized;
    }
}
