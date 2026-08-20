package org.nana.annasarchive;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.WithId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;

@Entity
public class DdosGuardCookies extends WithId.AutoLong implements PanacheEntity.Reactive {

    @Column(nullable = false, length = 8192)
    public String cookieHeader;

    @Column(nullable = false)
    public Instant updatedAt;
}
