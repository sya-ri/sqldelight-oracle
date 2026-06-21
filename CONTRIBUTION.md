# Contribution

## Development Checks

Unit tests use Kotest `FunSpec`.

Run the local release-blocking checks before publishing or opening release work:

```shell
./gradlew check
./gradlew publishToMavenLocal
./gradlew releaseCheck
```

`releaseCheck` runs module checks, Dokka generation, ABI validation, Kover verification, and `publishToMavenLocal` for the published artifacts.

## Oracle Testcontainers

Oracle read/write verification uses Testcontainers with the Oracle XE module when a real database is required.
The tests are gated because they require Docker and a large database container:

```shell
ORACLE_TESTCONTAINERS=true ./gradlew :sqldelight-oracle-dialect:test
```

Set `ORACLE_TESTCONTAINERS_IMAGE` to override the default `gvenzl/oracle-xe:21-slim-faststart` image.
For example, this runs the same gated tests against Oracle Free 23:

```shell
ORACLE_TESTCONTAINERS=true \
ORACLE_TESTCONTAINERS_IMAGE=gvenzl/oracle-free:23-slim-faststart \
ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1 \
./gradlew :sqldelight-oracle-dialect:test
```

Set `ORACLE_TESTCONTAINERS_SERVICE_NAME` when the image exposes a different JDBC service name from Testcontainers' Oracle XE default.
The container uses a 2 GiB `/dev/shm` by default; set `ORACLE_TESTCONTAINERS_SHM_BYTES` to override it.

## Static Analysis

Kotlinter runs as part of `check` and `releaseCheck`.
Qodana runs in GitHub Actions using `qodana.yaml`; keep local fixes compatible with that CI gate.
