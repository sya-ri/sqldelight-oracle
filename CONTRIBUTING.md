# Contributing

## Branches

sqldelight-oracle uses a stable `main` branch and release-line development branches.

- `main` points at the latest published release.
- `release/0.x` is the development branch for the current pre-1.0 release line.
- Feature, fix, and dependency update pull requests should target `release/0.x`.
- Release preparation is merged into `release/0.x` first, then `main` is advanced to the published release commit after artifacts are published.
- A publish pull request from `release/0.x` to `main` is created automatically when `release/0.x` is updated.
- Do not include agent-specific names in branch names.

## Pull Requests

Open pull requests against `release/0.x` unless a maintainer explicitly asks for another base branch.

Use short, descriptive titles without agent-specific prefixes. Keep each pull request focused on one behavior, fix, or release task.

## Development Checks

Unit tests use Kotest `FunSpec`.
SQLDelight parser and PSI classes are generated from `sqldelight-oracle-dialect/src/main/kotlin/dev/s7a/sqldelight/oracle/dialects/oracle/grammar/Oracle.bnf` during Gradle builds.
Generated files under `build/grammars` are not committed.

Run the local release-blocking checks before publishing or opening release work:

```shell
./gradlew check
./gradlew releaseCheck
```

`releaseCheck` runs root and module checks, including ABI validation through the Kotlin `check` lifecycle.
Run `publishToMavenLocal` only when you need to inspect generated publications locally.

## Oracle Testcontainers

Oracle read/write verification uses Testcontainers with the Oracle XE module when a real database is required.
The tests are gated because they require Docker and a large database container:

```shell
ORACLE_TESTCONTAINERS=true ./gradlew :sqldelight-oracle-dialect:test
```

Set `ORACLE_TESTCONTAINERS_IMAGE` to override the default `gvenzl/oracle-xe:21-slim-faststart` image.
For example, this runs the same gated tests against Oracle AI Database 26ai:

```shell
ORACLE_TESTCONTAINERS=true \
ORACLE_TESTCONTAINERS_IMAGE=gvenzl/oracle-free:23.26.2-slim-faststart \
ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1 \
./gradlew :sqldelight-oracle-dialect:test
```

Set `ORACLE_TESTCONTAINERS_SERVICE_NAME` when the image exposes a different JDBC service name from Testcontainers' Oracle XE default.
The container uses a 2 GiB `/dev/shm` by default; set `ORACLE_TESTCONTAINERS_SHM_BYTES` to override it.

Parser-backed tests can also validate their SQL against Oracle. This is separate from the smoke tests
so normal parser checks stay fast locally. CI runs release checks with Oracle validation enabled:

```shell
RUN_ORACLE_VALIDATION=true ./gradlew :sqldelight-oracle-dialect:test --tests '*OracleParserBackedTest'
```

Use the same image override variables as the smoke tests when validating against a different Oracle image:

```shell
RUN_ORACLE_VALIDATION=true \
ORACLE_TESTCONTAINERS_IMAGE=gvenzl/oracle-free:23.26.2-slim-faststart \
ORACLE_TESTCONTAINERS_SERVICE_NAME=FREEPDB1 \
./gradlew :sqldelight-oracle-dialect:test --tests '*OracleParserBackedTest'
```

By default, parser-backed SQL is split into Oracle statements, SQLDelight query labels are removed,
DML is validated with `EXPLAIN`, and other statements are executed inside a rolled-back transaction.
Tests that use SQLDelight placeholders or otherwise need different Oracle-ready SQL should provide
explicit Oracle validation metadata with literal values or bind metadata.
Validation failures are reported in the test output. Set `ORACLE_VALIDATION_STRICT=true` to fail the
test run on the first parser-backed test with Oracle validation failures.

## Static Analysis

Kotlinter runs as part of `check` and `releaseCheck`.
Qodana runs in GitHub Actions using `qodana.yaml`; keep local fixes compatible with that CI gate.

## Publishing

Publish pull requests target `main` from `release/0.x`. Merge the publish pull request with a merge commit only when the release branch is ready to become the latest published state.

Do not squash or rebase publish pull requests. A merge commit preserves the release branch commits and keeps the next publish pull request based on the previous published merge point.

Do not push directly to `main`. If `main` needs to advance, merge the publish pull request.

Publishing is not automated yet. Use this checklist for each release.

1. Confirm the release version on the publish branch:

   ```shell
   ./gradlew -q printVersion
   ```

2. Wait for required CI on the release pull request into `release/0.x`.

3. Merge the release pull request into `release/0.x`.
   Use the merge method required by the branch ruleset. Required status checks must be green before merging.

4. Wait for the automatically created publish pull request from `release/0.x` to `main`.

5. Wait for required CI on the publish pull request.

6. Merge the publish pull request into `main` with a merge commit.
   Do not squash or rebase the publish pull request.

7. Publish from the `main` merge commit:

   ```shell
   ./gradlew --no-daemon releaseCheck
   ./gradlew --no-daemon publishAndReleaseToMavenCentral
   ```

8. Verify published artifacts:

   ```shell
   curl -fsSL \
       https://repo.maven.apache.org/maven2/dev/s7a/sqldelight/oracle/sqldelight-oracle-dialect/<version>/sqldelight-oracle-dialect-<version>.pom
   curl -fsSL \
       https://repo.maven.apache.org/maven2/dev/s7a/sqldelight/oracle/sqldelight-check-oracle-dialect/<version>/sqldelight-check-oracle-dialect-<version>.pom
   curl -fsSL \
       https://repo.maven.apache.org/maven2/dev/s7a/sqldelight/oracle/sqldelight-check-oracle-rule/<version>/sqldelight-check-oracle-rule-<version>.pom
   ```

   Maven Central can lag after a successful publish. The GitHub Release can be created before Central is visible when the publish task completed successfully.

9. Create the GitHub Release after publishing.
   Target the `main` merge commit, use tag `v<version>`, and include the published artifact coordinates in the release notes.

Before merging a publish pull request, verify:

- `./gradlew --no-daemon releaseCheck`
- The CI Qodana job is green for the release commit, or run the local Qodana Docker command above with the repository `qodana.yaml` configuration when verifying outside CI.
- Maven Central publication metadata for all three artifacts.
