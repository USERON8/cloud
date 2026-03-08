param(
    [switch]$WithMonitoring,
    [switch]$NoKillPorts,
    [switch]$SkipContainers,
    [switch]$SkipServices,
    [switch]$OpenDashboards,
    [switch]$EnableSkyWalking,
    [string]$SkyWalkingAgentPath,
    [string]$SkyWalkingCollectorBackendService,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

foreach ($arg in $args) {
    if ($arg -eq "--with-monitoring") { $WithMonitoring = $true }
    if ($arg -eq "--no-kill-ports") { $NoKillPorts = $true }
    if ($arg -eq "--skip-containers") { $SkipContainers = $true }
    if ($arg -eq "--skip-services") { $SkipServices = $true }
    if ($arg -eq "--open-dashboards") { $OpenDashboards = $true }
    if ($arg -eq "--enable-skywalking") { $EnableSkyWalking = $true }
    if ($arg -eq "--dry-run") { $DryRun = $true }
    if ($arg -like "--skywalking-agent-path=*") {
        $SkyWalkingAgentPath = ($arg -split "=", 2)[1]
    }
    if ($arg -like "--skywalking-backend=*") {
        $SkyWalkingCollectorBackendService = ($arg -split "=", 2)[1]
    }
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\runtime.ps1")

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptPath,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)]
        [string]$ErrorMessage
    )

    & $ScriptPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw $ErrorMessage
    }
}

function Wait-Infrastructure {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [switch]$WaitMonitoring,
        [switch]$WaitSkyWalking
    )

    $targets = @(
        @{ name = "mysql"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_MYSQL" -DefaultValue 13306) },
        @{ name = "redis"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_REDIS" -DefaultValue 16379) },
        @{ name = "nacos"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_NACOS_HTTP" -DefaultValue 18848) },
        @{ name = "rocketmq-namesrv"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_RMQ_NAMESRV" -DefaultValue 19876) },
        @{ name = "elasticsearch"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_ES_HTTP" -DefaultValue 19200) },
        @{ name = "minio"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_MINIO_API" -DefaultValue 19000) },
        @{ name = "seata-server"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_SEATA_SERVER" -DefaultValue 18091) }
    )

    if ($WaitSkyWalking) {
        $targets += @{ name = "skywalking-oap"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_SKYWALKING_OAP_GRPC" -DefaultValue 11800) }
    }

    if ($WaitMonitoring) {
        $targets += @(
            @{ name = "prometheus"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_PROMETHEUS_HTTP" -DefaultValue 19099) },
            @{ name = "grafana"; port = (Get-DockerPortValue -Root $RepoRoot -Name "PORT_GRAFANA_HTTP" -DefaultValue 13000) }
        )
    }

    foreach ($target in $targets) {
        Write-Host ("WAIT_PORT name={0} port={1} status=waiting" -f $target.name, $target.port)
        if (-not (Wait-TcpPort -Host "127.0.0.1" -Port $target.port -TimeoutSeconds 120 -SleepMilliseconds 1000)) {
            throw ("Infrastructure port not ready: {0}:{1}" -f $target.name, $target.port)
        }
        Write-Host ("WAIT_PORT name={0} port={1} status=ready" -f $target.name, $target.port)
    }
}

function Configure-SkyWalking {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [switch]$Requested,
        [string]$AgentPath,
        [string]$CollectorBackend
    )

    $resolvedAgentPath = if (-not [string]::IsNullOrWhiteSpace($AgentPath)) {
        $AgentPath
    } else {
        $env:SKYWALKING_AGENT_PATH
    }

    if (-not [string]::IsNullOrWhiteSpace($resolvedAgentPath)) {
        try {
            $resolvedAgentPath = (Resolve-Path $resolvedAgentPath -ErrorAction Stop).Path
        } catch {
            if ($Requested) {
                throw ("SkyWalking agent not found: {0}" -f $resolvedAgentPath)
            }
            $resolvedAgentPath = $null
        }
    }

    if ([string]::IsNullOrWhiteSpace($resolvedAgentPath)) {
        if ($Requested) {
            throw "SkyWalking requested but no agent path was provided."
        }
        return $false
    }

    $resolvedCollector = if (-not [string]::IsNullOrWhiteSpace($CollectorBackend)) {
        $CollectorBackend
    } elseif (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_COLLECTOR_BACKEND_SERVICE)) {
        $env:SKYWALKING_COLLECTOR_BACKEND_SERVICE
    } else {
        "127.0.0.1:{0}" -f (Get-DockerPortValue -Root $RepoRoot -Name "PORT_SKYWALKING_OAP_GRPC" -DefaultValue 11800)
    }

    $env:SKYWALKING_AGENT_PATH = $resolvedAgentPath
    $env:SKYWALKING_COLLECTOR_BACKEND_SERVICE = $resolvedCollector
    Write-Host ("SKYWALKING enabled=true agent={0} backend={1}" -f $resolvedAgentPath, $resolvedCollector)
    return $true
}

Write-Host "=== START PLATFORM ==="

$containerArgs = @()
if ($WithMonitoring) { $containerArgs += "--with-monitoring" }
if ($NoKillPorts) { $containerArgs += "--no-kill-ports" }
if ($DryRun) { $containerArgs += "--dry-run" }

if (-not $SkipContainers) {
    Write-Host "STEP 1/3 containers=start"
    Invoke-Step -ScriptPath (Join-Path $PSScriptRoot "start-containers.ps1") `
        -Arguments $containerArgs `
        -ErrorMessage "Container startup failed"
} else {
    Write-Host "STEP 1/3 containers=skipped"
}

$skywalkingActive = Configure-SkyWalking -RepoRoot $root `
    -Requested:$EnableSkyWalking `
    -AgentPath $SkyWalkingAgentPath `
    -CollectorBackend $SkyWalkingCollectorBackendService

if (-not $DryRun -and -not $SkipServices) {
    Write-Host "STEP 2/3 infrastructure=wait"
    Wait-Infrastructure -RepoRoot $root -WaitMonitoring:$WithMonitoring -WaitSkyWalking:$skywalkingActive
}

Set-ServiceRuntimeEnvironment -Root $root

$serviceArgs = @()
if ($NoKillPorts) { $serviceArgs += "--no-kill-ports" }
if ($DryRun) { $serviceArgs += "--dry-run" }

if (-not $SkipServices) {
    Write-Host "STEP 3/3 services=start"
    Invoke-Step -ScriptPath (Join-Path $PSScriptRoot "start-services.ps1") `
        -Arguments $serviceArgs `
        -ErrorMessage "Service startup failed"
} else {
    Write-Host "STEP 3/3 services=skipped"
}

$dashboardUrls = @(
    "http://127.0.0.1:{0}" -f (Get-DockerPortValue -Root $root -Name "PORT_NACOS_HTTP" -DefaultValue 18848),
    "http://127.0.0.1:{0}" -f (Get-DockerPortValue -Root $root -Name "PORT_KIBANA_HTTP" -DefaultValue 15601)
)
if ($WithMonitoring) {
    $dashboardUrls += @(
        "http://127.0.0.1:{0}" -f (Get-DockerPortValue -Root $root -Name "PORT_PROMETHEUS_HTTP" -DefaultValue 19099),
        "http://127.0.0.1:{0}" -f (Get-DockerPortValue -Root $root -Name "PORT_GRAFANA_HTTP" -DefaultValue 13000)
    )
}
if ($skywalkingActive) {
    $dashboardUrls += "http://127.0.0.1:{0}" -f (Get-DockerPortValue -Root $root -Name "PORT_SKYWALKING_UI" -DefaultValue 13001)
}

if ($OpenDashboards -and -not $DryRun) {
    foreach ($url in ($dashboardUrls | Select-Object -Unique)) {
        Open-LocalUrl -Url $url
    }
}

Write-Host "PLATFORM_READY"
