# AI-assisted development

## Tools and setup

Development uses the Codex agent integrated into IntelliJ IDEA, with access to
the local repository and terminal. The agent identifies itself as based on GPT-6;
the exact model build and IDE-managed system configuration are not pinned in this
repository. No additional agent framework or project-specific agent configuration
has been introduced at this stage.

The Java module targets Java 17 and uses Maven, JUnit 5, and AssertJ. The initial
test suite was run locally with OpenJDK 25.0.1 and Maven 3.9.9.
The HTTP step adds Spring Boot 3.5.16 web and test starters. Its parent manages
dependency and Maven plugin versions, replacing the individually pinned test
libraries and older compiler/test plugin versions. Java compilation still targets 17.

## Workflow and task instructions

The following instructions summarize the task-specific prompts used so far and
can be reused with a coding agent against this repository:

1. Read the README, existing calculator, shared model, and tests before proposing
   changes. Identify behavioral boundaries and refactoring constraints.
2. Add characterization tests for `TimeDepositCalculator` without changing
   production code. Preserve existing arithmetic and observable results.
3. Use JUnit parameterized tests for repeated input cases. Test outcomes rather
   than implementation details; explain the purpose of each case.
4. Keep the shared `TimeDeposit` contract and the `updateBalance` method signature
   unchanged, as required by the assignment.
5. Run the Java test suite after changes and review the diff. Review generated
   code and expected values with the candidate before proceeding to refactoring.

These are a summary of the substantive task instructions used. Explanatory
questions and the full conversation are omitted. No separate custom system prompt
or agent configuration file has been created for this project. If either is added
later, it should be included in the repository and referenced here. Equivalent
instructions reproduce the workflow; generated code may vary between model
versions and runs.

To run the checks from the repository root, with Java 17 or newer and Maven installed:

```sh
cd java
mvn test
```

## Work assisted so far

- Requirements analysis and planning of a behavior-preserving refactor.
- Replacement of the placeholder assertion with calculator characterization tests:
  plan rates, eligibility boundaries, rounding, zero balances, unknown plans,
  collection handling, repeated updates, and preservation of deposit metadata.
- Conversion of dynamic tests to parameterized tests and addition of the matching
  JUnit dependency.
- Explanation and review of binary floating-point rounding, including why changing
  the monetary arithmetic could violate the requirement to preserve behavior.
- Drafting of this documentation.
- Addition of Lombok at the candidate's request: `@RequiredArgsConstructor` replaces
  explicit constructors in the controller, application service, and persistence
  adapter. Records and the shared model remain unchanged. The Spring Boot parent
  manages the Lombok version; Maven explicitly configures annotation processing
  and excludes Lombok from the executable JAR.
- PostgreSQL persistence and production wiring: Spring Data JDBC repositories,
  separate mapped entity records, Flyway migration, balance-only writes, and a
  narrow calculation-input read. Ordered row locks protect concurrent updates.
- Application-owned `@Transactional` boundaries, replacing the initial
  infrastructure transaction wrapper at the candidate's request. Retrieval uses
  two bulk repository queries; each balance write uses an explicit update query
  rather than an aggregate save, protecting metadata and withdrawal history.
- Eight PostgreSQL Testcontainers integration tests covering retrieval,
  decimal values, balance-only writes, empty storage, live HTTP updates, repeated
  updates, rollback after earlier writes, and waiting for locked rows before calculation.
- Docker Compose setup for PostgreSQL and a separate Swagger UI loading the
  checked-in contract; optional demo SQL and local startup instructions.
- Test-first implementation of the two HTTP operations using a thin Spring MVC
  controller. Four MockMvc tests use a real application service and calculator
  with a test-only repository, verifying JSON fields, date/number formatting,
  empty arrays, and a POST followed by a GET showing the persisted balance change.
- Test-first implementation of application use cases for retrieval and balance
  updates, a repository port, and immutable deposit/withdrawal snapshots. Five
  service tests use a test-only in-memory repository with explicit saving and the
  real calculator, without mocking or adding framework dependencies.
- Organization of application records under `application.model`, the service
  under `application.service`, and the repository interface under `application.port`.
- Refinement of balance persistence to accept only deposit IDs and decimal
  balances through `BalanceUpdate`. The application service converts calculator
  results using `BigDecimal.valueOf`, keeping that conversion out of adapters.
- Definition of the two-operation OpenAPI contract and API assumptions before
  implementation, including response schemas, examples, update semantics, and
  withdrawal representation. Contract validation used
  `npx --yes --package @apidevtools/swagger-cli@4.0.4 swagger-cli validate java/src/main/resources/openapi.yaml`
  from the repository root and passed. No application dependencies were added.
- Addition of JaCoCo reporting and review of line and branch coverage.
- Documentation of calculator assumptions in code comments: annual rates divided
  by 12 and one update per invocation, without scheduling or advancing days.
- Initial calculator refactoring: for-each iteration, extraction of interest
  calculation, and guard clauses, preserving arithmetic and rounding.
- Extraction of basic, student, and premium interest policies, selected by a map
    of existing plan names. Eligibility and rates belong to policies; rounding and
    balance updates remain in the calculator. Unknown plans still earn no interest.

AI assistance supported test design, implementation, and calculator refactoring. The
candidate reviewed the cases interactively, questioned expected values and
redundancy, and discussed the behavior each test protects.

The calculator now delegates interest calculation to plan-specific policies without
intentional behavior changes for valid inputs. Before the application layer was
added, `mvn clean verify` passed all 37 calculator tests and measured
100% line coverage (36/36) and branch coverage (12/12) for the current Java
production code. Coverage confirms execution of branches, not correctness by
itself; expected outcomes and boundary cases were reviewed separately.

After application implementation, `mvn clean verify` passed all 42 tests (37
calculator and five application tests). HTTP adapters, database persistence, and
transaction integration were still to be implemented at that point. The application service
documents that its full read-calculate-save operation must run in one transaction
when infrastructure is wired.

After HTTP implementation, `mvn clean verify` passed all 46 tests with 100% line
coverage (62/62) and branch coverage (12/12). These tests exercise the MVC layer
in a Spring test context, not a live network server. Database persistence,
production bean/startup configuration, and transactions remain for the next step.

To regenerate the coverage report from the repository root:

```sh
cd java
mvn clean verify
```

Open `java/target/site/jacoco/index.html` from the repository root to view the
report. Generated reports remain under the ignored `target/` directory.
This document should be updated as further development is completed.

## Persistence verification

The persistence step uses Spring Data JDBC, PostgreSQL JDBC, Flyway (including the
PostgreSQL module), and Testcontainers. Versions are managed by the existing
Spring Boot parent. Maven Failsafe runs `*IT` integration tests during `verify`;
`mvn test` still runs the 46 calculator/application/MVC tests without Docker.
No production in-memory database or repository is used.

After switching to Spring Data JDBC and annotation-based transactions,
`mvn clean verify` passed 54 tests (46 existing tests and eight PostgreSQL
integration tests), with no failures or skips. JaCoCo recorded 94/96 production
lines and 14/14 branches covered. The two uncovered lines are the main startup
method; executable JAR startup was checked separately with the initial JDBC adapter.

Manual smoke checks started the Compose services and executable JAR, loaded demo
records, issued GET and POST requests, and verified saved balances and unchanged
withdrawals. Swagger UI and its served contract returned HTTP 200, the served YAML
matched the source file, and GET/POST browser-origin CORS checks passed. A browser
UI click-through was not performed. Services started for these checks were stopped;
the local database volume is retained.

The API's annual-rate interpretation, eligibility, and floating-point rounding
remain unchanged. Transactions now use `@Transactional` in the application service;
Spring Data mapping and query annotations stay inside the persistence adapter.
The calculator and shared model have no Spring annotations. The current adapter
issues one SQL update per deposit rather than batching; this is an explicit
simplicity tradeoff, not a measured performance improvement.

## Shared test support

The duplicate in-memory repositories in application and MVC tests were extracted
into `org.ikigaidigital.support.InMemoryTimeDepositRepository` under test sources.
Both suites use its fixture setup/reset methods and immutable snapshots. It does
not simulate locking or transactions; PostgreSQL Testcontainers tests retain that
responsibility. No H2 dependency or production repository changes were introduced.

## Configurable local ports

Compose host ports now use `POSTGRES_PORT` (default 5432) and `SWAGGER_PORT`
(default 8081). Spring's default datasource URL and Swagger CORS origin use the
same exported variables, with `DATABASE_URL` retaining precedence. Documentation
explains exporting variables for both Compose and Maven. An HTTP integration test
checks Swagger preflight access with a non-default port; container-internal ports
and the OpenAPI application's port remain unchanged.

Validation passed: `mvn clean verify` ran all 55 tests successfully, IntelliJ's
build succeeded, and `docker compose config` resolved both default and custom ports.
