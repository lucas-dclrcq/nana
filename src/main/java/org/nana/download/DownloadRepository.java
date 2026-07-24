package org.nana.download;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import java.util.List;
import org.hibernate.annotations.processing.HQL;

public interface DownloadRepository extends PanacheRepository.Reactive<Download, Long> {

    @HQL("order by requestedAt desc")
    Uni<Page<Download>> history(PageRequest pageRequest);

    @HQL("where md5 = :md5 and status in :statuses")
    Uni<List<Download>> byMd5AndStatusIn(String md5, List<DownloadStatus> statuses);

    @HQL("where status in :statuses")
    Uni<List<Download>> byStatusIn(List<DownloadStatus> statuses);

    default Uni<Boolean> activeExists(String md5) {
        return byMd5AndStatusIn(md5, List.of(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING))
                .map(list -> !list.isEmpty());
    }
}
