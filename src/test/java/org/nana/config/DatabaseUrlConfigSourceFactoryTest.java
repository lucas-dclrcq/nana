package org.nana.config;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.nana.shared.config.DatabaseUrlConfigSourceFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseUrlConfigSourceFactoryTest {

    private static SmallRyeConfig configFor(String url) {
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(Map.of("nana.db.url", url), "test", 400))
                .withSources(new DatabaseUrlConfigSourceFactory())
                .build();
    }

    @Test
    void derivesAllFourDatasourceKeysFromASingleUrl() {
        SmallRyeConfig config = configFor("postgresql://nana:secret@localhost:5432/nana");

        assertEquals("postgresql://localhost:5432/nana",
                config.getConfigValue("quarkus.datasource.reactive.url").getValue());
        assertEquals("jdbc:postgresql://localhost:5432/nana",
                config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertEquals("nana", config.getConfigValue("quarkus.datasource.username").getValue());
        assertEquals("secret", config.getConfigValue("quarkus.datasource.password").getValue());
    }

    @Test
    void percentDecodesCredentialsAndStripsThemFromTheUrls() {
        // na%40na -> na@na, p%40ss -> p@ss ; les URLs ne doivent PAS contenir d'identifiants.
        SmallRyeConfig config = configFor("postgresql://na%40na:p%40ss@localhost:5432/nana");

        assertEquals("na@na", config.getConfigValue("quarkus.datasource.username").getValue());
        assertEquals("p@ss", config.getConfigValue("quarkus.datasource.password").getValue());
        assertEquals("postgresql://localhost:5432/nana",
                config.getConfigValue("quarkus.datasource.reactive.url").getValue());
        assertEquals("jdbc:postgresql://localhost:5432/nana",
                config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    void forwardsQueryStringIntoBothUrls() {
        SmallRyeConfig config = configFor("postgresql://nana:secret@db.internal:5432/nana?sslmode=require");

        assertEquals("postgresql://db.internal:5432/nana?sslmode=require",
                config.getConfigValue("quarkus.datasource.reactive.url").getValue());
        assertEquals("jdbc:postgresql://db.internal:5432/nana?sslmode=require",
                config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
    }

    @Test
    void contributesNothingWhenUrlIsAbsent() {
        // Pas de nana.db.url -> aucune propriété dérivée -> Dev Services prennent le relais.
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new DatabaseUrlConfigSourceFactory())
                .build();

        assertNull(config.getConfigValue("quarkus.datasource.jdbc.url").getValue());
        assertNull(config.getConfigValue("quarkus.datasource.reactive.url").getValue());
    }
}
