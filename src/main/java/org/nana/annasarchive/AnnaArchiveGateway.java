package org.nana.annasarchive;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.nana.annasarchive.FastDownloadResponse.AccountFastDownloadInfo;
import org.nana.shared.config.NanaConfiguration;

import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class AnnaArchiveGateway {

    private final AnnasArchiveClient annasArchiveClient;
    private final Vertx vertx;
    private final NanaConfiguration config;
    private final FastDownloadQuotaStore quotaStore;

    public AnnaArchiveGateway(@RestClient AnnasArchiveClient annasArchiveClient, Vertx vertx, NanaConfiguration config,
            FastDownloadQuotaStore quotaStore) {
        this.annasArchiveClient = annasArchiveClient;
        this.vertx = vertx;
        this.config = config;
        this.quotaStore = quotaStore;
    }

    public Uni<String> resolveDownloadUrl(String md5, int domainIndex) {
        return annasArchiveClient.fastDownload(md5, 0, domainIndex)
                .flatMap(response -> {
                    response.ensureHasUrl();
                    return persistQuota(response.accountFastDownloadInfo()).replaceWith(response.downloadUrl());
                });
    }

    private Uni<Void> persistQuota(AccountFastDownloadInfo info) {
        if (info == null || info.downloadsLeft() == null || info.downloadsPerDay() == null) {
            return Uni.createFrom().voidItem();
        }
        return quotaStore.save(info.downloadsLeft(), info.downloadsPerDay())
                .onFailure().invoke(t -> Log.warnf(t, "Could not persist fast download quota"))
                .onFailure().recoverWithNull();
    }
    
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
        return annasArchiveClient.download(url)
                .onItem().call(chunk -> file.write(Buffer.buffer(chunk)))
                .onItem().ignoreAsUni()
                .ifNoItem().after(config.download().timeout()).fail()
                .onItemOrFailure().transformToUni((ignored, failure) -> {
                    if (failure != null) {
                        return closeQuietly(file)
                                .flatMap(closed -> deleteQuietly(targetPath))
                                .flatMap(deleted -> Uni.createFrom().failure(mapDownloadFailure(failure)));
                    }
                    return file.close()
                            .onFailure().transform(e ->
                                    new AnnaArchiveException("could not close downloaded file: " + rootMessage(e)))
                            .flatMap(closed -> verifySize(targetPath));
                });
    }

    private Uni<Long> verifySize(String targetPath) {
        return vertx.fileSystem().props(targetPath).flatMap(props -> {
            long written = props.size();
            if (written == 0) {
                return deleteQuietly(targetPath).flatMap(ignored -> Uni.createFrom()
                        .failure(new AnnaArchiveException("empty file received")));
            }
            return Uni.createFrom().item(written);
        });
    }

    private AnnaArchiveException mapDownloadFailure(Throwable failure) {
        if (failure instanceof TimeoutException || failure.getClass().getSimpleName().contains("Timeout")) {
            return new AnnaArchiveException("file download timed out after " + config.download().timeout().toMillis() + "ms");
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

    private Uni<Void> closeQuietly(AsyncFile file) {
        return file.close().onFailure().recoverWithItem((Void) null);
    }

    private Uni<Void> deleteQuietly(String path) {
        return vertx.fileSystem().delete(path).onFailure().recoverWithNull();
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
