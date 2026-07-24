package org.nana.download;

import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Entity
public class Download extends PanacheEntity {

    @Column(nullable = false, length = 32)
    public String md5;

    @Column(nullable = false, length = 1000)
    public String title;

    @Column(length = 500)
    public String author;

    @Column(length = 20)
    public String extension;

    @Column(nullable = false)
    public String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public DownloadStatus status;

    @Column(nullable = false)
    public Instant requestedAt;

    public Instant startedAt;

    public Instant finishedAt;

    @Column(length = 2048)
    public String filePath;

    public Long sizeBytes;

    @Column(length = 2048)
    public String errorMessage;

    public Integer domainIndexUsed;
}
