package org.nana.shared.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "nana")
public interface NanaConfiguration {
    Auth auth();
    AnnasArchive annasArchive();
    Download download();
    Webhook webhook();

    // Comma-separated allowed ebook formats (env NANA_ALLOWED_FORMATS). Defaults to the parser's
    // full format universe (AnnaArchiveHtmlParser), so an unset config allows every format. Narrow
    // it to restrict search results, downloads and the UI format dropdown.
    @WithDefault("epub,kepub,mobi,azw3,pdf,cbz,djvu,fb2")
    List<String> allowedFormats();

    interface Auth {
        String headerName();
        Optional<String> fallbackUsername();
    }

    interface AnnasArchive {
        String mirrorUrl();
        Optional<String> secretKey();
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
