package org.nana.testsupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.nana.download.DownloadRepository;

@ApplicationScoped
public class TestDataSupport {

    @Inject
    DownloadRepository repository;

    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional
    public void setRequestedAt(long id, Instant requestedAt) {
        repository.findById(id).requestedAt = requestedAt;
    }
}
