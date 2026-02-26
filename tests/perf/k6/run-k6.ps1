param(
  [ValidateSet("acceptance", "smoke", "search-chain", "search-max", "route-only", "order-only")]
  [string]$Scenario = "acceptance",
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [string]$Profile = "loadtest"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../../..")
Set-Location $root
. (Join-Path $PSScriptRoot "lib/preflight.ps1")

$mode = "all"
$scriptPath = "/scripts/acceptance-cases.js"
$displayName = "acceptance"

switch ($Scenario) {
  "acceptance" {
    $mode = "all"
    $scriptPath = "/scripts/acceptance-cases.js"
    $displayName = "acceptance"
  }
  "smoke" {
    $mode = "all"
    $scriptPath = "/scripts/all-services-smoke.js"
    $displayName = "all-services smoke"
  }
  "search-chain" {
    $mode = "search"
    $scriptPath = "/scripts/search-chain.js"
    $displayName = "search-chain"
  }
  "search-max" {
    $mode = "search"
    $scriptPath = "/scripts/search-singleton-max.js"
    $displayName = "search singleton max"
  }
  "route-only" {
    $mode = "all"
    $scriptPath = "/scripts/gateway-route-only.js"
    $displayName = "gateway route-only"
  }
  "order-only" {
    $mode = "all"
    $scriptPath = "/scripts/order-create-only.js"
    $displayName = "order-create only"
  }
}

Assert-K6Preflight -Mode $mode

Write-Host ("[k6] scenario={0}" -f $Scenario)
Write-Host ("[k6] starting {0} run..." -f $displayName)
Write-Host ("[k6] BASE_URL={0}" -f $BaseUrl)

$env:K6_BASE_URL = $BaseUrl
docker compose -f docker/monitoring-compose.yml --profile $Profile run --rm k6 run -o experimental-prometheus-rw $scriptPath
