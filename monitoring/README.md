# Monitoring

Nana expose des métriques Prometheus sur `GET /q/metrics` (port applicatif, `8080` par défaut). L'endpoint n'est pas authentifié par le filtre `X-Authentik-Username` (qui ne couvre que `/api/*`) — restreignez son accès au niveau du reverse proxy si nécessaire.

## Scrape Prometheus

```yaml
scrape_configs:
  - job_name: nana
    metrics_path: /q/metrics
    static_configs:
      - targets: ["<nana-host>:8080"]
```

## Dashboard Grafana

Importer [`nana-grafana-dashboard.json`](nana-grafana-dashboard.json) : **Dashboards → New → Import**, puis sélectionner la datasource Prometheus. Le dashboard fournit quatre rangées : recherche (taux et latence p50/p95/p99), téléchargements (volumes, taux et durée p95 par statut), quota fast download Anna's Archive, et HTTP/upstream.

## Métriques custom

| Métrique | Type | Tags | Description |
|---|---|---|---|
| `nana_search_duration_seconds` | Timer (histogramme) | `status=success\|error` | Durée des recherches Anna's Archive ; `_count` donne le nombre de recherches par statut |
| `nana_download_duration_seconds` | Timer (histogramme) | `status=success\|failed` | Durée des téléchargements (du démarrage à l'état terminal) |
| `nana_downloads_total` | Counter | `status=requested\|success\|failed` | Nombre de téléchargements par étape du cycle de vie |
| `nana_annas_quota_remaining` | Gauge | — | Fast downloads Anna's Archive restants aujourd'hui (`-1` tant qu'inconnu) |
| `nana_annas_quota_limit` | Gauge | — | Fast downloads autorisés par jour (`-1` tant qu'inconnu) |

Les gauges de quota sont mises à jour à chaque téléchargement (la réponse de l'API Anna's Archive contient le quota) et réamorcées au démarrage depuis la base ; filtrez avec `>= 0` pour ignorer la valeur « inconnue ».

S'y ajoutent les métriques automatiques de Quarkus/Micrometer : `http_server_requests_seconds` (requêtes entrantes), `http_client_requests_seconds` (appels sortants vers Anna's Archive), JVM et système.
