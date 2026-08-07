package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownloadAllIndexes;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@WithTestResource(WireMockResource.class)
class MetricsResourceTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void exposesMetricsWithoutAuthentication() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("nana_annas_quota_remaining"))
                .body(Matchers.containsString("nana_annas_quota_limit"))
                .body(Matchers.containsString("nana_search_duration_seconds"))
                .body(Matchers.containsString("nana_download_duration_seconds"))
                .body(Matchers.containsString("nana_downloads_total"));
    }

    @Test
    void recordsSearchDurationByStatus() {
        server().stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(fixture())));

        given().header(AUTH)
                .when().get("/api/search?q=dune")
                .then().statusCode(200);

        awaitMetrics(body -> metricValue(body, "nana_search_duration_seconds_count{status=\"success\"") > 0);

        server().resetAll();
        server().stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(500).withBody("boom")));

        given().header(AUTH)
                .when().get("/api/search?q=dune")
                .then().statusCode(502);

        awaitMetrics(body -> metricValue(body, "nana_search_duration_seconds_count{status=\"error\"") > 0);
    }

    @Test
    void recordsSuccessfulDownloadAndQuota() {
        String md5 = "00000000000000000000000000000e01";
        stubFastDownloadAllIndexes(md5, fileUrl("/files/metrics.epub"), 7, 25);
        stubFile("/files/metrics.epub", "EPUB-METRICS".getBytes(StandardCharsets.UTF_8));

        int id = createDownload(md5, "Metrics Book");
        awaitStatus(id, "SUCCESS");

        awaitMetrics(body -> body.contains("nana_downloads_total{status=\"success\"}")
                && body.contains("nana_downloads_total{status=\"requested\"}")
                && body.contains("nana_download_duration_seconds_count{status=\"success\"")
                && body.contains("nana_annas_quota_remaining 7.0")
                && body.contains("nana_annas_quota_limit 25.0"));
    }

    @Test
    void recordsFailedDownload() {
        String md5 = "00000000000000000000000000000e02";
        stubFastDownloadAllIndexes(md5, null, "Not a member");

        int id = createDownload(md5, "Failing Book");
        awaitStatus(id, "FAILED");

        awaitMetrics(body -> body.contains("nana_downloads_total{status=\"failed\"}")
                && body.contains("nana_download_duration_seconds_count{status=\"failed\""));
    }

    private static double metricValue(String body, String seriesPrefix) {
        return body.lines()
                .filter(line -> line.startsWith(seriesPrefix))
                .mapToDouble(line -> Double.parseDouble(line.substring(line.lastIndexOf(' ') + 1)))
                .findFirst()
                .orElse(0);
    }

    private void awaitMetrics(java.util.function.Predicate<String> condition) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            String body = given()
                    .when().get("/q/metrics")
                    .then().statusCode(200)
                    .extract().asString();
            org.junit.jupiter.api.Assertions.assertTrue(condition.test(body));
        });
    }

    private int createDownload(String md5, String title) {
        return given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", title, "extension", "epub"))
                .when().post("/api/downloads")
                .then()
                .statusCode(202)
                .extract().path("id");
    }

    private void awaitStatus(int id, String expected) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                given().header(AUTH)
                        .when().get("/api/downloads/" + id)
                        .then().body("status", Matchers.equalTo(expected)));
    }

    private String fixture() {
        try (InputStream in = getClass().getResourceAsStream("/annas-search-results.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
