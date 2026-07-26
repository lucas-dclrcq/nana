package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.nana.testsupport.WireMockResource.server;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(value = WireMockResource.class, restrictToAnnotatedClass = true)
class SearchResourceTest {

    @BeforeEach
    void resetStubs() {
        server().resetAll();
    }

    @Test
    void parsesCommentWrappedSearchResults() {
        stubSearchWithFixture();

        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/search?q=dune")
                .then()
                .statusCode(200)
                .body("size()", Matchers.equalTo(2))
                .body("[0].md5", Matchers.equalTo("1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d"))
                .body("[0].title", Matchers.equalTo("Dune"))
                .body("[0].author", Matchers.equalTo("Frank Herbert"))
                .body("[0].extension", Matchers.equalTo("epub"))
                .body("[0].sizeBytes", Matchers.equalTo(1258291))
                .body("[0].language", Matchers.equalTo("en"))
                .body("[0].year", Matchers.equalTo(1965))
                .body("[0].coverUrl", Matchers.equalTo("https://covers.example.org/dune.jpg"))
                .body("[1].md5", Matchers.equalTo("beefbeefbeefbeefbeefbeefbeefbeef"))
                .body("[1].title", Matchers.equalTo("Dune Messiah"))
                .body("[1].author", Matchers.equalTo("Frank Herbert"))
                .body("[1].extension", Matchers.equalTo("pdf"))
                .body("[1].sizeBytes", Matchers.equalTo(512000))
                .body("[1].language", Matchers.equalTo("fr"))
                .body("[1].year", Matchers.equalTo(1969))
                .body("[1].coverUrl", Matchers.nullValue());
    }

    @Test
    void relaysFiltersToTheMirror() {
        stubSearchWithFixture();

        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/search?q=dune&lang=fr&ext=pdf&content=book_fiction")
                .then()
                .statusCode(200);

        server().verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("dune"))
                .withQueryParam("lang", equalTo("fr"))
                .withQueryParam("ext", equalTo("pdf"))
                .withQueryParam("content", equalTo("book_fiction")));
    }

    @Test
    void missingQueryIsRejected() {
        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/search")
                .then()
                .statusCode(400);
    }

    @Test
    void mirrorFailureYieldsBadGateway() {
        server().stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(500).withBody("boom")));

        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/search?q=dune")
                .then()
                .statusCode(502)
                .body("message", Matchers.equalTo("Anna's Archive search failed"));
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
