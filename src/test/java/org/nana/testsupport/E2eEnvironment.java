package org.nana.testsupport;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


public class E2eEnvironment implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private Optional<String> networkId = Optional.empty();
    private GenericContainer<?> wiremock;

    private static WireMock client;
    private static String wireMockInternalBase;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.networkId = context.containerNetworkId();
    }

    @Override
    public Map<String, String> start() {
        if (networkId.isEmpty()) {
            System.err.println("[E2eEnvironment] WARNING: no shared container network id — "
                    + "the app container will likely be unable to reach WireMock.");
        }

        wiremock = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.13.2"))
                .withExposedPorts(8080)
                .withCommand("--disable-banner")
                .waitingFor(Wait.forHttp("/__admin/mappings").forPort(8080).forStatusCode(200));
        networkId.ifPresent(wiremock::withNetworkMode);
        wiremock.start();

        wireMockInternalBase = "http://" + internalIp(wiremock) + ":8080";
        client = new WireMock(wiremock.getHost(), wiremock.getMappedPort(8080));

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.rest-client.annasarchive.url", wireMockInternalBase);
        config.put("nana.annas-archive.mirror-url", wireMockInternalBase);
        config.put("nana.annas-archive.secret-key", "test-key");
        config.put("nana.webhook.url", wireMockInternalBase + "/webhook");
        config.put("nana.webhook.enabled", "true");
        return config;
    }

    @Override
    public void stop() {
        if (wiremock != null) {
            wiremock.stop();
        }
    }

    public static WireMock wireMock() {
        return client;
    }

    public static String fileUrl(String path) {
        return wireMockInternalBase + path;
    }

    private static String internalIp(GenericContainer<?> container) {
        return container.getCurrentContainerInfo()
                .getNetworkSettings()
                .getNetworks()
                .values()
                .iterator()
                .next()
                .getIpAddress();
    }
}
