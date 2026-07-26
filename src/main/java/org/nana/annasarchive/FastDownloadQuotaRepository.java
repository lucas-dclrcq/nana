package org.nana.annasarchive;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import org.hibernate.annotations.processing.HQL;

public interface FastDownloadQuotaRepository extends PanacheRepository.Reactive<FastDownloadQuota, Long> {

    @HQL("order by id desc")
    Uni<List<FastDownloadQuota>> all();
}
