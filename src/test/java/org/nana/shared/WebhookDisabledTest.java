package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownload;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
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
@QuarkusTestResource(WireMockResource.class)
class WebhookDisabledTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void sendsNothingWhenWebhooksAreDisabled() throws Exception {
        server().stubFor(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
        String md5 = "00000000000000000000000000000008";
        stubFastDownload(md5, 0, fileUrl("/files/w4.epub"), null);
        stubFile("/files/w4.epub", "NO-WEBHOOK".getBytes(StandardCharsets.UTF_8));

        int id = given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", "Heretics of Dune", "extension", "epub"))
                .when().post("/api/downloads")
                .then()
                .statusCode(202)
                .extract().path("id");

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                given().header(AUTH)
                        .when().get("/api/downloads/" + id)
                        .then().body("status", Matchers.equalTo("SUCCESS")));

        Thread.sleep(500);
        server().verify(0, postRequestedFor(urlEqualTo("/webhook")));
    }
}
