# Products App

Create products and see the list of saved products. A React UI talks to a Spring Boot REST API,
which stores products in PostgreSQL.

| Part | Stack | Folder |
|---|---|---|
| UI | React + TypeScript, Vite | `client/` |
| API | Spring Boot 4 on Java 25, Gradle | `api/` |
| Database | PostgreSQL 18 (Docker Compose), Flyway migrations | `docker-compose.yml`, `api/src/main/resources/db/migration/` |

## Prerequisites

- **JDK 25.** Gradle picks it up through its toolchain support, even if another JDK is your default.
- **Node 26.** `nvm use` reads `.nvmrc`.
- **Docker** with Compose v2+ (Docker Desktop on macOS and Windows). It runs Postgres locally and
  in the integration tests.

## Run locally

Use three terminals, all from the repository root:

```sh
# 1. Database
docker compose up -d

# 2. API on http://localhost:8080 (Flyway creates the schema on startup)
cd api && ./gradlew bootRun

# 3. UI on http://localhost:3000 (proxies /api to the API)
cd client && nvm use && npm ci && npm run dev
```

Open http://localhost:3000.

To stop, press Ctrl+C in the API and UI terminals, then run `docker compose down`. Your data
stays in the `db-data` volume. `docker compose down -v` deletes it.

## Tests

```sh
cd api && ./gradlew test     # API tests; needs Docker (Testcontainers starts a throwaway Postgres)

cd client && npm run lint && npm run build   # lint and type check
```

## Configuration

Every value has a local default, so nothing needs setting to run locally.

| Setting | Default | Override with |
|---|---|---|
| Postgres database / user / password | `productdatabase` / `admin` / `pass` (local only) | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Postgres port on the host | `20028` (bound to `127.0.0.1`) | `POSTGRES_PORT` |
| API datasource | `jdbc:postgresql://localhost:20028/productdatabase` | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| API port | `8080` | `SERVER_PORT` |
| UI dev port | `3000` | fixed in `client/vite.config.ts` |

Compose also reads these from a `.env` file in the repository root. That file is gitignored.

If port 20028 is already taken, move both the database and the API to another port:

```sh
POSTGRES_PORT=20029 docker compose up -d
cd api && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:20029/productdatabase ./gradlew bootRun
```

## API

| Endpoint | Description |
|---|---|
| `POST /api/products` | Create a product from `{"name": "..."}` |
| `GET /api/products` | List all products |

Errors use `application/problem+json` (RFC 9457). The full contract is in [docs/PLAN.md](docs/PLAN.md).
