package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownload;
import static org.nana.testsupport.AnnaStubs.stubFastDownloadAllIndexes;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(value = WireMockResource.class, restrictToAnnotatedClass = true)
@TestProfile(WebhookTest.WebhookEnabledProfile.class)
class WebhookTest {

    public static class WebhookEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("nana.webhook.enabled", "true");
        }
    }

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void sendsSucceededWebhookWithFullPayload() {
        server().stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
        String md5 = "00000000000000000000000000000005";
        stubFastDownload(md5, 0, fileUrl("/files/w1.epub"), null);
        stubFile("/files/w1.epub", "WEBHOOK-EPUB".getBytes(StandardCharsets.UTF_8));

        int id = createDownload(md5, "Dune", "epub");
        awaitStatus(id, "SUCCESS");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                server().verify(postRequestedFor(urlEqualTo("/webhook"))
                        .withHeader("Content-Type", equalTo("application/json"))
                        .withRequestBody(matchingJsonPath("$.event", equalTo("download.succeeded")))
                        .withRequestBody(matchingJsonPath("$.download.id", equalTo(String.valueOf(id))))
                        .withRequestBody(matchingJsonPath("$.download.md5", equalTo(md5)))
                        .withRequestBody(matchingJsonPath("$.download.title", equalTo("Dune")))
                        .withRequestBody(matchingJsonPath("$.download.extension", equalTo("epub")))
                        .withRequestBody(matchingJsonPath("$.download.requestedBy", equalTo("lucas")))
                        .withRequestBody(matchingJsonPath("$.download.status", equalTo("SUCCESS")))
                        .withRequestBody(matchingJsonPath("$.download.sizeBytes", equalTo("12")))
                        .withRequestBody(matchingJsonPath("$.download.filePath"))
                        .withRequestBody(matchingJsonPath("$.download.requestedAt"))
                        .withRequestBody(matchingJsonPath("$.download.finishedAt"))));
    }

    @Test
    void sendsFailedWebhookWithErrorMessage() {
        server().stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
        String md5 = "00000000000000000000000000000006";
        stubFastDownloadAllIndexes(md5, null, "Not a member");

        int id = createDownload(md5, "Dune Messiah", "pdf");
        awaitStatus(id, "FAILED");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                server().verify(postRequestedFor(urlEqualTo("/webhook"))
                        .withRequestBody(matchingJsonPath("$.event", equalTo("download.failed")))
                        .withRequestBody(matchingJsonPath("$.download.md5", equalTo(md5)))
                        .withRequestBody(matchingJsonPath("$.download.status", equalTo("FAILED")))
                        .withRequestBody(matchingJsonPath("$.download.errorMessage", equalTo("Not a member")))));
    }

    @Test
    void webhookFailureDoesNotAffectDownloadStatus() {
        server().stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(500)));
        String md5 = "00000000000000000000000000000007";
        stubFastDownload(md5, 0, fileUrl("/files/w3.epub"), null);
        stubFile("/files/w3.epub", "WEBHOOK-EPUB-3".getBytes(StandardCharsets.UTF_8));

        int id = createDownload(md5, "God Emperor of Dune", "epub");
        awaitStatus(id, "SUCCESS");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                server().verify(postRequestedFor(urlEqualTo("/webhook"))
                        .withRequestBody(matchingJsonPath("$.event", equalTo("download.succeeded")))));

        given().header(AUTH)
                .when().get("/api/downloads/" + id)
                .then()
                .statusCode(200)
                .body("status", Matchers.equalTo("SUCCESS"))
                .body("errorMessage", Matchers.nullValue());
    }

    private int createDownload(String md5, String title, String extension) {
        return given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", title, "extension", extension))
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
}
