package org.nana.annasarchive;

import io.quarkus.hibernate.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import java.util.List;
import org.hibernate.annotations.processing.HQL;

public interface DdosGuardCookiesRepository extends PanacheRepository.Reactive<DdosGuardCookies, Long> {

    @HQL("order by id desc")
    Uni<List<DdosGuardCookies>> all();
}
