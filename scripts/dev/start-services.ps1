param(
    [switch]$NoKillPorts,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

foreach ($arg in $args) {
    if ($arg -eq "--dry-run") { $DryRun = $true }
    if ($arg -eq "--kill-ports") { $NoKillPorts = $false }
}

$killPorts = -not $NoKillPorts
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { return }
        $parts = $line.Split("=", 2)
        if ($parts.Length -ne 2) { return }
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (-not [string]::IsNullOrWhiteSpace($key) -and -not (Test-Path "Env:$key")) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

Import-DotEnv -Path (Join-Path $root "docker\.env")
if (-not $env:NACOS_SERVER_ADDR) {
    $nacosHost = if ($env:NACOS_HOST) { $env:NACOS_HOST } else { "127.0.0.1" }
    $nacosPort = if ($env:NACOS_PORT) { $env:NACOS_PORT } else { "18848" }
    $env:NACOS_SERVER_ADDR = "$nacosHost`:$nacosPort"
}
if (-not $env:ROCKETMQ_NAME_SERVER) {
    $mqHost = if ($env:ROCKETMQ_NAMESRV_HOST) { $env:ROCKETMQ_NAMESRV_HOST } else { "127.0.0.1" }
    $mqPort = if ($env:ROCKETMQ_NAMESRV_PORT) { $env:ROCKETMQ_NAMESRV_PORT } else { "19876" }
    $env:ROCKETMQ_NAME_SERVER = "$mqHost`:$mqPort"
}
if (-not $env:AUTH_HOST) { $env:AUTH_HOST = "127.0.0.1" }
if (-not $env:AUTH_PORT) { $env:AUTH_PORT = "8081" }
if (-not $env:AUTH_ISSUER_URI) { $env:AUTH_ISSUER_URI = "http://$($env:AUTH_HOST):$($env:AUTH_PORT)" }
if (-not $env:AUTH_JWK_SET_URI) { $env:AUTH_JWK_SET_URI = "http://$($env:AUTH_HOST):$($env:AUTH_PORT)/.well-known/jwks.json" }
if (-not $env:AUTH_TOKEN_URI) { $env:AUTH_TOKEN_URI = "http://$($env:AUTH_HOST):$($env:AUTH_PORT)/oauth2/token" }
if (-not $env:DB_HOST) { $env:DB_HOST = "127.0.0.1" }
if (-not $env:DB_PORT) { $env:DB_PORT = if ($env:PORT_MYSQL) { $env:PORT_MYSQL } else { "13306" } }
if (-not $env:REDIS_HOST) { $env:REDIS_HOST = "127.0.0.1" }
if (-not $env:REDIS_PORT) { $env:REDIS_PORT = if ($env:PORT_REDIS) { $env:PORT_REDIS } else { "16379" } }
if (-not $env:ELASTICSEARCH_URIS) {
    $esPort = if ($env:PORT_ES_HTTP) { $env:PORT_ES_HTTP } else { "19200" }
    $env:ELASTICSEARCH_URIS = "http://127.0.0.1:$esPort"
}
if (-not $env:MINIO_ENDPOINT) {
    $minioPort = if ($env:PORT_MINIO_API) { $env:PORT_MINIO_API } else { "19000" }
    $env:MINIO_ENDPOINT = "http://127.0.0.1:$minioPort"
}
if (-not $env:MINIO_PUBLIC_ENDPOINT) { $env:MINIO_PUBLIC_ENDPOINT = $env:MINIO_ENDPOINT }

$servicePorts = 8080..8087
if ($killPorts) {
    foreach ($port in $servicePorts) {
        Kill-PortOwner -Port $port -DryRun:$DryRun
    }
}
if ($DryRun) {
    Write-Host "DRY_RUN_DONE script=start-services"
    exit 0
}

$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    "java"
}

$logDir = Join-Path $root ".tmp\acceptance\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$services = @(
    @{ name = "gateway";         port = 8080; jar = "gateway\target\gateway-0.0.1-SNAPSHOT.jar"; profiles = "dev,route" },
    @{ name = "auth-service";    port = 8081; jar = "auth-service\target\auth-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "user-service";    port = 8082; jar = "user-service\target\user-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "order-service";   port = 8083; jar = "order-service\target\order-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "product-service"; port = 8084; jar = "product-service\target\product-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "stock-service";   port = 8085; jar = "stock-service\target\stock-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "payment-service"; port = 8086; jar = "payment-service\target\payment-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "search-service";  port = 8087; jar = "search-service\target\search-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" }
)

$results = @()
$allOk = $true

function Get-HealthStatus($resp) {
    if ($null -eq $resp) { return $null }
    if ($resp.PSObject.Properties.Name -contains "status") { return $resp.status }
    if ($resp.PSObject.Properties.Name -contains "data" -and $null -ne $resp.data) {
        if ($resp.data.PSObject.Properties.Name -contains "status") { return $resp.data.status }
    }
    return $null
}

foreach ($svc in $services) {
    $name = $svc.name
    $port = [int]$svc.port
    $jarPath = Join-Path $root $svc.jar
    if (-not (Test-Path $jarPath)) {
        throw "jar missing: $jarPath"
    }

    $outLog = Join-Path $logDir "$name.out.log"
    $errLog = Join-Path $logDir "$name.err.log"
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    if (Test-Path $errLog) { Remove-Item $errLog -Force }

    $argsLine = "-jar `"$jarPath`" --spring.profiles.active=$($svc.profiles)"
    $start = Get-Date
    $proc = Start-Process -FilePath $java -ArgumentList $argsLine -WorkingDirectory $root -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru

    $deadline = $start.AddSeconds(180)
    $status = "TIMEOUT"
    $healthy = $false
    while ((Get-Date) -lt $deadline) {
        if ($proc.HasExited) {
            $status = "EXITED:$($proc.ExitCode)"
            break
        }
        try {
            $resp = Invoke-RestMethod -Uri "http://127.0.0.1:$port/actuator/health" -Method GET -TimeoutSec 5
            if ((Get-HealthStatus $resp) -eq "UP") {
                $status = "UP"
                $healthy = $true
                break
            }
        } catch {}
        Start-Sleep -Seconds 2
    }

    $duration = [int][Math]::Round((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds, 0)
    $results += [pscustomobject]@{
        service         = $name
        port            = $port
        pid             = $proc.Id
        status          = $status
        startup_seconds = $duration
        out_log         = $outLog
        err_log         = $errLog
    }

    Write-Host ("SERVICE_START service={0} port={1} pid={2} health={3}" -f $name, $port, $proc.Id, $status)

    if (-not $healthy) {
        $allOk = $false
        break
    }
}

$csvPath = Join-Path $root ".tmp\acceptance\startup.csv"
$results | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

$pidsPath = Join-Path $root ".tmp\acceptance\pids.txt"
$results | ForEach-Object { "{0},{1},{2},{3}" -f $_.service, $_.port, $_.pid, $_.status } | Set-Content -Path $pidsPath -Encoding UTF8

if ($allOk) { Write-Host "STARTUP_OK" } else { Write-Host "STARTUP_FAILED" }
$results | Format-Table -AutoSize
