package org.nana.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class AuthFilterTest {

    @Test
    void rejectsApiRequestWithoutHeader() {
        given()
                .when().get("/api/downloads")
                .then()
                .statusCode(401)
                .body("message", containsString("X-Authentik-Username"));
    }

    @Test
    void acceptsApiRequestWithHeader() {
        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/downloads")
                .then()
                .statusCode(200);
    }

    @Test
    void doesNotGuardNonApiPaths() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200);
    }
}
