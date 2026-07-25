package org.nana.shared;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConfigResourceTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @Test
    void exposesTheFullFormatUniverseByDefault() {
        given().header(AUTH)
                .when().get("/api/config")
                .then()
                .statusCode(200)
                .body("allowedFormats", Matchers.contains(
                        "epub", "kepub", "mobi", "azw3", "pdf", "cbz", "djvu", "fb2"));
    }
}
