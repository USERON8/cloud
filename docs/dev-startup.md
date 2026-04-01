# Dev Startup
Version: 1.1.0

This project now has a unified startup entrypoint for local development.

## Recommended entrypoints

WSL/Linux (recommended):

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

PowerShell compatibility:

```powershell
powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

Compatibility aliases:

```bash
bash scripts/dev/start-all.sh --with-monitoring
```

```powershell
powershell -File scripts/dev/start-all.ps1 --with-monitoring
```

## Behavior

`start-platform.*` orchestrates:
- `start-containers.*`
- infrastructure readiness checks using host ports from `docker/.env`
- runtime env export for `NACOS_SERVER_ADDR` and `ROCKETMQ_NAME_SERVER`
- automatic SkyWalking agent wiring with local cache/download
- scheduled outbox relay inside `order-service`, `payment-service`, and `stock-service`
- `start-services.*`

`start-containers.*` and `start-services.*` are still available when you want finer control.
When `start-services.*` is run directly, it now auto-exports local runtime addresses and development secrets from `scripts/dev/lib/runtime.*`, so gateway/auth no longer depend on a prior `start-platform.*` run just to get required keys. It also auto-resolves the SkyWalking javaagent from `.tmp/skywalking/`, downloading it on first use when `SKYWALKING_AUTO_ENABLE` is not disabled.

Service process logs are written under `.tmp/service-runtime/<service>/stdout.log` and `.tmp/service-runtime/<service>/stderr.log`.
Rolling application and error logs are written to `services/<service>/logs/` by default, or `.tmp/service-runtime/<service>/app-logs/` if the module log directory is not writable.
SkyWalking agent logs are written under `.tmp/service-runtime/<service>/skywalking-agent/`.

## Common flags

- `--with-monitoring`: start Prometheus, Grafana, and exporters together with the base containers
- `--no-kill-ports`: do not terminate existing listeners before startup
- `--skip-containers`: only run the service startup phase
- `--skip-services`: only run the container startup phase
- `--services=order-service,stock-service`: only restart the named services and leave the others running
- `--open-dashboards`: open local console URLs after startup
- `--enable-skywalking`: fail fast if the SkyWalking agent cannot be resolved or downloaded
- `--skywalking-agent-path=/path/to/skywalking-agent.jar`: set the agent path explicitly instead of the cached copy
- `--skywalking-backend=127.0.0.1:11800`: override the SkyWalking OAP backend address
- `--dry-run`: validate argument flow without starting containers or services

Environment overrides:
- `SKYWALKING_AUTO_ENABLE=false`: disable automatic agent lookup/download
- `SKYWALKING_AGENT_PATH=/path/to/skywalking-agent.jar`: use a custom agent jar
- `SKYWALKING_AGENT_DOWNLOAD_URL=...`: override the default download source
- `SKYWALKING_AGENT_DOWNLOAD_TIMEOUT_SECONDS=180`: override the download timeout
- `SKYWALKING_COLLECTOR_BACKEND_SERVICE=127.0.0.1:11800`: override the OAP gRPC endpoint

## Frontend (UniApp H5)

Build the H5 frontend:

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp build:h5
```

`docker/docker-compose.yml` mounts `my-shop-uniapp/dist` to Nginx `/usr/share/nginx/html`.

## Examples

Start everything with monitoring:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

Start services only, reusing existing containers:

```bash
bash scripts/dev/start-platform.sh --skip-containers
```

Restart only the services you changed:

```bash
bash scripts/dev/start-platform.sh --skip-containers --services=order-service,stock-service
```

```powershell
powershell -File scripts/dev/start-platform.ps1 --skip-containers --services=order-service
```

Start containers only:

```bash
bash scripts/dev/start-platform.sh --skip-services --with-monitoring
```

Start platform and open dashboards:

```bash
bash scripts/dev/start-platform.sh --with-monitoring --open-dashboards
```

Enable SkyWalking explicitly and fail if the agent cannot be prepared:

```bash
bash scripts/dev/start-platform.sh \
  --enable-skywalking
```

```powershell
powershell -File scripts/dev/start-platform.ps1 `
  --enable-skywalking
```

Use a custom agent path when needed:

```bash
bash scripts/dev/start-platform.sh \
  --enable-skywalking \
  --skywalking-agent-path=/path/to/skywalking-agent.jar \
  --skywalking-backend=127.0.0.1:11800
```
