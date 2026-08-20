package org.nana.testsupport;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;

public class WireMockResource implements QuarkusTestResourceLifecycleManager {

    private static WireMockServer server;

    public static WireMockServer server() {
        return server;
    }

    @Override
    public Map<String, String> start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        String base = "http://localhost:" + server.port();
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.rest-client.annasarchive.url", base);
        config.put("nana.annas-archive.mirror-url", base);
        config.put("nana.annas-archive.secret-key", "test-key");
        config.put("nana.webhook.url", base + "/webhook");
        // Point the FlareSolverr client at the same WireMock server; only exercised when
        // nana.flaresolverr.enabled is turned on (e.g. via a test profile).
        config.put("quarkus.rest-client.flaresolverr.url", base);
        return config;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
}
