# Project instructions

## Scope and compatibility

- Read `README.md` and the relevant code and tests before making changes.
- Work on the Java solution unless another language is explicitly requested.
- Preserve the package, public constructor, method signatures, and behavior of
  the shared `TimeDeposit` class and `TimeDepositCalculator.updateBalance` method.
- Preserve interest eligibility, rates, operation order, and existing floating-point
  rounding behavior. Treat arithmetic corrections as separate behavior changes.
- Keep exactly two business API operations, consistent with
  `java/src/main/resources/openapi.yaml`.

## Design and testing

- Separate HTTP adapters, application services, business rules, and persistence.
  Business calculations must not depend on HTTP or database implementations.
- Prefer records for immutable data and constructor injection for collaborators.
  Use Lombok for class boilerplate when it preserves the intended public contract.
- Keep changes small and focused. Add abstractions only for a concrete benefit.
- Add meaningful outcome-based tests before changing behavior or refactoring.
  Cover relevant boundaries without redundant cases or unnecessary mocking.
- Use the shared in-memory repository for application and MVC tests. Use
  PostgreSQL Testcontainers for SQL, transaction, and locking verification.
- Review generated code, remove dead code, and explain significant tradeoffs.

## Verification and documentation

- Run `mvn -f java/pom.xml test` after relevant changes.
- Before finishing implementation changes, run `mvn -f java/pom.xml clean verify`.
  Docker must be running; do not silently skip integration tests when unavailable.
- Run `git diff --check` and review the final diff. Use coverage as a guide, not
  as proof that behavior is correct.
- Document logical assumptions in code comments. Keep API and startup instructions
  in `java/API.md` consistent with the implementation.
- Update `AI_USAGE.md` for substantive AI-assisted work and `java/SECURITY.md` when
  dependency-security decisions change. Do not suppress findings merely to clear warnings.
- Use US English in comments and documentation.
- Follow atomic commit practices. Do not publish, push, or send the submission
  email without explicit user instructions.
