package org.nana.testsupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.nana.testsupport.WireMockResource.server;

public final class AnnaStubs {

    private AnnaStubs() {}

    public static String fileUrl(String path) {
        return server().baseUrl() + path;
    }

    public static void stubFastDownload(String md5, int domainIndex, String downloadUrl, String error) {
        server().stubFor(get(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(md5))
                .withQueryParam("domain_index", equalTo(String.valueOf(domainIndex)))
                .willReturn(okJson(fastDownloadBody(downloadUrl, error))));
    }

    public static void stubFastDownloadAllIndexes(String md5, String downloadUrl, String error) {
        server().stubFor(get(urlPathEqualTo("/dyn/api/fast_download.json"))
                .withQueryParam("md5", equalTo(md5))
                .willReturn(okJson(fastDownloadBody(downloadUrl, error))));
    }

    public static String fastDownloadBody(String downloadUrl, String error) {
        return "{\"download_url\":" + jsonString(downloadUrl) + ",\"error\":" + jsonString(error) + "}";
    }

    public static void stubFile(String path, byte[] body) {
        server().stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(body)));
    }

    public static void stubFileStatus(String path, int status) {
        server().stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse().withStatus(status)));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value.replace("\"", "\\\"") + "\"";
    }
}
