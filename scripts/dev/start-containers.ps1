param(
    [switch]$WithMonitoring,
    [switch]$NoKillPorts,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

foreach ($arg in $args) {
    if ($arg -eq "--with-monitoring") { $WithMonitoring = $true }
    if ($arg -eq "--dry-run") { $DryRun = $true }
    if ($arg -eq "--kill-ports") { $NoKillPorts = $false }
    if ($arg -eq "--no-kill-ports") { $NoKillPorts = $true }
}

$killPorts = -not $NoKillPorts
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")
. (Join-Path $PSScriptRoot "lib\runtime.ps1")

function Assert-DockerDaemonReady {
    docker version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker daemon is not ready. Start Docker Desktop first."
    }
}

$envPath = Join-Path $root "docker\.env"
if (-not (Test-Path $envPath)) {
    throw "docker/.env not found"
}

Sync-EnvironmentFiles -Root $root -ImportProcessEnvironment | Out-Null

$portVars = Get-Content $envPath |
        Where-Object { $_ -match "^PORT_[A-Z0-9_]+=([0-9]+)$" } |
        ForEach-Object { [int](($_ -split "=", 2)[1]) }

if ($killPorts) {
    $dockerOwnerFound = $false
    foreach ($port in $portVars) {
        if (Is-DockerOwner -Port $port) {
            $dockerOwnerFound = $true
            break
        }
    }

    if ($dockerOwnerFound -and -not $DryRun) {
        Push-Location (Join-Path $root "docker")
        try {
            docker compose -f docker-compose.yml down | Out-Null
            docker compose -f monitoring-compose.yml down | Out-Null
        } catch {}
        Pop-Location
    }

    foreach ($port in $portVars) {
        Kill-PortOwner -Port $port -DryRun:$DryRun
    }
}

if ($DryRun) {
    Write-Host ("DRY_RUN_DONE script=start-containers withMonitoring={0}" -f $WithMonitoring)
    exit 0
}

Assert-DockerDaemonReady

Push-Location (Join-Path $root "docker")
try {
    docker compose -f docker-compose.yml up -d --pull missing --build
    if ($WithMonitoring) {
        docker compose -f monitoring-compose.yml up -d --pull missing
    }
} finally {
    Pop-Location
}

Write-Host ("CONTAINERS_START withMonitoring={0} status=started" -f $WithMonitoring)
