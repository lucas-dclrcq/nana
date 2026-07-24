# nana

Self-hosted ebook downloader for Anna's Archive mirrors. Search books from a web UI, queue
server-side downloads into a configurable directory, keep history in PostgreSQL, and get
webhooks on success/failure. Designed to run on Kubernetes behind an authenticating proxy
(e.g. Authentik forward auth).

- Backend: Quarkus 3.37 (Java 21), experimental `quarkus-data-hibernate` (Panache-next),
  Flyway, PostgreSQL.
- Frontend: Vue 3 + Vite + Tailwind CSS v4 + TanStack Query, served by Quinoa; API client
  generated with Orval from the OpenAPI schema (committed under `src/main/webui/src/api/generated`).
- Search scrapes the mirror's `/search` HTML (results are hidden inside HTML comments);
  downloads use the members-only `/dyn/api/fast_download.json` API and iterate
  `domain_index` on failures.

## Authentication

`nana` trusts the reverse proxy: every `/api/*` request must carry the username header
(default `X-Authentik-Username`), otherwise it gets a 401. Never expose the app without a
proxy that strips/sets this header. In dev mode a `dev` fallback user is used.

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `NANA_AUTH_HEADER_NAME` | `X-Authentik-Username` | Trusted username header |
| `ANNAS_ARCHIVE_MIRROR_URL` | `https://annas-archive.org` | Mirror base URL |
| `ANNAS_ARCHIVE_KEY` | _(empty)_ | Membership secret key (required for downloads) |
| `ANNAS_ARCHIVE_MAX_DOMAIN_INDEX` | `2` | Retries `domain_index` 0..N |
| `NANA_DOWNLOAD_DIR` | `$TMPDIR/nana/downloads` | Target directory (mount a volume) |
| `NANA_WEBHOOK_ENABLED` | `false` | Enable webhooks |
| `NANA_WEBHOOK_URL` | _(empty)_ | Webhook target URL |
| `DB_JDBC_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | PostgreSQL (prod only; dev/test use Dev Services) |

Webhook payload: `{"event": "download.succeeded" | "download.failed", "download": {id, md5,
title, extension, requestedBy, status, filePath, sizeBytes, errorMessage, requestedAt,
finishedAt}}`.

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
  -e DB_JDBC_URL=jdbc:postgresql://db:5432/nana -e DB_USERNAME=nana -e DB_PASSWORD=... \
  -e ANNAS_ARCHIVE_KEY=... \
  nana
```

The image runs as an arbitrary non-root UID/GID (`runAsUser`/`runAsGroup` friendly): the
binary is root-owned and read-only (`0555`), `HOME=/tmp`, and the only writable paths it
needs are `/tmp` and the download directory (provide via volume + `fsGroup`).
