package org.nana.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.nana.testsupport.AnnaStubs.fastDownloadBody;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nana.testsupport.E2eEnvironment;

@QuarkusIntegrationTest
@QuarkusTestResource(value = E2eEnvironment.class, restrictToAnnotatedClass = true)
class DownloadFlowIT {

    private static final String MD5 = "0123456789abcdef0123456789abcdef";

    private static final String SEARCH_HTML =
            "<div class=\"js-aarecord-list-outer\">"
            + "  <div class=\"flex pt-3\">"
            + "    <a href=\"/md5/" + MD5 + "\"><img src=\"https://covers.example.org/dune.jpg\" alt=\"\"/></a>"
            + "    <div>"
            + "      <a href=\"/md5/" + MD5 + "\"><h3>Dune</h3></a>"
            + "      <div>English [en], .epub, 1.2MB, Book (fiction), 1965</div>"
            + "      <a href=\"/search?q=frank\">Frank Herbert</a>"
            + "    </div>"
            + "  </div>"
            + "</div>";

    @TestHTTPResource("/")
    URL base;

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
        String executable = resolveChromiumExecutable();
        if (executable != null) {
            options.setExecutablePath(java.nio.file.Path.of(executable));
        }
        browser = playwright.chromium().launch(options);
    }

    /**
     * Which Chromium to drive. Explicit override wins (system property / env var). Otherwise, for
     * local dev on an OS Playwright doesn't officially support (e.g. Arch, where the bundled ubuntu
     * build can't render text), fall back to a system Chromium if one is present. In CI the bundled
     * browser is installed with {@code --with-deps} and version-matches the driver, so we keep it.
     */
    private static String resolveChromiumExecutable() {
        String override = System.getProperty("e2e.chromium.executable");
        if (override == null || override.isBlank()) {
            override = System.getenv("E2E_CHROMIUM_EXECUTABLE");
        }
        if (override != null && !override.isBlank()) {
            return override;
        }
        if (System.getenv("CI") != null) {
            return null;
        }
        for (String candidate : new String[] {
                "/usr/bin/chromium", "/usr/bin/chromium-browser", "/usr/bin/google-chrome-stable"}) {
            if (java.nio.file.Files.isExecutable(java.nio.file.Path.of(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        page = context.newPage();
        registerStubs();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    private void registerStubs() {
        WireMock wm = E2eEnvironment.wireMock();
        wm.resetMappings();
        wm.register(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("dune"))
                .withQueryParam("ext", equalTo("epub"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(SEARCH_HTML)));
        wm.register(get(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(MD5))
                .willReturn(okJson(fastDownloadBody(E2eEnvironment.fileUrl("/files/dune.epub"), null, 21, 25))));
        wm.register(get(urlPathEqualTo("/files/dune.epub"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody("DUNE".getBytes(StandardCharsets.UTF_8))));
        wm.register(post(urlEqualTo("/webhook")).willReturn(aResponse().withStatus(200)));
    }

    @Test
    void searchWithFilterThenDownloadThenBadgeQuotaHistory() {
        page.navigate(base.toString());

        page.locator("select.bg-pop-yellow").selectOption("epub");
        page.getByRole(AriaRole.SEARCHBOX).fill("dune");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();

        Locator card = page.locator("li.pop-card")
                .filter(new Locator.FilterOptions().setHasText("Dune"))
                .first();
        Locator downloadButton = card.locator("button.pop-btn");
        assertThat(downloadButton).hasText("Download");
        downloadButton.click();

        assertThat(downloadButton).hasText("Success ✓",
                new LocatorAssertions.HasTextOptions().setTimeout(30_000));

        assertThat(page.locator("main span.pop-badge.bg-pop-cyan"))
                .containsText("21 / 25",
                        new LocatorAssertions.ContainsTextOptions().setTimeout(30_000));

        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("History")).click();
        Locator row = page.locator("table tbody tr")
                .filter(new Locator.FilterOptions().setHasText("Dune"))
                .first();
        assertThat(row.locator("span.pop-badge")).containsText("Success",
                new LocatorAssertions.ContainsTextOptions().setTimeout(30_000));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                E2eEnvironment.wireMock().verifyThat(postRequestedFor(urlEqualTo("/webhook"))));
    }
}
