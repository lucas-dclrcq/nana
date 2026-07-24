package org.nana.annasarchive;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.codec.BodyCodec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AnnaArchiveGateway {

    @Inject
    @RestClient
    AnnasArchiveClient fastDownloadClient;

    @Inject
    Vertx vertx;

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "nana.download.timeout")
    Duration downloadTimeout;

    public Uni<String> resolveDownloadUrl(String md5, int domainIndex) {
        return fastDownloadClient.fastDownload(md5, 0, domainIndex)
                .map(response -> {
                    response.ensureHasUrl();
                    return response.downloadUrl();
                });
    }

    // The WebClient request timeout bounds the whole transfer and releases the AsyncFile; the
    // body is streamed straight to disk so a large file never buffers in memory.
    public Uni<Long> streamToFile(String url, Path target) {
        String targetPath = target.toString();
        OpenOptions options = new OpenOptions().setCreate(true).setWrite(true).setTruncateExisting(true);
        return ensureParent(target)
                .flatMap(ignored -> vertx.fileSystem().open(targetPath, options))
                .flatMap(file -> download(url, file, targetPath));
    }

    public Uni<Void> move(Path source, Path target) {
        return vertx.fileSystem().move(source.toString(), target.toString(),
                new CopyOptions().setAtomicMove(true).setReplaceExisting(true));
    }

    public Uni<Void> deleteQuietly(Path path) {
        if (path == null) {
            return Uni.createFrom().voidItem();
        }
        return deleteQuietly(path.toString());
    }

    private Uni<Long> download(String url, AsyncFile file, String targetPath) {
        return webClient.getAbs(url)
                .followRedirects(true)
                .timeout(downloadTimeout.toMillis())
                .as(BodyCodec.pipe(file))
                .send()
                .onItemOrFailure().transformToUni((response, failure) -> {
                    if (failure != null) {
                        return file.close().onFailure().recoverWithItem((Void) null)
                                .flatMap(ignored -> deleteQuietly(targetPath))
                                .flatMap(ignored -> Uni.createFrom().failure(mapDownloadFailure(failure)));
                    }
                    return handleResponse(response, targetPath);
                });
    }

    private Uni<Long> handleResponse(HttpResponse<Void> response, String targetPath) {
        if (response.statusCode() / 100 != 2) {
            return deleteQuietly(targetPath).flatMap(ignored -> Uni.createFrom()
                    .failure(new AnnaArchiveException("file download returned HTTP " + response.statusCode())));
        }
        return vertx.fileSystem().props(targetPath).flatMap(props -> {
            long written = props.size();
            if (written == 0) {
                return deleteQuietly(targetPath).flatMap(ignored -> Uni.<Long>createFrom()
                        .failure(new AnnaArchiveException("empty file received")));
            }
            return Uni.createFrom().item(written);
        });
    }

    private AnnaArchiveException mapDownloadFailure(Throwable failure) {
        if (failure instanceof TimeoutException || failure.getClass().getSimpleName().contains("Timeout")) {
            return new AnnaArchiveException("file download timed out after " + downloadTimeout);
        }
        return new AnnaArchiveException("file download failed: " + rootMessage(failure));
    }

    private Uni<Void> ensureParent(Path target) {
        Path parent = target.getParent();
        if (parent == null) {
            return Uni.createFrom().voidItem();
        }
        return vertx.fileSystem().mkdirs(parent.toString());
    }

    private Uni<Void> deleteQuietly(String path) {
        return vertx.fileSystem().delete(path).onFailure().recoverWithItem((Void) null);
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message != null ? message : cause.getClass().getSimpleName();
    }
}
