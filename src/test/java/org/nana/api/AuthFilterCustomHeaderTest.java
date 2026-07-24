package org.nana.api;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
@TestProfile(AuthFilterCustomHeaderTest.CustomHeaderProfile.class)
class AuthFilterCustomHeaderTest {

    public static class CustomHeaderProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("nana.auth.header-name", "X-Custom-User");
        }
    }

    @Test
    void defaultHeaderNoLongerAuthenticates() {
        given()
                .header("X-Authentik-Username", "lucas")
                .when().get("/api/downloads")
                .then()
                .statusCode(401);
    }

    @Test
    void configuredHeaderAuthenticates() {
        given()
                .header("X-Custom-User", "alice")
                .when().get("/api/downloads")
                .then()
                .statusCode(200);
    }
}
