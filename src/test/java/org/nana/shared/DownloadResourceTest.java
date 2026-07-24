package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.nana.testsupport.AnnaStubs.fastDownloadBody;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownload;
import static org.nana.testsupport.AnnaStubs.stubFastDownloadAllIndexes;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.AnnaStubs.stubFileStatus;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class DownloadResourceTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void downloadsFileToDiskOnHappyPath() throws Exception {
        String md5 = "00000000000000000000000000000001";
        byte[] epub = "EPUB-BYTES-0123456789".getBytes(StandardCharsets.UTF_8);
        stubFastDownload(md5, 0, fileUrl("/files/dune.epub"), null);
        stubFile("/files/dune.epub", epub);

        int id = given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", "Dune", "author", "Frank Herbert", "extension", "epub"))
                .when().post("/api/downloads")
                .then()
                .statusCode(202)
                .body("md5", Matchers.equalTo(md5))
                .body("status", Matchers.equalTo("PENDING"))
                .body("requestedBy", Matchers.equalTo("lucas"))
                .extract().path("id");

        awaitStatus(id, "SUCCESS");

        String filePath = given().header(AUTH)
                .when().get("/api/downloads/" + id)
                .then()
                .statusCode(200)
                .body("domainIndexUsed", Matchers.equalTo(0))
                .body("sizeBytes", Matchers.equalTo(epub.length))
                .body("errorMessage", Matchers.nullValue())
                .body("startedAt", Matchers.notNullValue())
                .body("finishedAt", Matchers.notNullValue())
                .extract().path("filePath");

        assertTrue(filePath.endsWith("dune-" + md5 + ".epub"), filePath);
        Path file = Path.of(filePath);
        assertTrue(Files.exists(file));
        assertArrayEquals(epub, Files.readAllBytes(file));

        server().verify(getRequestedFor(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(md5))
                .withQueryParam("key", equalTo("test-key"))
                .withQueryParam("path_index", equalTo("0"))
                .withQueryParam("domain_index", equalTo("0")));
    }

    @Test
    void iteratesDomainIndexesUntilOneSucceeds() {
        String md5 = "00000000000000000000000000000002";
        byte[] pdf = "PDF-BYTES".getBytes(StandardCharsets.UTF_8);
        stubFastDownload(md5, 0, null, "No servers available");
        stubFastDownload(md5, 1, fileUrl("/files/b1.pdf"), null);
        stubFileStatus("/files/b1.pdf", 404);
        stubFastDownload(md5, 2, fileUrl("/files/b2.pdf"), null);
        stubFile("/files/b2.pdf", pdf);

        int id = createDownload(md5, "Dune Messiah", "pdf");
        awaitStatus(id, "SUCCESS");

        given().header(AUTH)
                .when().get("/api/downloads/" + id)
                .then()
                .body("domainIndexUsed", Matchers.equalTo(2))
                .body("sizeBytes", Matchers.equalTo(pdf.length))
                .body("filePath", Matchers.endsWith(".pdf"));

        for (int domainIndex = 0; domainIndex <= 2; domainIndex++) {
            server().verify(getRequestedFor(urlPathEqualTo("/dyn/api/fast_download.json"))
                    .withQueryParam("md5", equalTo(md5))
                    .withQueryParam("domain_index", equalTo(String.valueOf(domainIndex))));
        }
    }

    @Test
    void marksFailedWhenAllDomainIndexesAreExhausted() {
        String md5 = "00000000000000000000000000000003";
        stubFastDownloadAllIndexes(md5, null, "Not a member");

        int id = createDownload(md5, "Children of Dune", "epub");
        awaitStatus(id, "FAILED");

        given().header(AUTH)
                .when().get("/api/downloads/" + id)
                .then()
                .body("errorMessage", Matchers.equalTo("Not a member"))
                .body("filePath", Matchers.nullValue())
                .body("finishedAt", Matchers.notNullValue());

        server().verify(3, getRequestedFor(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(md5)));
    }

    @Test
    void rejectsInvalidMd5() {
        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", "not-a-md5", "title", "Dune"))
                .when().post("/api/downloads")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsMissingTitle() {
        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", "00000000000000000000000000000009"))
                .when().post("/api/downloads")
                .then()
                .statusCode(400);
    }

    @Test
    void rejectsDuplicateActiveDownload() {
        String md5 = "00000000000000000000000000000004";
        server().stubFor(get(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(md5))
                .willReturn(okJson(fastDownloadBody(null, "slow mirror")).withFixedDelay(1500)));

        int id = createDownload(md5, "Dune again", "epub");

        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", "Dune again", "extension", "epub"))
                .when().post("/api/downloads")
                .then()
                .statusCode(409)
                .body("message", Matchers.containsString("already"));

        awaitStatus(id, "FAILED");
    }

    @Test
    void unknownDownloadIs404() {
        given().header(AUTH)
                .when().get("/api/downloads/999999")
                .then()
                .statusCode(404);
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
