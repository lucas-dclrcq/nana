package org.nana.download;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DownloadRecovery {

    private final DownloadRepository repository;
    private final DownloadRecovery self;
    private final Path downloadDirectory;

    public DownloadRecovery(DownloadRepository repository, DownloadRecovery self, @ConfigProperty(name = "nana.download.directory") Path downloadDirectory) {
        this.repository = repository;
        this.self = self;
        this.downloadDirectory = downloadDirectory;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            VertxContextSupport.subscribeAndAwait(() -> self.failStrandedDownloads());
        } catch (Throwable t) {
            Log.errorf(t, "Could not fail stranded downloads at startup");
        }
        sweepPartFiles();
    }

    // Jobs live only in this JVM's event loop: any non-terminal row at boot was orphaned by a
    // crash or restart and would otherwise block its md5 forever through the 409 rule.
    @WithTransaction
    public Uni<Void> failStrandedDownloads() {
        return repository.byStatusIn(List.of(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING))
                .invoke(stranded -> stranded.forEach(download -> {
                    download.status = DownloadStatus.FAILED;
                    download.finishedAt = Instant.now();
                    download.errorMessage = "interrupted by application restart";
                    Log.warnf("Download %d (md5 %s) marked failed: interrupted by application restart",
                            download.id, download.md5);
                }))
                .replaceWithVoid();
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
