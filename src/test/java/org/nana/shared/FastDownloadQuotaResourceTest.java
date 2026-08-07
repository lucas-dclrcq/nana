package org.nana.shared;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownloadAllIndexes;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.TestDataSupport;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@WithTestResource(WireMockResource.class)
class FastDownloadQuotaResourceTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @Inject
    TestDataSupport testData;

    @BeforeEach
    void reset() {
        server().resetAll();
        testData.reset();
    }

    @Test
    void quotaIsUnknownBeforeAnyDownload() {
        given().header(AUTH)
                .when().get("/api/quota")
                .then()
                .statusCode(200)
                .body("remaining", Matchers.nullValue())
                .body("total", Matchers.nullValue())
                .body("updatedAt", Matchers.nullValue());
    }

    @Test
    void quotaReflectsLastDownloadAndUpdatesInPlace() {
        downloadWithQuota("000000000000000000000000000000a1", "/files/q1.epub", 21, 25);
        given().header(AUTH)
                .when().get("/api/quota")
                .then()
                .statusCode(200)
                .body("remaining", Matchers.equalTo(21))
                .body("total", Matchers.equalTo(25))
                .body("updatedAt", Matchers.notNullValue());

        downloadWithQuota("000000000000000000000000000000b2", "/files/q2.epub", 20, 25);
        given().header(AUTH)
                .when().get("/api/quota")
                .then()
                .statusCode(200)
                .body("remaining", Matchers.equalTo(20))
                .body("total", Matchers.equalTo(25));

        assertEquals(1L, testData.quotaCount());
    }

    private void downloadWithQuota(String md5, String filePath, int downloadsLeft, int downloadsPerDay) {
        stubFastDownloadAllIndexes(md5, fileUrl(filePath), downloadsLeft, downloadsPerDay);
        stubFile(filePath, "EPUB".getBytes(StandardCharsets.UTF_8));
        int id = given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", "Book", "extension", "epub"))
                .when().post("/api/downloads")
                .then().statusCode(202)
                .extract().path("id");
        awaitStatus(id, "SUCCESS");
    }

    private void awaitStatus(int id, String expected) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                given().header(AUTH)
                        .when().get("/api/downloads/" + id)
                        .then().body("status", Matchers.equalTo(expected)));
    }
}
