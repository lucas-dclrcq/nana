package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.nana.testsupport.AnnaStubs.fileUrl;
import static org.nana.testsupport.AnnaStubs.stubFastDownload;
import static org.nana.testsupport.AnnaStubs.stubFile;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
@TestProfile(AllowedFormatsRestrictedTest.RestrictedFormatsProfile.class)
class AllowedFormatsRestrictedTest {

    // Deliberately messy value to exercise FormatPolicy normalization: leading dot, casing,
    // surrounding whitespace and a duplicate all collapse to the ordered set [epub, mobi].
    public static class RestrictedFormatsProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("nana.allowed-formats", ".EPUB, mobi ,epub");
        }
    }

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void configEndpointReturnsTheNormalizedAllowedList() {
        given().header(AUTH)
                .when().get("/api/config")
                .then()
                .statusCode(200)
                .body("allowedFormats", Matchers.contains("epub", "mobi"));
    }

    @Test
    void searchDropsDisallowedFormats() {
        stubSearchWithFixture();

        // The fixture yields an epub result and a pdf result; only the epub survives the filter.
        given().header(AUTH)
                .when().get("/api/search?q=dune")
                .then()
                .statusCode(200)
                .body("size()", Matchers.equalTo(1))
                .body("[0].extension", Matchers.equalTo("epub"));
    }

    @Test
    void rejectsDownloadOfDisallowedFormat() {
        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", "00000000000000000000000000000021", "title", "Dune Messiah", "extension", "pdf"))
                .when().post("/api/downloads")
                .then()
                .statusCode(400)
                .body("message", Matchers.containsString("not allowed"));
    }

    @Test
    void acceptsDownloadWithoutExtensionEvenWhenRestricted() {
        // A null/blank extension is an undetectable format, not a disallowed one, so it is never
        // blocked — this preserves the behaviour of downloads whose format the parser couldn't detect.
        String md5 = "00000000000000000000000000000022";
        stubFastDownload(md5, 0, fileUrl("/files/unknown.bin"), null);
        stubFile("/files/unknown.bin", "BIN".getBytes(StandardCharsets.UTF_8));

        Map<String, String> body = new HashMap<>();
        body.put("md5", md5);
        body.put("title", "Dune Unknown Format");

        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/downloads")
                .then()
                .statusCode(202)
                .body("status", Matchers.equalTo("PENDING"));
    }

    @Test
    void acceptsDownloadOfAllowedFormat() {
        String md5 = "00000000000000000000000000000023";
        stubFastDownload(md5, 0, fileUrl("/files/dune.epub"), null);
        stubFile("/files/dune.epub", "EPUB".getBytes(StandardCharsets.UTF_8));

        given()
                .header(AUTH).contentType(ContentType.JSON)
                .body(Map.of("md5", md5, "title", "Dune", "extension", "epub"))
                .when().post("/api/downloads")
                .then()
                .statusCode(202)
                .body("status", Matchers.equalTo("PENDING"));
    }

    private void stubSearchWithFixture() {
        server().stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(fixture())));
    }

    private String fixture() {
        try (InputStream in = getClass().getResourceAsStream("/annas-search-results.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
