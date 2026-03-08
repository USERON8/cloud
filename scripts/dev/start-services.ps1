param(
    [switch]$NoKillPorts,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

foreach ($arg in $args) {
    if ($arg -eq "--dry-run") { $DryRun = $true }
    if ($arg -eq "--kill-ports") { $NoKillPorts = $false }
    if ($arg -eq "--no-kill-ports") { $NoKillPorts = $true }
}

$killPorts = -not $NoKillPorts
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")

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

$skywalkingAgentPath = $env:SKYWALKING_AGENT_PATH
$skywalkingEnabled = -not [string]::IsNullOrWhiteSpace($skywalkingAgentPath) -and (Test-Path $skywalkingAgentPath)
$skywalkingBackend = if ([string]::IsNullOrWhiteSpace($env:SKYWALKING_COLLECTOR_BACKEND_SERVICE)) {
    "127.0.0.1:11800"
} else {
    $env:SKYWALKING_COLLECTOR_BACKEND_SERVICE
}
$serviceJvmOpts = if ([string]::IsNullOrWhiteSpace($env:SERVICE_JVM_OPTS)) {
    "-XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8"
} else {
    $env:SERVICE_JVM_OPTS
}
$startupTimeoutSeconds = 300
if (-not [string]::IsNullOrWhiteSpace($env:SERVICE_STARTUP_TIMEOUT_SECONDS)) {
    $parsedTimeout = 0
    if ([int]::TryParse($env:SERVICE_STARTUP_TIMEOUT_SECONDS, [ref]$parsedTimeout) -and $parsedTimeout -gt 0) {
        $startupTimeoutSeconds = $parsedTimeout
    }
}

$services = @(
    @{ name = "gateway";         port = 8080; jar = "services\gateway\target\gateway-0.0.1-SNAPSHOT.jar"; profiles = "dev,route" },
    @{ name = "auth-service";    port = 8081; jar = "services\auth-service\target\auth-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "user-service";    port = 8082; jar = "services\user-service\target\user-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "order-service";   port = 8083; jar = "services\order-service\target\order-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "product-service"; port = 8084; jar = "services\product-service\target\product-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "stock-service";   port = 8085; jar = "services\stock-service\target\stock-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "payment-service"; port = 8086; jar = "services\payment-service\target\payment-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" },
    @{ name = "search-service";  port = 8087; jar = "services\search-service\target\search-service-0.0.1-SNAPSHOT.jar"; profiles = "dev" }
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

function Get-HttpStatusCode($exception) {
    if ($null -eq $exception) { return $null }
    if ($exception.PSObject.Properties.Name -contains "Exception" -and $null -ne $exception.Exception) {
        return Get-HttpStatusCode $exception.Exception
    }
    if ($exception.PSObject.Properties.Name -contains "Response" -and $null -ne $exception.Response) {
        try {
            return [int]$exception.Response.StatusCode
        } catch {
            try {
                return [int]$exception.Response.StatusCode.value__
            } catch {
                return $null
            }
        }
    }
    return $null
}

function Get-ServiceHealthState {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/actuator/health" -Method GET -TimeoutSec 5 -MaximumRedirection 0
        $content = if ($null -ne $resp.Content) { [string]$resp.Content } else { "" }
        $responsePath = $null
        try {
            $responsePath = $resp.BaseResponse.ResponseUri.AbsolutePath
        } catch {
        }
        try {
            $json = $content | ConvertFrom-Json -ErrorAction Stop
            if ((Get-HealthStatus $json) -eq "UP") {
                return "UP"
            }
        } catch {
        }

        if (
            (($responsePath) -and ($responsePath -ne "/actuator/health")) -or
            ($content -match '<title>\s*Please sign in\s*</title>') -or
            ($content -match 'action="/login"')
        ) {
            return "UP_SECURED"
        }
    } catch {
        $httpCode = Get-HttpStatusCode $_
        if ($httpCode -in @(301, 302, 303, 307, 308, 401, 403)) {
            return "UP_SECURED"
        }
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

    $argsList = @()
    if ($skywalkingEnabled) {
        $argsList += "-javaagent:$skywalkingAgentPath"
        $argsList += "-Dskywalking.agent.service_name=$name"
        $argsList += "-Dskywalking.collector.backend_service=$skywalkingBackend"
    }
    foreach ($jvmOpt in ($serviceJvmOpts -split '\s+')) {
        if (-not [string]::IsNullOrWhiteSpace($jvmOpt)) {
            $argsList += $jvmOpt
        }
    }
    $argsList += "-jar"
    $argsList += $jarPath
    $argsList += "--spring.profiles.active=$($svc.profiles)"
    $start = Get-Date
    $proc = Start-Process -FilePath $java -ArgumentList $argsList -WorkingDirectory $root -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru

    $deadline = $start.AddSeconds($startupTimeoutSeconds)
    $status = "TIMEOUT"
    $healthy = $false
    while ((Get-Date) -lt $deadline) {
        if ($proc.HasExited) {
            $status = "EXITED:$($proc.ExitCode)"
            break
        }
        $healthState = Get-ServiceHealthState -Port $port
        if (-not [string]::IsNullOrWhiteSpace($healthState)) {
            $status = $healthState
            $healthy = $true
            break
        }
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
if (-not $allOk) {
    exit 1
}
