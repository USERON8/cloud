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
}

$killPorts = -not $NoKillPorts
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")

$baseImages = @(
    "mysql:9.3.0",
    "redis:7.4.5-bookworm",
    "nacos/nacos-server:v3.0.2",
    "apache/rocketmq:5.3.2",
    "apacherocketmq/rocketmq-dashboard:2.1.0",
    "nginx:stable-perl",
    "minio/minio:RELEASE.2025-07-23T15-54-02Z-cpuv1",
    "elasticsearch:9.1.2",
    "kibana:9.1.2",
    "logstash:9.1.2"
)

$monitoringImages = @(
    "bitnami/prometheus:3.5.0-debian-12-r3",
    "grafana/grafana:12.2.0-17084981832"
)

$envPath = Join-Path $root "docker\.env"
if (-not (Test-Path $envPath)) {
    throw "docker/.env not found"
}

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

$imagesToCheck = @($baseImages)
if ($WithMonitoring) {
    $imagesToCheck += $monitoringImages
}

$missingImages = New-Object System.Collections.Generic.List[string]
foreach ($image in ($imagesToCheck | Select-Object -Unique)) {
    docker image inspect $image *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Host ("IMAGE_CHECK image={0} exists=true action=use-local" -f $image)
    } else {
        Write-Host ("IMAGE_CHECK image={0} exists=false action=abort" -f $image)
        $missingImages.Add($image)
    }
}

if ($missingImages.Count -gt 0) {
    throw ("Local image check failed. Missing images: {0}" -f ($missingImages -join ", "))
}

if ($DryRun) {
    Write-Host "DRY_RUN_DONE script=start-containers"
    exit 0
}

Push-Location (Join-Path $root "docker")
try {
    docker compose -f docker-compose.yml up -d --pull never
    if ($WithMonitoring) {
        docker compose -f monitoring-compose.yml up -d --pull never prometheus grafana
    }
} finally {
    Pop-Location
}

Write-Host ("CONTAINERS_START withMonitoring={0} status=started" -f $WithMonitoring)
