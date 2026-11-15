# Manual HTTP requests

VS Code has no built-in HTTP client; install the **REST Client** extension (`humao.rest-client`).
Open a `.http` file and click "Send Request" above a request (or `Ctrl+Alt+R`).

1. Start the database: `docker compose up -d`
2. Start the app: `./mvnw spring-boot:run -pl :infrastructure` (listens on `localhost:8080`)
3. Run the files:
   - `01-task-scenario.http`: the Flowers Incorporated example from `brief/TASK.md`, in order.
   - `02-rules-and-errors.http`: uniqueness, validation, tenant isolation and PATCH semantics.

The tenant is sent in the `X-Tenant-Id` header, not in the body (decision 11). Registering a field
or creating a customer a second time returns 409; reset with `docker compose down -v`.
