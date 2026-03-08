# Dev Startup

This project now has a unified startup entrypoint for local development.

## Recommended entrypoints

PowerShell:

```powershell
powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

Bash:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

Compatibility aliases:

```powershell
powershell -File scripts/dev/start-all.ps1 --with-monitoring
```

```bash
bash scripts/dev/start-all.sh --with-monitoring
```

## Behavior

`start-platform.*` orchestrates:
- `start-containers.*`
- infrastructure readiness checks using host ports from `docker/.env`
- runtime env export for `NACOS_SERVER_ADDR`, `ROCKETMQ_NAME_SERVER`, and `SEATA_SERVER_ADDR`
- optional SkyWalking agent wiring
- `start-services.*`

`start-containers.*` and `start-services.*` are still available when you want finer control.

## Common flags

- `--with-monitoring`: start Prometheus, Grafana, and exporters together with the base containers
- `--no-kill-ports`: do not terminate existing listeners before startup
- `--skip-containers`: only run the service startup phase
- `--skip-services`: only run the container startup phase
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

Start containers only:

```bash
powershell -File scripts/dev/start-platform.ps1 --skip-services --with-monitoring
```

Start platform and open dashboards:

```powershell
powershell -File scripts/dev/start-platform.ps1 --with-monitoring --open-dashboards
```

Enable SkyWalking explicitly:

```powershell
powershell -File scripts/dev/start-platform.ps1 `
  --enable-skywalking `
  --skywalking-agent-path=D:\tools\skywalking-agent\skywalking-agent.jar `
  --skywalking-backend=127.0.0.1:11800
```
