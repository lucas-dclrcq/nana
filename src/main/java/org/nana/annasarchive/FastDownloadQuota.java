package org.nana.annasarchive;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.WithId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class FastDownloadQuota extends WithId.AutoLong implements PanacheEntity.Reactive {

    @Column(nullable = false)
    public int remaining;

    @Column(nullable = false)
    public int total;

    @Column(nullable = false)
    public Instant updatedAt;
}
