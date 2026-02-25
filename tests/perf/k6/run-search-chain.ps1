param(
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [string]$Profile = "loadtest"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../../..")
Set-Location $root
. (Join-Path $PSScriptRoot "lib/preflight.ps1")
Assert-K6Preflight -Mode "search"

Write-Host "[k6] starting search-chain load run..."
Write-Host "[k6] BASE_URL=$BaseUrl"

$env:K6_BASE_URL = $BaseUrl

docker compose -f docker/monitoring-compose.yml --profile $Profile run --rm k6 run -o experimental-prometheus-rw /scripts/search-chain.js
