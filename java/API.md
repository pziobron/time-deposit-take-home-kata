# API contract

The contract is defined in [openapi.yaml](src/main/resources/openapi.yaml).
It specifies exactly two operations:

- `GET /time-deposits`: retrieve all deposits, including their withdrawals.
- `POST /time-deposits/update-balances`: apply one interest update to all deposits;
  return `204 No Content` after saving the updates.

## Assumptions

Each POST triggers one update rather than checking whether a calendar month has
elapsed. Repeating it can compound interest. All deposits are updated in one
transaction; their days and withdrawal records are unchanged. Unknown plan names
continue to receive no interest. Existing calculator arithmetic is preserved.

Nested withdrawals expose the fields specified for the withdrawals table,
including `timeDepositId`. They are returned as recorded history; the requirements
do not specify deducting them during interest calculation. Collection order is
unspecified. Empty collections are represented as arrays, not nulls.

Money is represented as JSON numbers without a two-decimal-place restriction.
The database will use decimal columns, while the shared calculator retains its
existing `Double` contract. Mapping at that boundary must preserve the calculator's
existing behavior rather than introduce a new rounding rule.

## Run locally

Requirements: Java 17 or newer, Maven, and a running Docker-compatible daemon.
Run these commands from the `java/` directory:

```sh
docker compose up -d --wait
mvn spring-boot:run
```

Flyway creates the schema on application startup. The database initially contains
no deposits. In a second terminal, optionally load demo records from `java/`:

```sh
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U time_deposits -d time_deposits < demo-data.sql
```

The application defaults to port 8080 and database port 5432. Database connection
settings can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and
`DATABASE_PASSWORD`. Compose credentials are for local development.

To run the packaged application instead:

```sh
mvn clean verify
java -jar target/time-deposit-kata-1.0-SNAPSHOT.jar
```

## Trigger the endpoints using Swagger

Open <http://localhost:8081> after starting Compose and the application. Swagger UI
loads the checked-in OpenAPI YAML directly. Expand an operation, click **Try it
out**, then **Execute**. The server URL is `http://localhost:8080`.

1. Execute `GET /time-deposits` to inspect the stored deposits and withdrawals.
2. Execute `POST /time-deposits/update-balances` with no body; expect status 204.
3. Execute GET again to inspect the updated balances.

Swagger UI runs separately from the application, which defines exactly the two
business operations. Browser access is allowed from `http://localhost:8081`.
The same operations can be called from a terminal:

```sh
curl http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/update-balances
```

## Persistence and transaction decisions

The adapter uses Spring Data JDBC repositories and PostgreSQL. Flyway migrations create the required
`"timeDeposits"` and `withdrawals` tables, their required columns, a foreign key,
and an index on the withdrawal's deposit ID. Camel-case identifiers are quoted to
match the assignment. Unrestricted `NUMERIC` preserves decimal scale without
imposing a new two-decimal balance rule.

Retrieval uses two bulk queries (deposits and withdrawals), including deposits
with no withdrawals, rather than issuing one withdrawal query per deposit.
Updating loads only calculation inputs, without withdrawal history. Explicit
repository update queries write only balances; aggregate saves are avoided so
withdrawals and deposit metadata cannot be rewritten as a side effect.

`@Transactional` on `TimeDepositService.updateBalances()` covers the complete
read-calculate-save operation. A failed write rolls back all earlier writes.
The repository adapter's save method also participates in that transaction, or
starts one when invoked directly. Updates currently issue one SQL statement per
deposit rather than batching; this keeps writes inside Spring Data repository
methods. For larger datasets, write batching can be considered after measurement.
Ordered row locks serialize updates to existing deposits so concurrent requests
do not overwrite results computed from stale balances. A deposit inserted after
the input query is handled by a subsequent request.

The application service uses Spring transaction annotations; business calculations
and the shared `TimeDeposit` class remain independent of Spring. Persistence mapping
annotations live only on separate entity records inside the database adapter.
Conversion between decimal storage and the shared calculator's `Double` values
happens in the application service. This retains the shared precision limitation;
it does not replace the calculator with exact decimal arithmetic.

## Verification and shutdown

```sh
mvn test          # calculator, application, and MVC tests; Docker not required
mvn clean verify # also runs PostgreSQL Testcontainers integration tests
```

Integration tests use a separate disposable database, run migrations, and exercise
real HTTP requests, Spring Data JDBC persistence, repeated updates, and rollback
after earlier writes have succeeded. Docker is required; integration tests are not silently skipped when it
is unavailable. The first run downloads the PostgreSQL and Testcontainers images.
Coverage is generated at `target/site/jacoco/index.html`.

Stop the application with Ctrl-C and stop the local services with:

```sh
docker compose down
```

The database volume retains local data between runs. To deliberately reset only
this project's local database, run `docker compose down --volumes`.
