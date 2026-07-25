package org.nana.shared.config;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseUrlConfigSourceFactory implements ConfigSourceFactory {

    private static final String SOURCE_KEY = "nana.db.url"; 
    private static final int ORDINAL = 275;                 // > app.properties(250), < env(300)

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue cv = context.getValue(SOURCE_KEY);
        if (cv == null || cv.getValue() == null || cv.getValue().isBlank()) {
            return List.of(); // dev/test/CI -> don't parse db url because it uses dev services
        }

        URI uri;
        try {
            uri = new URI(cv.getValue().trim());
        } catch (Exception e) {
            throw new IllegalStateException(SOURCE_KEY
                    + " doit ressembler à postgresql://user:pass@host:5432/db"
                    + " (encoder en %XX les caractères spéciaux) : " + e.getMessage(), e);
        }

        int port = uri.getPort();
        String authority = uri.getHost() + (port > 0 ? ":" + port : "");
        String db = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();

        Map<String, String> props = new LinkedHashMap<>();
        props.put("quarkus.datasource.reactive.url", "postgresql://" + authority + "/" + db + query);
        props.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://" + authority + "/" + db + query);

        String rawUserInfo = uri.getRawUserInfo(); // encore percent-encodé ; peut être null
        if (rawUserInfo != null) {
            int c = rawUserInfo.indexOf(':');
            props.put("quarkus.datasource.username",
                    decode(c >= 0 ? rawUserInfo.substring(0, c) : rawUserInfo));
            if (c >= 0) {
                props.put("quarkus.datasource.password", decode(rawUserInfo.substring(c + 1)));
            }
        }

        return List.of(new PropertiesConfigSource(props, "NanaDatabaseUrl", ORDINAL));
    }

    private static String decode(String s) { // décodage RFC-3986, sans le piège '+' -> espace
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
