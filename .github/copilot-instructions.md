# Copilot instructions for the Cloud Shop repository

These instructions are written to help AI coding agents become productive quickly in this repository.
Keep suggestions short, specific, and repository-aware. Avoid generic advice; prefer direct edits with file/line references.

## Quick summary (1-2 lines)

- This is a Spring Boot 3 / Spring Cloud Alibaba microservices monorepo (Java 17) with a UniApp frontend. Services live under `services/` and share common libs under `common-*` modules. Local dev uses docker-compose and helper scripts in `scripts/dev/`.

## Where to start reading

- `README.md` — high-level architecture, ports, and quick-start commands (see `Quick Start`).
- `pom.xml` (root) — modules, shared dependency versions (MapStruct, MyBatis-Plus, RocketMQ, SkyWalking).
- `services/*/README.md` — per-service responsibilities (gateway, auth-service, user-service, etc.).
- `scripts/dev/` — orchestrates local containers and service startup; important: `start-platform.sh`, `start-services.*`, and PowerShell variants for Windows.
- `db/init/` and `db/test/` — DB bootstrap SQL scripts.

## Big-picture architecture notes

- Gateway (`services/gateway`) is the single public HTTP entry. It validates JWTs, signs internal identity headers, and normalizes routes. Route prefixes are documented in `services/gateway/README.md`.
- Services communicate via HTTP + RocketMQ. Messaging patterns: Outbox (per-service scheduled relay) -> RocketMQ -> idempotent consumers. Look for `outbox_event` usage and scheduled relays in `order-service`, `payment-service`, `stock-service`.
- Caching: mix of explicit Redis services and selective local L1 caches. Recent changes moved many caches to explicit Redis-only models; product detail keeps multi-level (Caffeine + Redis).
- Tracing & observability: SkyWalking javaagent is auto-wired via startup scripts and `.tmp/skywalking/`.
- Deployment: local dev uses `docker/docker-compose.yml` and `scripts/dev/start-*` flows. `start-host-linked` and `start-cluster-linked` are helper flows for staged acceptance and cluster-like runs.

## Developer workflows and exact commands

- Build backend (fast):
    - `mvn -T 1C clean package -DskipTests`
- Run unit tests locally:
    - `mvn -B -T 1C -DskipITs clean test`
- Start local infra (Linux/WSL preferred):
    - `bash scripts/dev/start-containers.sh` or `bash scripts/dev/start-platform.sh --with-monitoring`
    - PowerShell: `powershell -File scripts/dev/start-platform.ps1 --with-monitoring`
- Start only services (after containers):
    - `bash scripts/dev/start-services.sh` (or PowerShell equivalent)
- Start gateway locally for quick debugging:
    - `mvn -pl gateway spring-boot:run`
- Frontend build (UniApp H5):
    - `pnpm --dir my-shop-uniapp install` then `pnpm --dir my-shop-uniapp build:h5`

## CI / GitHub Actions notes

- Unit tests run in `.github/workflows/unit-test.yml`:
    - Java 17 (Temurin), `mvn -B -T 1C -DskipITs clean test` for backend.
    - Frontend build uses pnpm (node 22) and `pnpm run build:h5` in `my-shop-uniapp`.
- Docker compose is validated in CI using `docker compose -f docker-compose.yml config -q`.

## Project-specific conventions and patterns (explicit and discoverable)

- Outbox-relay pattern: look for `outbox_event` table, scheduled relay tasks inside `order-service`, `payment-service`, `stock-service`. When adding messaging, use the existing outbox + RocketMQ flow; consumers must be idempotent (Redis-based dedupe patterns exist).
- Remote call wrapper: `RemoteCallSupport` (shared pattern) standardizes timeouts, error translation and fallbacks — reuse it for downstream calls to keep semantics consistent.
- Cache behavior:
    - Prefer explicit Redis cache services and keys (project intentionally moved away from generic `@EnableCaching` for most modules).
    - Product detail remains multi-level (Caffeine + Redis) — inspect `ProductDetailCacheService` before changing cache layering.
    - Gateway-level fallback caches are separate from domain caches (`SearchFallbackCache`) — do not mix semantics.
- Secrets & dev keys: `start-services.*` exports development secrets (GATEWAY_SIGNATURE_SECRET, CLIENT_SERVICE_SECRET, etc.). For local runs prefer execution via scripts to avoid missing env exports.
- Logging and runtime files:
    - Service stdout/stderr: `.tmp/service-runtime/<service>/{stdout.log,stderr.log}`.
    - Rolling application logs: `services/<service>/logs/` or `.tmp/service-runtime/<service>/app-logs/`.

## Common change patterns to follow

- When changing RPC/HTTP endpoints, update gateway route mapping in `services/gateway/` to keep external behavior stable.
- When touching transactional flows across services, follow the outbox pattern: local DB transaction -> insert outbox row -> scheduled relay publishes to RocketMQ.
- When adding new caches, document TTLs and invalidation paths and prefer explicit Redis services rather than global `@EnableCaching` annotations.

## Files worth opening when debugging a failing startup or test

- `scripts/dev/start-platform.sh` and PowerShell variants — env wiring and SkyWalking agent download logic.
- `docker/docker-compose.yml` and `docker/.env` — container ports and linked services.
- `services/*/application-*.yml` (service configs) — runtime property overrides.
- `common-*` modules (for helpers like `common-web`, `common-messaging`, `common-db`)

## Example edits the agent can make safely (low-risk)

- Small docs updates in `services/*/README.md` to reflect endpoint changes.
- Add or fix logging messages and mapstruct mappings inside `common-*` modules used across services.
- Add unit tests for pure Java logic that don't require containers (e.g., utilities, mappers, validators).

## Risky areas — avoid changing without human review

- Global cache semantics (removing or enabling `@EnableCaching`) — can silently change runtime behavior.
- Gateway route mutations — may break external clients.
- Messaging and outbox transactional logic — must preserve idempotency and ordering assumptions.

## Where to look for tests and test helpers

- Root `pom.xml` configures surefire and test properties (`spring.profiles.active=test`). Common test utilities are in `common-*` modules under `src/test/java`.

## When you need more context

- If a runtime or infra behavior is unclear (e.g., exact env var values), run `scripts/dev/start-platform.sh --dry-run` or inspect `scripts/dev/lib/runtime.*` to see exported env wiring.

---

If anything above is unclear or you want extra examples (e.g., a short walkthrough to add a new downstream call using `RemoteCallSupport`), tell me which part to expand and I will iterate.
