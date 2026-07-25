# nana

Self-hosted ebook downloader for Anna's Archive mirrors.

## Features

- Search for books, filter on language, format, content type
- Download from Anna's Archive mirrors (try all mirrors until one succeeds)
- Webhooks on download success/failure
- History of downloads
- Optionnaly use forward auth and username header to store who downloaded what

_Search scrapes the mirror's `/search` HTML (results are hidden inside HTML comments);
downloads use the members-only `/dyn/api/fast_download.json` API and iterate
`domain_index` on failures._

## Screenshots

_Search_

![Search](docs/assets/search_screenshot.png)

_History_

![History](docs/assets/history_screenshot.png)

## Configuration

| Env var                               | Default                    | Purpose                                                                                           |
|---------------------------------------|----------------------------|---------------------------------------------------------------------------------------------------|
| `NANA_AUTH_HEADER_NAME`               | `X-Authentik-Username`     | Trusted username header                                                                           |
| `NANA_AUTH_FALLBACK_USERNAME`         | `unknown`                  | Fallback username when no header                                                                  |
| `NANA_ANNAS_ARCHIVE_MIRROR_URL`       | `https://annas-archive.gd` | Mirror base URL                                                                                   |
| `NANA_ANNAS_ARCHIVE_SECRET_KEY`       | _(empty)_                  | Membership secret key (required for downloads)                                                    |
| `NANA_ANNAS_ARCHIVE_MAX_DOMAIN_INDEX` | `2`                        | Retries `domain_index` 0..N                                                                       |
| `NANA_ALLOWED_FORMATS`                | _(all known formats)_      | Comma-separated allowed formats. Narrow (e.g. `epub,mobi`) to filter search results, downloads and the UI format dropdown |
| `NANA_DOWNLOAD_DIRECTORY`             | `$TMPDIR/nana/downloads`   | Target directory (mount a volume)                                                                 |
| `NANA_DOWNLOAD_TIMEOUT`               | `5M`                       | Download timeout                                                                                  |
| `NANA_WEBHOOK_ENABLED`                | `false`                    | Enable webhooks                                                                                   |
| `NANA_WEBHOOK_URL`                    | _(empty)_                  | Webhook target URL                                                                                |
| `NANA_DB_URL`                         | —                          | PostgreSQL URL, e.g. `postgresql://user:pass@host:5432/db` (prod only; dev/test use Dev Services) |

## Webhooks

When webhooks are enabled, Nana will make a POST request to the configured webhook URL on download success/failure, with the following body:

```json
{
  "event": "download.succeeded",
  "download": {
    "id": 42,
    "md5": "a1b2c3d4e5f67890a1b2c3d4e5f67890",
    "title": "The Great Gatsby",
    "extension": "epub",
    "requestedBy": "lucas",
    "status": "SUCCESS",
    "filePath": "/downloads/the-great-gatsby.epub",
    "sizeBytes": 1048576,
    "errorMessage": null,
    "requestedAt": "2026-07-25T14:32:10Z",
    "finishedAt": "2026-07-25T14:32:45Z"
  }
}
```

On failure, `event` is `download.failed`, `status` is `FAILED`, `errorMessage` describes the
error, and `filePath`/`sizeBytes`/`finishedAt` may be `null`:

```json
{
  "event": "download.failed",
  "download": {
    "id": 43,
    "md5": "f0e1d2c3b4a5968778695a4b3c2d1e0f",
    "title": "Moby Dick",
    "extension": "pdf",
    "requestedBy": "lucas",
    "status": "FAILED",
    "filePath": null,
    "sizeBytes": null,
    "errorMessage": "Mirror returned HTTP 404",
    "requestedAt": "2026-07-25T14:35:00Z",
    "finishedAt": null
  }
}
```

## Development

```sh
./mvnw quarkus:dev            # Dev Services PostgreSQL + Flyway + live reload, UI on :8080
```

Each backend build stores the OpenAPI schema in `src/main/webui/openapi/`; regenerate the
committed API client after changing resources:

```sh
cd src/main/webui
npm install
npm run generate:api          # orval -> src/api/generated/nana.ts
npm run typecheck
```

Tests (WireMock for the mirror + webhook, PostgreSQL Dev Services):

```sh
./mvnw verify
```

## Container image (native, rootless)

```sh
docker build -f src/main/docker/Dockerfile.native-multistage -t nana .
docker run --rm -p 8080:8080 -u 4242:4242 \
  -v nana-downloads:/downloads -e NANA_DOWNLOAD_DIR=/downloads \
  -e NANA_DB_URL=postgresql://nana:...@db:5432/nana \
  -e ANNAS_ARCHIVE_KEY=... \
  nana
```

The image runs as an arbitrary non-root UID/GID (`runAsUser`/`runAsGroup` friendly): the
binary is root-owned and read-only (`0555`), `HOME=/tmp`, and the only writable paths it
needs are `/tmp` and the download directory (provide via volume + `fsGroup`).
