# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Multi-tenant customer store with tenant-defined "generic fields" (boolean, integer, string, ISO 8601 datetime).
Java 25, Spring Boot 4.1.x, Maven multi-module, PostgreSQL 17. The solution is extended in follow-up sessions: the
user gives a high-level goal and we agree on how to reach it before anything is coded. The user is a senior backend
engineer: be critical, point out risks, and propose alternatives.

## Workflow: plan first, code only after approval

1. **Goal in, plan out. Do not write or edit code, tests, config or docs when given a task.** Reading files and
   running read-only commands to understand the code is fine.
2. **Present a plan** and wait. A plan contains:
   - the goal as you understood it, plus open questions and assumptions;
   - the proposed steps in order, naming the modules, classes and migrations touched;
   - design choices with the alternatives you rejected and why, and what each costs;
   - risks (data migration, backward compatibility, concurrency, performance);
   - **the testing strategy** (see below): which tests at which level, which scenarios and edge cases, which
     existing tests change.
3. **Code only after explicit approval** ("approved", "go ahead" or similar). Silence, "ok, interesting" or answers
   to your questions are not approval. Approval covers the plan as presented: if the work needs a different
   approach, an extra dependency or a changed public API, stop and re-plan.
4. **Coding routine**, in this order:
   1. implement the plan step by step, tests together with the code they cover;
   2. `./mvnw spotless:apply`;
   3. `./mvnw verify` (Docker running); fix failures at the cause, never by weakening a test;
   4. report honestly what passed, failed or was skipped, and update the README if behaviour changed.
5. **Clean up**: kill every process you started (app, background jobs), run `docker compose down` for containers you
   started, delete temporary files created outside the scratchpad.

## Commands

Use the Maven wrapper (no local Maven installed).

- Format: `./mvnw spotless:apply` (`spotless:check` runs in `validate` and fails the build on drift)
- Everything: `./mvnw verify`; fast loop without Docker: `./mvnw test`
- One module: `./mvnw verify -pl :infrastructure -am` (`-am` builds upstream modules; required for infrastructure)
- One unit test: `./mvnw test -pl :domain -Dtest=ClassName#methodName`
- One integration test: `./mvnw verify -pl :infrastructure -am -Dit.test=ClassName -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false`
- Local database: `docker compose up -d` (Postgres 17, db/user/password `customers`)
- Run the app: `./mvnw spring-boot:run -pl :infrastructure`
- Tests need a running Docker daemon (Docker Desktop; `docker` is on the PowerShell PATH, not in Git Bash).
- `http/` holds manual REST Client requests; keep them working when the API changes.

## Architecture

- Exactly three Maven modules under `modules/`: `domain` <- `application` <- `infrastructure`. Dependencies point
  inward only; never add a fourth module.
- `domain`: plain Java (records, sealed types), JSpecify annotations only. Holds aggregates, value objects, domain
  exceptions and the repository interfaces (the domain states what persistence it needs).
- `application`: plain Java, no Spring, no annotations. One inbound port interface per use case (`execute`), a
  `*Service` implementation, a `*Command` record where input is needed.
- `infrastructure`: Spring Boot, REST, JDBC, Flyway. Beans are wired in `ApplicationConfiguration`. Spring, Jackson
  and JDBC types never leave this module.
- `Customer` and `GenericFieldDefinition` are separate aggregates. Customers refer to definitions by name only;
  use cases check rules spanning both by passing the loaded definitions into the customer operation.
  `JdbcCustomerRepository` also loads definitions itself, to type stored values on read.
- Definitions are register-only (no rename, retype or delete), which keeps stored values valid forever.
- Persistence: `JdbcClient` with explicit SQL, no JPA. Generic field values live in a JSONB column and are typed on
  read using their definitions. A stored row that fails validation on read is a server fault: the repository
  wraps the `DomainValidationException` in an `IllegalStateException` (500, logged), never a 400. Flyway
  migrations are append-only: never edit an applied one.
- Concurrency: optimistic locking with a `version` column. Repositories report persistence facts (conflict,
  duplicate); use cases decide the outcome.
- Identity: a customer has a private database surrogate key and a public id. Only the public id appears in the
  domain, API, errors and logs. The public id is immutable.
- The tenant comes from the `X-Tenant-Id` header only (`TenantIdArgumentResolver`), never from bodies. Tenant ids,
  like customer ids, are 1-128 characters of letters, digits, underscore and hyphen.

## API conventions

- JSON is snake_case; types are `BOOL`, `INTEGER`, `STRING`, `DATETIME`. Unknown request properties are rejected (400).
- `PATCH /customers/{id}`: omitted = keep, `null` = clear, value = set, for every attribute.
- Statuses: 200 for POST, GET and PATCH (as the original task specified), 204 for DELETE (our choice, the task is
  silent), 400 validation, 404 missing customer, 409 duplicate or concurrent modification.
- One error body shape: `{"error": "<CODE>", "message": "<text>"}`. Domain exceptions are mapped to HTTP only in
  `ApiExceptionHandler`. Messages never echo submitted values; they may name the customer id, field or property
  concerned (`Unknown property: x`, `Field score expects INTEGER but received a string`).

## Code conventions

- Readability over brevity. No exotic libraries, no long method chains, no hidden assumptions: name intermediate
  values. Small classes with one clear responsibility. No speculative abstractions.
- Format and import hygiene are enforced by Spotless (google-java-format AOSP, 100 columns; unused imports removed,
  wildcard imports forbidden). Never hand-format: run `./mvnw spotless:apply`.
- Records for immutable values, sealed interfaces for closed sets. Every branch on a sealed type or enum is an
  exhaustive `switch` without `default`, so the compiler lists what a new type must handle.
- Null policy: null is a programming error (`Objects.requireNonNull`); non-null but invalid content is a
  `DomainValidationException` (400). Packages are `@NullMarked`; `@Nullable` only where nullability is intended.
- Comments say why, not what.
- A new dependency needs approval in the plan.

## Testing conventions

- `*Test` = fast unit test (Surefire, no Docker). `*IT` = needs PostgreSQL (Failsafe, runs in `verify`).
- Domain: plain JUnit 5 and AssertJ, no Spring, no mocks of domain types.
- No in-memory fakes and no separate use-case tests: behaviour is tested through the API (`e2e/`) against real
  PostgreSQL (Testcontainers); repositories have their own `persistence/*IT`.
- API tests start the real server on a random port, use `ApiClient` with plain maps (never production DTOs), compare
  whole responses, and assert exact error code and message. Invalid-input cases are parameterized.
- Test names describe behaviour (`clearsADatetimeField`). Shared helpers live in `e2e/helper/`.
- Put each test at the lowest level that can observe the behaviour; do not specify one behaviour at two levels.
- Every bug fix starts with a failing test. Concurrency behaviour needs a test that actually races requests.

### What a plan's testing strategy must cover

New and changed behaviour per level (domain, persistence, API); error and edge cases (validation, missing,
duplicate, tenant isolation, concurrency); migrations and existing data; which existing tests change and why; how the
result is verified at the end (`spotless:apply`, `verify`, a manual `.http` check if the API changed).

## Adding a field type

1. Add the constant to `FieldType`.
2. Add a record to the sealed `FieldValue` (the compiler flags every `switch` that needs a branch).
3. Add the branch in `GenericFieldDefinition.parse` and implement `rawValue()`.
4. If the JSON shape is new, update both `toRawValue` mappings (`rest/GenericFieldChanges`,
   `persistence/GenericFieldsJson`).
5. Add a Flyway migration widening the `type` check constraint.
6. Tests: domain value test, `GenericFieldDefinitionTest`, `CustomerTest`, JSONB round trip, API test.
7. Update the README list of types.

## README

Reviewer-facing: build and run, API examples, assumptions and deliberate deviations from the original task
(header-only tenant, strict types, PATCH update, rejected unknown properties, hard delete, no caching) and honest
future steps. Update it in the same change as any behaviour it describes.
