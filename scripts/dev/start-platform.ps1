param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$WithMonitoring = $false
$NoKillPorts = $false
$SkipContainers = $false
$SkipServices = $false
$Services = $null
$OpenDashboards = $false
$EnableSkyWalking = $false
$SkyWalkingAgentPath = $null
$SkyWalkingCollectorBackendService = $null
$DryRun = $false

for ($index = 0; $index -lt $CliArgs.Count; $index++) {
    $arg = $CliArgs[$index]
    if ($arg -in @("--with-monitoring", "-WithMonitoring")) {
        $WithMonitoring = $true
        continue
    }
    if ($arg -in @("--no-kill-ports", "-NoKillPorts")) {
        $NoKillPorts = $true
        continue
    }
    if ($arg -in @("--skip-containers", "-SkipContainers")) {
        $SkipContainers = $true
        continue
    }
    if ($arg -in @("--skip-services", "-SkipServices")) {
        $SkipServices = $true
        continue
    }
    if ($arg -like "--services=*") {
        $Services = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -like "-Services=*") {
        $Services = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -eq "-Services") {
        if (($index + 1) -ge $CliArgs.Count) {
            throw "Missing value for -Services"
        }
        $index += 1
        $Services = $CliArgs[$index]
        continue
    }
    if ($arg -in @("--open-dashboards", "-OpenDashboards")) {
        $OpenDashboards = $true
        continue
    }
    if ($arg -in @("--enable-skywalking", "-EnableSkyWalking")) {
        $EnableSkyWalking = $true
        continue
    }
    if ($arg -in @("--dry-run", "-DryRun")) {
        $DryRun = $true
        continue
    }
    if ($arg -like "--skywalking-agent-path=*") {
        $SkyWalkingAgentPath = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -like "-SkyWalkingAgentPath=*") {
        $SkyWalkingAgentPath = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -eq "-SkyWalkingAgentPath") {
        if (($index + 1) -ge $CliArgs.Count) {
            throw "Missing value for -SkyWalkingAgentPath"
        }
        $index += 1
        $SkyWalkingAgentPath = $CliArgs[$index]
        continue
    }
    if ($arg -like "--skywalking-backend=*") {
        $SkyWalkingCollectorBackendService = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -like "-SkyWalkingCollectorBackendService=*") {
        $SkyWalkingCollectorBackendService = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -eq "-SkyWalkingCollectorBackendService") {
        if (($index + 1) -ge $CliArgs.Count) {
            throw "Missing value for -SkyWalkingCollectorBackendService"
        }
        $index += 1
        $SkyWalkingCollectorBackendService = $CliArgs[$index]
    }
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\runtime.ps1")
. (Join-Path $PSScriptRoot "lib\skywalking.ps1")

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
        if (-not (Wait-TcpPort -TargetHost "127.0.0.1" -Port $target.port -TimeoutSeconds 120 -SleepMilliseconds 1000)) {
            throw ("Infrastructure port not ready: {0}:{1}" -f $target.name, $target.port)
        }
        Write-Host ("WAIT_PORT name={0} port={1} status=ready" -f $target.name, $target.port)
    }
}

function Configure-SkyWalking {
    $allowDownload = -not $DryRun
    return (Configure-SkyWalkingRuntime -RepoRoot $root `
        -Requested:$EnableSkyWalking `
        -AgentPath $SkyWalkingAgentPath `
        -CollectorBackend $SkyWalkingCollectorBackendService `
        -AllowDownload:$allowDownload)
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

$skywalkingActive = Configure-SkyWalking

if (-not $DryRun -and -not $SkipServices) {
    Write-Host "STEP 2/3 infrastructure=wait"
    Wait-Infrastructure -RepoRoot $root -WaitMonitoring:$WithMonitoring -WaitSkyWalking:$skywalkingActive
}

Set-ServiceRuntimeEnvironment -Root $root

$serviceArgs = @()
if ($NoKillPorts) { $serviceArgs += "--no-kill-ports" }
if ($DryRun) { $serviceArgs += "--dry-run" }
if (-not [string]::IsNullOrWhiteSpace($Services)) { $serviceArgs += "--services=$Services" }

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
