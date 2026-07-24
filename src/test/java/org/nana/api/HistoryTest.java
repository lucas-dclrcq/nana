package org.nana.api;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.nana.download.DownloadStateStore;
import org.nana.testsupport.TestDataSupport;
import org.nana.testsupport.WireMockResource;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class HistoryTest {

    private static final Header AUTH = new Header("X-Authentik-Username", "lucas");

    @Inject
    DownloadStateStore stateStore;

    @Inject
    TestDataSupport testData;

    @Test
    void paginatesHistoryMostRecentFirst() {
        testData.deleteAll();
        Instant now = Instant.now();
        long oldest = stateStore.createPending("0000000000000000000000000000000a", "Oldest", null, "epub", "lucas").id;
        long middle = stateStore.createPending("0000000000000000000000000000000b", "Middle", null, "epub", "lucas").id;
        long newest = stateStore.createPending("0000000000000000000000000000000c", "Newest", null, "epub", "lucas").id;
        testData.setRequestedAt(oldest, now.minus(3, ChronoUnit.HOURS));
        testData.setRequestedAt(middle, now.minus(2, ChronoUnit.HOURS));
        testData.setRequestedAt(newest, now.minus(1, ChronoUnit.HOURS));

        given().header(AUTH)
                .when().get("/api/downloads?page=0&size=2")
                .then()
                .statusCode(200)
                .body("totalElements", Matchers.equalTo(3))
                .body("totalPages", Matchers.equalTo(2))
                .body("page", Matchers.equalTo(0))
                .body("size", Matchers.equalTo(2))
                .body("content.size()", Matchers.equalTo(2))
                .body("content[0].title", Matchers.equalTo("Newest"))
                .body("content[1].title", Matchers.equalTo("Middle"));

        given().header(AUTH)
                .when().get("/api/downloads?page=1&size=2")
                .then()
                .statusCode(200)
                .body("page", Matchers.equalTo(1))
                .body("content.size()", Matchers.equalTo(1))
                .body("content[0].title", Matchers.equalTo("Oldest"));
    }
}
