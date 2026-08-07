<img src="docs/assets/title.svg" alt="nana">

[![Latest release](https://img.shields.io/github/v/release/lucas-dclrcq/nana?labelColor=141414&color=ff2d95)](https://github.com/lucas-dclrcq/nana/releases/latest)
![Coverage](.github/badges/jacoco.svg)

Self-hosted ebook downloader for Anna's Archive mirrors.

## Features

- Search for books, filter on language, format, content type
- Download from Anna's Archive mirrors (try all mirrors until one succeeds)
- Webhooks on download success/failure
- History of downloads
- Optionnally use forward auth and username header to store who downloaded what
- Very low on resource usage (around 60mo of RAM)

_Search scrapes the mirror's `/search` HTML (results are hidden inside HTML comments);
downloads use the members-only `/dyn/api/fast_download.json` API and iterate
`domain_index` on failures._

## Screenshots

_Search_

![Search](docs/assets/search_screenshot.png)

_History_

![History](docs/assets/history_screenshot.png)

## User identification

Nana does not handle authentication itself. Instead, it trusts a username header set by
your reverse proxy / forward auth (e.g. Authentik, Authelia) on `/api` requests —
`X-Authentik-Username` by default, configurable via `NANA_AUTH_HEADER_NAME`. The username
is stored with each download (`requestedBy`), shown in the history and included in webhook
payloads. When the header is absent, `NANA_AUTH_FALLBACK_USERNAME` (`unknown` by default)
is used, so the feature is entirely optional.

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

## Deploy with Docker Compose

A ready-to-use example ([docker-compose.yml](docker-compose.yml)) runs the latest image
from `ghcr.io/lucas-dclrcq/nana` alongside PostgreSQL, with named volumes for the
database and downloads:

```sh
docker compose up -d       # UI on http://localhost:8080
```

Set `NANA_ANNAS_ARCHIVE_SECRET_KEY` in the file before starting (required for downloads).

## Container image (native, rootless)

The image runs as an arbitrary non-root UID/GID (`runAsUser`/`runAsGroup` friendly): the
binary is root-owned and read-only (`0555`), `HOME=/tmp`, and the only writable paths it
needs are `/tmp` and the download directory (provide via volume + `fsGroup`).
