# AGENTS.md

Project-specific instructions for coding agents working in this repository.

## Database migrations (Flyway)

- Never delete, rename, or modify an already-applied Flyway migration. Files under
  `src/main/resources/db/migration/` are append-only once released: any change to a file
  that has already been executed on a database breaks Flyway's checksum validation and
  will make the next deploy fail.
- To change the schema, add a **new** migration with the next version number
  (e.g. `V4__drop_ddos_guard_cookies.sql`) instead of editing an existing one.
- When removing a feature that created a table/sequence, drop that object in a new
  migration — keep the original migration file untouched.

## Tests

- Run the full suite with `./mvnw verify`.
- Run a subset with `./mvnw test -Dtest='ClassName,OtherClass'`.
- Regenerating the backend build stores the OpenAPI schema in `src/main/webui/openapi/`;
  revert those files if the change is only a version drift, not an API change.
