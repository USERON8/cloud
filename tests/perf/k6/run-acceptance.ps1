param(
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [string]$Profile = "loadtest"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../../..")
Set-Location $root
. (Join-Path $PSScriptRoot "lib/preflight.ps1")
Assert-K6Preflight -Mode "all"

Write-Host "[k6] starting acceptance load run..."
Write-Host "[k6] BASE_URL=$BaseUrl"

$env:K6_BASE_URL = $BaseUrl

docker compose -f docker/monitoring-compose.yml --profile $Profile run --rm k6
