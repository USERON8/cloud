param(
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [string]$Profile = "loadtest"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../../..")
Set-Location $root

Write-Host "[k6] starting search singleton max run..."
Write-Host "[k6] BASE_URL=$BaseUrl"

$env:K6_BASE_URL = $BaseUrl

docker compose -f docker/monitoring-compose.yml --profile $Profile run --rm k6 run -o experimental-prometheus-rw /scripts/search-singleton-max.js
