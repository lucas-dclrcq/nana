package org.nana.annasarchive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AnnaArchiveGateway {

    @Inject
    @RestClient
    AnnasArchiveClient fastDownloadClient;

    @ConfigProperty(name = "nana.download.timeout")
    Duration downloadTimeout;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public String resolveDownloadUrl(String md5, int domainIndex) {
        FastDownloadResponse response = fastDownloadClient.fastDownload(md5, 0, domainIndex);
        response.ensureHasUrl();
        return response.downloadUrl();
    }

    // The JDK HTTP client has no body read timeout, so a stalled mirror would block the job
    // thread forever; the future deadline bounds the whole transfer and cancel() aborts it.
    public long streamToFile(String url, Path target) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(downloadTimeout)
                .GET()
                .build();
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            CompletableFuture<HttpResponse<Path>> future = http.sendAsync(request,
                    HttpResponse.BodyHandlers.ofFile(target,
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
            HttpResponse<Path> response;
            try {
                response = future.get(downloadTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new AnnaArchiveException("file download timed out after " + downloadTimeout);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new AnnaArchiveException("file download failed: " + rootMessage(cause));
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new RuntimeException("file download interrupted");
            }
            if (response.statusCode() / 100 != 2) {
                Files.deleteIfExists(target);
                throw new AnnaArchiveException("file download returned HTTP " + response.statusCode());
            }
            long written = Files.size(target);
            if (written == 0) {
                Files.deleteIfExists(target);
                throw new AnnaArchiveException("empty file received");
            }
            return written;
        } catch (IOException e) {
            throw new AnnaArchiveException("file download failed: " + rootMessage(e));
        }
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
