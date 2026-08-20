package org.nana.shared;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.nana.testsupport.WireMockResource.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.vertx.VertxContextSupport;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.annasarchive.DdosGuardCookieStore;
import org.nana.testsupport.TestDataSupport;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@WithTestResource(WireMockResource.class)
@TestProfile(FlareSolverrSearchTest.FlareSolverrEnabled.class)
class FlareSolverrSearchTest {

    private static final String COOKIE_HEADER = "__ddg1_=abc; __ddg2_=def";

    public static class FlareSolverrEnabled implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("nana.flaresolverr.enabled", "true");
        }
    }

    @Inject
    DdosGuardCookieStore cookieStore;

    @Inject
    TestDataSupport testData;

    @BeforeEach
    void reset() {
        server().resetAll();
        try {
            VertxContextSupport.subscribeAndAwait(() -> cookieStore.invalidate());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Test
    void proxiesSearchThroughFlareSolverrAndParsesTheReturnedHtml() {
        stubFlareSolverrOk(fixture());

        search()
                .statusCode(200)
                .body("size()", Matchers.equalTo(2))
                .body("[0].md5", Matchers.equalTo("1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d"))
                .body("[0].title", Matchers.equalTo("Dune"))
                .body("[1].md5", Matchers.equalTo("beefbeefbeefbeefbeefbeefbeefbeef"));

        server().verify(postRequestedFor(urlPathEqualTo("/v1"))
                .withRequestBody(containing("request.get"))
                .withRequestBody(containing("/search"))
                .withRequestBody(containing("q=dune")));
    }

    @Test
    void reusesTheSolvedCookiesToSearchTheMirrorDirectly() {
        stubFlareSolverrOk(fixture());
        server().stubFor(get(urlPathEqualTo("/search"))
                .withHeader("Cookie", equalTo(COOKIE_HEADER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(fixture())));

        // First search pays the challenge through FlareSolverr; the second rides its cookies.
        search().statusCode(200);
        search()
                .statusCode(200)
                .body("size()", Matchers.equalTo(2));

        server().verify(1, postRequestedFor(urlPathEqualTo("/v1")));
        server().verify(1, getRequestedFor(urlPathEqualTo("/search"))
                .withHeader("Cookie", equalTo(COOKIE_HEADER)));
    }

    @Test
    void persistsTheSolvedCookiesInTheDatabase() {
        stubFlareSolverrOk(fixture());

        search().statusCode(200);

        org.junit.jupiter.api.Assertions.assertEquals(COOKIE_HEADER, testData.ddosGuardCookieHeader());
    }

    @Test
    void usesCookiesPersistedInTheDatabaseWithoutReSolving() {
        // Simulates a restart: cookies exist in the database but not in the in-memory cache.
        testData.insertDdosGuardCookies(COOKIE_HEADER);
        server().stubFor(get(urlPathEqualTo("/search"))
                .withHeader("Cookie", equalTo(COOKIE_HEADER))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(fixture())));

        search()
                .statusCode(200)
                .body("size()", Matchers.equalTo(2));

        server().verify(0, postRequestedFor(urlPathEqualTo("/v1")));
    }

    @Test
    void solvesTheChallengeAgainWhenTheMirrorRejectsTheCookies() {
        stubFlareSolverrOk(fixture());
        server().stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(403).withBody("DDOS-Guard")));

        // First search stores cookies; the mirror then rejects them, forcing a re-solve.
        search().statusCode(200);
        search()
                .statusCode(200)
                .body("size()", Matchers.equalTo(2));

        server().verify(1, getRequestedFor(urlPathEqualTo("/search")));
        server().verify(2, postRequestedFor(urlPathEqualTo("/v1")));
    }

    @Test
    void flareSolverrErrorYieldsBadGateway() {
        server().stubFor(post(urlPathEqualTo("/v1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"message\":\"challenge not solved\"}")));

        search()
                .statusCode(502)
                .body("message", Matchers.equalTo("Anna's Archive search failed"));
    }

    private ValidatableResponse search() {
        return given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/search?q=dune")
                .then();
    }

    private void stubFlareSolverrOk(String html) {
        server().stubFor(post(urlPathEqualTo("/v1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(okEnvelope(html))));
    }

    private static String okEnvelope(String html) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(Map.of(
                    "status", "ok",
                    "message", "Challenge solved",
                    "solution", Map.of(
                            "url", "https://annas-archive.gd/search?q=dune",
                            "status", 200,
                            "response", html,
                            "userAgent", "Mozilla/5.0 (X11; Linux x86_64)",
                            "cookies", List.of(
                                    Map.of("name", "__ddg1_", "value", "abc"),
                                    Map.of("name", "__ddg2_", "value", "def")))));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String fixture() {
        try (InputStream in = getClass().getResourceAsStream("/annas-search-results.html")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
