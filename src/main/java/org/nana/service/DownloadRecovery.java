package org.nana.service;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nana.domain.Download;
import org.nana.domain.DownloadRepository;
import org.nana.domain.DownloadStatus;

@ApplicationScoped
public class DownloadRecovery {

    @Inject
    DownloadRepository repository;

    @ConfigProperty(name = "nana.download.directory")
    Path downloadDirectory;

    void onStart(@Observes StartupEvent event) {
        failStrandedDownloads();
        sweepPartFiles();
    }

    // Jobs live only in this JVM's executor: any non-terminal row at boot was orphaned by a
    // crash or restart and would otherwise block its md5 forever through the 409 rule.
    @Transactional
    void failStrandedDownloads() {
        List<Download> stranded = repository.byStatusIn(List.of(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING));
        for (Download download : stranded) {
            download.status = DownloadStatus.FAILED;
            download.finishedAt = Instant.now();
            download.errorMessage = "interrupted by application restart";
            Log.warnf("Download %d (md5 %s) marked failed: interrupted by application restart",
                    download.id, download.md5);
        }
    }

    private void sweepPartFiles() {
        if (!Files.isDirectory(downloadDirectory)) {
            return;
        }
        try (Stream<Path> files = Files.list(downloadDirectory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".part"))
                    .forEach(this::deleteOrphan);
        } catch (IOException e) {
            Log.warnf("Could not sweep partial downloads in %s: %s", downloadDirectory, e.getMessage());
        }
    }

    private void deleteOrphan(Path path) {
        try {
            Files.deleteIfExists(path);
            Log.infof("Deleted orphaned partial download %s", path);
        } catch (IOException e) {
            Log.warnf("Could not delete orphaned partial download %s: %s", path, e.getMessage());
        }
    }
}
