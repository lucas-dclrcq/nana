package org.nana.shared;

import io.smallrye.config.ConfigMapping;

import java.nio.file.Path;
import java.time.Duration;

@ConfigMapping(prefix = "nana")
public interface NanaConfiguration {
    AnnasArchive annasArchive();
    Download download();
    Webhook webhook();
    
    interface AnnasArchive {
        String mirrorUrl();
        String secretKey();
        Long maxDomainIndex();
    }
    
    interface Download {
        Path directory();
        Duration timeout();
    }
    
    interface Webhook {
        boolean enabled();
        String url();
    }
}
