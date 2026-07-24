package org.nana.download;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.nana.annasarchive.AnnaArchiveException;
import org.nana.annasarchive.AnnaArchiveGateway;
import org.nana.api.ApiDtos.DownloadDto;
import org.nana.download.DownloadStateStore.DownloadJob;
import org.nana.webhook.WebhookNotifier;

@ApplicationScoped
public class DownloadJobRunner {

    private static final int SLUG_MAX_LENGTH = 80;

    @Inject
    DownloadStateStore stateStore;

    @Inject
    AnnaArchiveGateway gateway;

    @Inject
    WebhookNotifier webhookNotifier;

    @Inject
    ManagedExecutor executor;

    @ConfigProperty(name = "nana.download.directory")
    Path downloadDirectory;

    @ConfigProperty(name = "nana.annas-archive.max-domain-index")
    int maxDomainIndex;

    public void start(long downloadId) {
        executor.execute(() -> {
            try {
                run(downloadId);
            } catch (RuntimeException e) {
                Log.errorf(e, "Download %d crashed", downloadId);
                try {
                    webhookNotifier.downloadFailed(
                            stateStore.markFailed(downloadId, "internal error: " + e.getMessage()));
                } catch (RuntimeException ignored) {
                    // the download stays in its last persisted state; the crash is already logged
                }
            }
        });
    }

    private void run(long downloadId) {
        DownloadJob job = stateStore.markDownloading(downloadId);
        Log.infof("Download %d started (md5 %s)", downloadId, job.md5());
        String lastError = "no download attempt was made";
        for (int domainIndex = 0; domainIndex <= maxDomainIndex; domainIndex++) {
            Path partFile = null;
            try {
                String url = gateway.resolveDownloadUrl(job.md5(), domainIndex);
                Path target = downloadDirectory.resolve(fileName(job));
                partFile = target.resolveSibling(target.getFileName() + ".part");
                long sizeBytes = gateway.streamToFile(url, partFile);
                Files.move(partFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                DownloadDto result = stateStore.markSuccess(downloadId, target.toString(), sizeBytes, domainIndex);
                Log.infof("Download %d succeeded: %s (%d bytes, domain index %d)",
                        downloadId, target, sizeBytes, domainIndex);
                webhookNotifier.downloadSucceeded(result);
                return;
            } catch (AnnaArchiveException e) {
                lastError = e.getMessage();
                Log.warnf("Download %d attempt with domain index %d failed: %s", downloadId, domainIndex, lastError);
                deleteQuietly(partFile);
            } catch (IOException e) {
                lastError = "could not move downloaded file: " + e.getMessage();
                Log.warnf("Download %d attempt with domain index %d failed: %s", downloadId, domainIndex, lastError);
                deleteQuietly(partFile);
            }
        }
        DownloadDto result = stateStore.markFailed(downloadId, lastError);
        Log.errorf("Download %d failed: %s", downloadId, lastError);
        webhookNotifier.downloadFailed(result);
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

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort cleanup of a partial download
        }
    }
}
