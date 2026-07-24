package org.nana.download;

import io.quarkus.signals.Receives;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.nana.shared.ApiDtos.DownloadDto;

// Fans every download state change out to the connected SSE clients. It consumes the same
// quarkus-signals events as WebhookNotifier (signals allow several consumers), plus the two
// intermediate signals, so the stream carries the full PENDING -> DOWNLOADING -> SUCCESS/FAILED
// lifecycle. The processor is hot: late subscribers only receive events published after they
// connect, which is why the frontend resynchronises on (re)connection.
@ApplicationScoped
public class DownloadEventStream {

    private final BroadcastProcessor<DownloadDto> processor = BroadcastProcessor.create();

    Uni<Void> onPending(@Receives DownloadPending event) {
        return emit(event.download());
    }

    Uni<Void> onDownloading(@Receives DownloadDownloading event) {
        return emit(event.download());
    }

    Uni<Void> onSucceeded(@Receives DownloadSucceeded event) {
        return emit(event.download());
    }

    Uni<Void> onFailed(@Receives DownloadFailed event) {
        return emit(event.download());
    }

    public Multi<DownloadDto> stream() {
        return processor;
    }

    private Uni<Void> emit(DownloadDto download) {
        processor.onNext(download);
        return Uni.createFrom().voidItem();
    }
}
