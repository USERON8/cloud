# Dev Startup

Updated: 2026-04-22

## Primary Entrypoints

Recommended:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

PowerShell:

```powershell
powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

Related scripts:

- `start-containers.*`: start only infrastructure
- `start-services.*`: start only Java services
- `start-host-linked.*`: validate host-started services first
- `start-cluster-linked.*`: attach validated host services back into the container cluster
- `stop-*.sh`, `restart-*.sh`: stop or restart local environment pieces

## What `start-platform.*` Does

- starts containers through `start-containers.*`
- waits for infrastructure readiness
- exports local runtime addresses and development secrets
- prepares SkyWalking agent when enabled
- starts Java services through `start-services.*`

Logs:

- process stdout/stderr: `.tmp/service-runtime/<service>/`
- rolling service logs: `services/<service>/logs/`
- fallback app logs when module directory is not writable: `.tmp/service-runtime/<service>/app-logs/`

## Common Flags

- `--with-monitoring`: include Prometheus, Grafana, exporters, and SkyWalking stack
- `--skip-containers`: reuse existing containers and only start services
- `--skip-services`: only start containers
- `--services=order-service,stock-service`: only start or restart selected services
- `--no-kill-ports`: do not clean occupied ports before startup
- `--open-dashboards`: open local console URLs after startup
- `--dry-run`: validate flow without actually starting anything

## SkyWalking Notes

- `start-services.*` can resolve the agent automatically from `.tmp/skywalking/`
- first run may download the agent if cache is empty
- use `SKYWALKING_AUTO_ENABLE=false` to disable auto-attach
- use `SKYWALKING_AGENT_PATH` to provide a fixed agent jar

## Host-Linked Workflow

Use this when some Java services run on the host but the rest of the platform stays in Docker.

1. Run `start-host-linked.* --services=...`
2. The script checks `.tmp/acceptance/startup.csv`
3. Only `UP` or `UP_SECURED` counts as accepted
4. Run `start-cluster-linked.* --services=...` to reconnect through Nginx and the cluster network

`start-cluster-linked.*` switches `NGINX_GATEWAY_UPSTREAM` to `gateway:8080`.

## Frontend

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp dev:h5
```

Build output for Nginx:

```bash
pnpm --dir my-shop-uniapp build:h5
```

`docker/docker-compose.yml` mounts `my-shop-uniapp/dist/h5` into Nginx.
