# Customers with Generic Fields

A multi-tenant customer store. Each tenant registers its own typed "generic fields" (boolean, integer, string, datetime) and attaches values to its customers.

Author: Jarosław Kulik

## Requirements

- JDK 25 or newer (the build targets Java 25).
- Docker, running. The tests start a real PostgreSQL 17 through Testcontainers, and the application needs a database.
- No Maven installation is needed; the Maven wrapper is included.

## Build and test

```
./mvnw verify
```

This builds the three modules and runs all tests. Unit tests (`*Test`, Surefire, `./mvnw test`) need no Docker. Integration and end-to-end tests (`*IT`, Failsafe, `verify`) use Testcontainers (PostgreSQL), so the Docker daemon must be running.

| | Linux / macOS | Windows (PowerShell) |
|---|---|---|
| Build and test | `./mvnw verify` | `.\mvnw.cmd verify` |
| Start database | `docker compose up -d` | `docker compose up -d` |
| Run | `java -jar modules/infrastructure/target/infrastructure-1.0.0-SNAPSHOT.jar` | `java -jar modules\infrastructure\target\infrastructure-1.0.0-SNAPSHOT.jar` |

Every other `./mvnw` command below has the same form on Windows: `.\mvnw.cmd ...`.

A single module needs its upstream modules built too: `./mvnw verify -pl :infrastructure -am`.

Code style is enforced by [Spotless](https://github.com/diffplug/spotless) (google-java-format, AOSP style, 100 columns; unused imports are removed and wildcard imports are forbidden). The check runs first in every build and fails on any deviation; `./mvnw spotless:apply` fixes formatting automatically.

## Run

```
docker compose up -d                                   # PostgreSQL 17 on localhost:5432
./mvnw package -DskipTests
java -jar modules/infrastructure/target/infrastructure-1.0.0-SNAPSHOT.jar
```

The application listens on `http://localhost:8080` and creates its schema with Flyway on startup. The database location comes from `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` (defaults match `docker-compose.yml`).

## API

Every request needs the header `X-Tenant-Id`. Bodies and responses are JSON with snake_case names. `DELETE` returns 204; all other successful calls, including the `POST`s, return 200.

```
# register generic fields (types: BOOL, INTEGER, STRING, DATETIME)
curl -X POST localhost:8080/generic_fields -H 'X-Tenant-Id: tenant_123' -H 'Content-Type: application/json' \
  -d '{"name":"dob","type":"DATETIME"}'

# create a customer
curl -X POST localhost:8080/customers -H 'X-Tenant-Id: tenant_123' -H 'Content-Type: application/json' \
  -d '{"id":"cus_123","first_name":"John","last_name":"Smith",
       "generic_fields":{"dob":"1990-01-01T00:00:00","vip":true,"score":100}}'

# read
curl localhost:8080/customers/cus_123 -H 'X-Tenant-Id: tenant_123'

# partial update: omitted = keep, null = remove, value = set
curl -X PATCH localhost:8080/customers/cus_123 -H 'X-Tenant-Id: tenant_123' -H 'Content-Type: application/json' \
  -d '{"first_name":"Lewis","last_name":"Bull","generic_fields":{"dob":null,"vip":false}}'

# delete
curl -X DELETE localhost:8080/customers/cus_123 -H 'X-Tenant-Id: tenant_123'
```

| Endpoint | Errors |
|---|---|
| `POST /generic_fields` | 400 invalid body, 409 name already registered for the tenant |
| `POST /customers` | 400 invalid body, unknown field, wrong value type; 409 id already exists |
| `GET /customers/{id}` | 404 |
| `PATCH /customers/{id}` | 400, 404, 409 concurrent modification |
| `DELETE /customers/{id}` | 404 |

Every error has the same body: `{"error": "<CODE>", "message": "<text>"}`.

## Assumptions and deliberate decisions

**Deviations from the task text**

- **The tenant comes from the `X-Tenant-Id` header on every endpoint, and `tenant_id` is absent from request and response bodies.** One source for the tenant on all endpoints avoids a header that can disagree with the body. A `tenant_id` in a body is rejected as an unknown property. The header is only a stand-in for authentication, which the task excludes.
- **Update is `PATCH`, not a full replace**, with one rule for every attribute: omitted keeps the stored value, `null` removes it, a value replaces it. This is what the task's update example describes.
- **Successful calls return 200, as in the task text; deletion returns 204.**
- **Unknown properties in request bodies are rejected (400)** instead of ignored, so a typo cannot silently lose data.
- **The task names the types in words, not as identifiers.** The API uses `BOOL`, `INTEGER`, `STRING` and `DATETIME`.

**Behaviour**

- The public customer id is unique per tenant and immutable. The database also has a private surrogate key that never appears in the API, errors or application code.
- Values are strictly typed and never converted (`"100"` is not an integer). Integers are 64-bit.
- Datetimes are ISO 8601, with or without an offset (`1990-01-01T00:00:00`, `1990-01-01T00:00:00Z`, `1990-01-01T02:00:00+02:00`); seconds are optional on input, fractions are allowed. The value is stored and returned as received (offset kept, seconds always written), so a fetched value equals the one sent. A zone name (`1990-01-01T00:00:00+01:00[Europe/Paris]`) is rejected, because it could not be kept. Values are not normalized to UTC, so an offset and a no-offset value are not comparable.
- Tenant ids (the `X-Tenant-Id` header): 1-128 characters, letters, digits, underscore and hyphen, so near-identical values such as `tenant 1` and `tenant_1 ` cannot silently name different tenants.
- Field names: 1-64 characters, letters, digits and underscore, case-sensitive. Customer ids: 1-128 characters, letters, digits, underscore and hyphen, so an id is always usable as a URL path segment without encoding (`/`, `;`, `.` or `..` would make a customer unreachable). Names up to 255 and not blank (use `null` to clear one).
- Field definitions are register-only: they cannot be renamed, retyped or deleted. That makes every stored value valid forever and removes any race between reading a definition and saving a customer.
- Clearing or setting a field that is not registered is a 400, also when the value is `null`. On create there is nothing to clear, so a `null` generic field is a 400 (omit it instead); `null` names on create simply mean "no name".
- Deleting is permanent (hard delete); deleting a missing customer is a 404.
- Not limited: length of string values, generic fields per customer, definitions per tenant. Strings must not contain U+0000 (PostgreSQL cannot store it): 400.
- The version used for optimistic locking is internal and not returned. Two concurrent PATCHes that touch different attributes both succeed (merged); if they touch the same attribute, the last one wins. A PATCH is attempted at most 3 times against newer state, then answered with 409.
- Unknown routes, wrong methods and wrong content types use the same error body.
- A stored customer that no longer passes validation (corrupt data, or a rule tightened later) is answered with 500 and logged, not with 400: the client sent nothing wrong.
- No caching. Reading a customer loads the definitions of its fields in a second query. Caching them would be safe, because definitions never change once registered, but at this scale it is not needed.

### Tests

| Level | What |
|---|---|
| Domain unit | value validation, each type, datetime format, merge rules (keep/clear/set), the task scenario |
| Persistence integration | JSONB round trip of all types, duplicate keys, optimistic locking, tenant isolation (PostgreSQL in Docker) |
| Request parsing | PATCH tri-state, unknown properties, JSON shapes |
| API (`e2e/*ApiIT`) | every endpoint through real HTTP to the embedded server (requests and responses as plain maps): the task's example, error cases, tenant isolation, concurrent create / patch races. Assertions also read the rows in PostgreSQL (Docker). There are no separate use-case tests; the API tests cover them. |

## Future steps

Deliberately left out of this solution, roughly in the order I would add them:

- **Size limits**: a request body cap, a maximum number of generic fields per customer, a maximum string length and a maximum number of definitions per tenant. Today one tenant can grow rows without bound, and every PATCH rewrites the whole JSONB value.
- **One query per read**: load the definitions together with the customer (or cache them per tenant) instead of in a second round trip.
- **Atomic PATCH under contention**: every rule is checked per value, so a PATCH could be a single `UPDATE` that merges the JSONB in SQL, with no read, retry or 409. Worth it only if many clients write the same customer at once.
- **Conditional requests**: return the version as an `ETag` and accept `If-Match` on PATCH, so a client's own read-modify-write cannot overwrite a change it has not seen.
- **Tenant isolation in depth**: PostgreSQL row-level security, so a query that forgets `tenant_id` returns nothing instead of another tenant's data. Real authentication replaces the trusted header.
- **Operations**: health and readiness endpoints, metrics for conflicts and retries, statement timeouts, and no default database credentials outside local development.
- **API**: `GET /generic_fields` so clients can discover the types, listing and searching customers (JSONB indexes, and a decision on comparing datetimes with and without an offset), accepting `application/merge-patch+json` for PATCH, and a note that integers above 2^53 lose precision in JavaScript clients.

## How this was built

This solution was developed with Claude Code. Design decisions were agreed in a plan-first workflow before any code was written, and the working agreements (architecture, API and testing conventions) are kept in `CLAUDE.md` so the same workflow can be continued in later sessions. All code was reviewed by me.
