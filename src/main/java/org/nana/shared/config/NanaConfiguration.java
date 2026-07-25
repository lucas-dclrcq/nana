package org.nana.shared.config;

import io.smallrye.config.ConfigMapping;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

@ConfigMapping(prefix = "nana")
public interface NanaConfiguration {
    Auth auth();
    AnnasArchive annasArchive();
    Download download();
    Webhook webhook();

    interface Auth {
        String headerName();
        Optional<String> fallbackUsername();
    }

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
        Optional<String> url();
    }
}
