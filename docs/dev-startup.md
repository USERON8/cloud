# Dev Startup

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
- runtime env export for `NACOS_SERVER_ADDR`, `ROCKETMQ_NAME_SERVER`, and `SEATA_SERVER_ADDR`
- optional SkyWalking agent wiring
- `start-services.*`

`start-containers.*` and `start-services.*` are still available when you want finer control.

Service process logs are written under `.tmp/service-runtime/<service>/stdout.log` and `.tmp/service-runtime/<service>/stderr.log`.
Rolling application and error logs are written to `services/<service>/logs/` by default, or `.tmp/service-runtime/<service>/app-logs/` if the module log directory is not writable.

## Common flags

- `--with-monitoring`: start Prometheus, Grafana, and exporters together with the base containers
- `--no-kill-ports`: do not terminate existing listeners before startup
- `--skip-containers`: only run the service startup phase
- `--skip-services`: only run the container startup phase
- `--services=order-service,stock-service`: only restart the named services and leave the others running
- `--open-dashboards`: open local console URLs after startup
- `--enable-skywalking`: require a valid SkyWalking agent path and wire the collector backend
- `--skywalking-agent-path=/path/to/skywalking-agent.jar`: set the agent path explicitly
- `--skywalking-backend=127.0.0.1:11800`: override the SkyWalking OAP backend address
- `--dry-run`: validate argument flow without starting containers or services

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

Enable SkyWalking explicitly:

```bash
bash scripts/dev/start-platform.sh \
  --enable-skywalking \
  --skywalking-agent-path=/path/to/skywalking-agent.jar \
  --skywalking-backend=127.0.0.1:11800
```

```powershell
powershell -File scripts/dev/start-platform.ps1 `
  --enable-skywalking `
  --skywalking-agent-path=D:\tools\skywalking-agent\skywalking-agent.jar `
  --skywalking-backend=127.0.0.1:11800
```
