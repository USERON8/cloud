param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$Services = $null
$NoKillPorts = $false
$DryRun = $false

for ($index = 0; $index -lt $CliArgs.Count; $index++) {
    $arg = $CliArgs[$index]
    if ($arg -in @("--dry-run", "-DryRun")) {
        $DryRun = $true
        continue
    }
    if ($arg -eq "--kill-ports") {
        $NoKillPorts = $false
        continue
    }
    if ($arg -in @("--no-kill-ports", "-NoKillPorts")) {
        $NoKillPorts = $true
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
    }
}

function Get-ServiceCatalog {
    return @(
        [pscustomobject]@{ name = "user-service";    port = 8082; jar = "services\user-service\target\user-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "auth-service";    port = 8081; jar = "services\auth-service\target\auth-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "product-service"; port = 8084; jar = "services\product-service\target\product-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "stock-service";   port = 8085; jar = "services\stock-service\target\stock-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "payment-service"; port = 8086; jar = "services\payment-service\target\payment-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "order-service";   port = 8083; jar = "services\order-service\target\order-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "search-service";  port = 8087; jar = "services\search-service\target\search-service-1.1.0.jar"; profiles = "dev" },
        [pscustomobject]@{ name = "gateway";         port = 8080; jar = "services\gateway\target\gateway-1.1.0.jar"; profiles = "dev,route" }
    )
}

function Resolve-SelectedServices {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Catalog,
        [string]$RequestedServices
    )

    if ([string]::IsNullOrWhiteSpace($RequestedServices)) {
        return $Catalog
    }

    $serviceIndex = @{}
    foreach ($service in $Catalog) {
        $serviceIndex[$service.name] = $service
    }

    $selectedServices = @()
    $missingServices = @()
    $seen = @{}
    foreach ($candidate in ($RequestedServices -split "," | ForEach-Object { $_.Trim() })) {
        if ([string]::IsNullOrWhiteSpace($candidate) -or $seen.ContainsKey($candidate)) {
            continue
        }
        $seen[$candidate] = $true
        if ($serviceIndex.ContainsKey($candidate)) {
            $selectedServices += $serviceIndex[$candidate]
        } else {
            $missingServices += $candidate
        }
    }

    if ($missingServices.Count -gt 0) {
        throw ("Unknown services: {0}" -f ($missingServices -join ", "))
    }
    if ($selectedServices.Count -eq 0) {
        throw "No services selected. Use --services=name1,name2."
    }

    return $selectedServices
}

function Resolve-WritableDir {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PreferredDir,
        [Parameter(Mandatory = $true)]
        [string]$FallbackDir
    )

    try {
        New-Item -ItemType Directory -Force -Path $PreferredDir | Out-Null
        $probePath = Join-Path $PreferredDir ([System.IO.Path]::GetRandomFileName())
        New-Item -ItemType File -Path $probePath -Force | Out-Null
        Remove-Item -Path $probePath -Force -ErrorAction SilentlyContinue
        return $PreferredDir
    } catch {
        New-Item -ItemType Directory -Force -Path $FallbackDir | Out-Null
        return $FallbackDir
    }
}

function Get-ServiceModulePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JarRelativePath
    )

    return Split-Path (Split-Path $JarRelativePath -Parent) -Parent
}

function Ensure-ServiceArtifacts {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Services,
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $missingServices = @()
    $modulePaths = New-Object System.Collections.Generic.List[string]
    $moduleSeen = @{}
    foreach ($service in $Services) {
        $jarPath = Join-Path $Root $service.jar
        if (Test-Path $jarPath) {
            continue
        }

        $missingServices += $service
        $modulePath = Get-ServiceModulePath -JarRelativePath $service.jar
        if (-not $moduleSeen.ContainsKey($modulePath)) {
            $modulePaths.Add($modulePath)
            $moduleSeen[$modulePath] = $true
        }
    }

    if ($missingServices.Count -eq 0) {
        return
    }

    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -eq $mvnCommand) {
        throw "mvn not found, cannot build missing service artifacts"
    }

    $requestedServices = ($missingServices | ForEach-Object { $_.name }) -join ","
    $requestedModules = $modulePaths -join ","
    Write-Host ("ARTIFACT_BUILD action=package services={0} modules={1}" -f $requestedServices, $requestedModules)

    Push-Location $Root
    try {
        & $mvnCommand.Source "-Dmaven.test.skip=true" "-DskipTests" "-T" "1C" "-pl" $requestedModules "-am" "package"
        if ($LASTEXITCODE -ne 0) {
            throw ("maven package failed for services: {0}" -f $requestedServices)
        }
    } finally {
        Pop-Location
    }

    $remainingMissing = $missingServices | Where-Object { -not (Test-Path (Join-Path $Root $_.jar)) }
    if ($remainingMissing.Count -gt 0) {
        throw ("service artifacts still missing after package: {0}" -f (($remainingMissing | ForEach-Object { $_.name }) -join ","))
    }
}

function Get-ServiceEnvironmentOverrides {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ServiceName
    )

    $overrides = @{}
    switch ($ServiceName) {
        "user-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20880"
            $overrides["APP_MYBATIS_ILLEGAL_SQL_ENABLED"] = "false"
        }
        "order-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20882"
        }
        "product-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20884"
        }
        "stock-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20886"
        }
        "payment-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20888"
        }
        "search-service" {
            $overrides["DUBBO_PROTOCOL_PORT"] = "20890"
        }
    }

    return $overrides
}

function Update-ServiceStateFiles {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Catalog,
        [Parameter(Mandatory = $true)]
        [object[]]$Results,
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $acceptanceDir = Join-Path $Root ".tmp\acceptance"
    New-Item -ItemType Directory -Force -Path $acceptanceDir | Out-Null

    $csvPath = Join-Path $acceptanceDir "startup.csv"
    $stateByService = @{}
    if (Test-Path $csvPath) {
        foreach ($row in Import-Csv $csvPath) {
            if ([string]::IsNullOrWhiteSpace($row.service)) {
                continue
            }
            $stateByService[$row.service] = [pscustomobject]@{
                service         = $row.service
                port            = [int]$row.port
                pid             = [long]$row.pid
                status          = $row.status
                startup_seconds = [int]$row.startup_seconds
                out_log         = $row.out_log
                err_log         = $row.err_log
            }
        }
    }
    foreach ($result in $Results) {
        $stateByService[$result.service] = $result
    }

    $orderedRows = foreach ($service in $Catalog) {
        if ($stateByService.ContainsKey($service.name)) {
            $stateByService[$service.name]
        }
    }
    $orderedRows | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

    $pidsPath = Join-Path $acceptanceDir "pids.txt"
    $pidStateByService = @{}
    if (Test-Path $pidsPath) {
        foreach ($line in Get-Content $pidsPath) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            $parts = $line -split ",", 4
            if ($parts.Count -lt 4) {
                continue
            }
            $pidStateByService[$parts[0]] = [pscustomobject]@{
                service = $parts[0]
                port    = $parts[1]
                pid     = $parts[2]
                status  = $parts[3]
            }
        }
    }
    foreach ($result in $Results) {
        $pidStateByService[$result.service] = [pscustomobject]@{
            service = $result.service
            port    = $result.port
            pid     = $result.pid
            status  = $result.status
        }
    }

    $pidLines = foreach ($service in $Catalog) {
        if ($pidStateByService.ContainsKey($service.name)) {
            $row = $pidStateByService[$service.name]
            "{0},{1},{2},{3}" -f $row.service, $row.port, $row.pid, $row.status
        }
    }
    Set-Content -Path $pidsPath -Value $pidLines -Encoding UTF8
}

$killPorts = -not $NoKillPorts
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")
. (Join-Path $PSScriptRoot "lib\runtime.ps1")

$catalog = Get-ServiceCatalog
$services = Resolve-SelectedServices -Catalog $catalog -RequestedServices $Services
$selectedServiceNames = $services | ForEach-Object { $_.name }
Write-Host ("SERVICE_SCOPE services={0}" -f ($selectedServiceNames -join ","))

$servicePorts = $services | ForEach-Object { [int]$_.port }
if ($killPorts) {
    foreach ($port in $servicePorts) {
        Kill-PortOwner -Port $port -DryRun:$DryRun
    }
}
if ($DryRun) {
    Write-Host "DRY_RUN_DONE script=start-services"
    exit 0
}

Ensure-ServiceArtifacts -Services $services -Root $root
Set-ServiceRuntimeEnvironment -Root $root

$java = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    "java"
}

$runtimeLogRoot = if ([string]::IsNullOrWhiteSpace($env:SERVICE_RUNTIME_LOG_ROOT)) {
    Join-Path $root ".tmp\service-runtime"
} else {
    $env:SERVICE_RUNTIME_LOG_ROOT
}
New-Item -ItemType Directory -Force -Path $runtimeLogRoot | Out-Null

$skywalkingAgentJar = Join-Path $root "docker\monitor\skywalking\agent\skywalking-agent.jar"
$skywalkingEnabled = $true
if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_ENABLED)) {
    $rawSkywalking = $env:SKYWALKING_ENABLED.Trim().ToLowerInvariant()
    if ($rawSkywalking -in @("false", "0", "no", "off")) {
        $skywalkingEnabled = $false
    }
}
$skywalkingAgentAvailable = $skywalkingEnabled -and (Test-Path $skywalkingAgentJar)
if (-not $skywalkingEnabled) {
    Write-Host "SKYWALKING disabled reason=env-disabled"
} elseif (-not $skywalkingAgentAvailable) {
    Write-Host ("SKYWALKING disabled reason=agent-not-found path={0}" -f $skywalkingAgentJar)
}
$serviceJvmOpts = if ([string]::IsNullOrWhiteSpace($env:SERVICE_JVM_OPTS)) {
    "-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError"
} else {
    $env:SERVICE_JVM_OPTS
}
if (-not [string]::IsNullOrWhiteSpace($env:NACOS_GRPC_PORT_OFFSET) -and $serviceJvmOpts -notmatch "nacos\.server\.grpc\.port\.offset") {
    $serviceJvmOpts = "$serviceJvmOpts -Dnacos.server.grpc.port.offset=$($env:NACOS_GRPC_PORT_OFFSET)"
}
$startupTimeoutSeconds = 300
if (-not [string]::IsNullOrWhiteSpace($env:SERVICE_STARTUP_TIMEOUT_SECONDS)) {
    $parsedTimeout = 0
    if ([int]::TryParse($env:SERVICE_STARTUP_TIMEOUT_SECONDS, [ref]$parsedTimeout) -and $parsedTimeout -gt 0) {
        $startupTimeoutSeconds = $parsedTimeout
    }
}
$healthStabilizationSeconds = 8
if (-not [string]::IsNullOrWhiteSpace($env:SERVICE_HEALTH_STABILIZATION_SECONDS)) {
    $parsedStabilization = 0
    if ([int]::TryParse($env:SERVICE_HEALTH_STABILIZATION_SECONDS, [ref]$parsedStabilization) -and $parsedStabilization -ge 0) {
        $healthStabilizationSeconds = $parsedStabilization
    }
}

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

function Get-ServiceHealthState {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $request = [System.Net.HttpWebRequest]::Create("http://127.0.0.1:$Port/actuator/health")
    $request.Method = "GET"
    $request.Timeout = 5000
    $request.ReadWriteTimeout = 5000
    $request.AllowAutoRedirect = $false

    $response = $null
    try {
        try {
            $response = [System.Net.HttpWebResponse]$request.GetResponse()
        } catch [System.Net.WebException] {
            if ($null -ne $_.Exception.Response) {
                $response = [System.Net.HttpWebResponse]$_.Exception.Response
            } else {
                return $null
            }
        }

        if ($null -eq $response) {
            return $null
        }

        $statusCode = [int]$response.StatusCode
        $responsePath = $null
        try {
            $responsePath = $response.ResponseUri.AbsolutePath
        } catch {
        }

        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        $content = $reader.ReadToEnd()
        $reader.Dispose()

        if ($statusCode -eq 200) {
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
        }

        if ($statusCode -in @(301, 302, 303, 307, 308, 401, 403)) {
            return "UP_SECURED"
        }
    } finally {
        if ($null -ne $response) {
            $response.Dispose()
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

    $serviceDir = (Resolve-Path (Join-Path (Split-Path $jarPath -Parent) "..")).Path
    $runtimeLogDir = Join-Path $runtimeLogRoot $name
    $preferredServiceLogDir = Join-Path $serviceDir "logs"
    New-Item -ItemType Directory -Force -Path $runtimeLogDir | Out-Null
    $serviceLogDir = Resolve-WritableDir -PreferredDir $preferredServiceLogDir -FallbackDir (Join-Path $runtimeLogDir "app-logs")
    if ($serviceLogDir -ne $preferredServiceLogDir) {
        Write-Host ("LOG_DIR_FALLBACK service={0} path={1}" -f $name, $serviceLogDir)
    }

    $outLog = Join-Path $runtimeLogDir "stdout.log"
    $errLog = Join-Path $runtimeLogDir "stderr.log"
    if (Test-Path $outLog) { Remove-Item $outLog -Force }
    if (Test-Path $errLog) { Remove-Item $errLog -Force }

    $argsList = @()
    $previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
    $previousAgentName = $env:SW_AGENT_NAME
    $previousLoggingDir = $env:SW_LOGGING_DIR
    $serviceEnvOverrides = Get-ServiceEnvironmentOverrides -ServiceName $name
    $previousServiceEnv = @{}
    if ($skywalkingAgentAvailable) {
        $skywalkingLogDir = Join-Path $runtimeLogDir "skywalking-agent"
        New-Item -ItemType Directory -Force -Path $skywalkingLogDir | Out-Null
        $javaToolOptions = "-javaagent:$skywalkingAgentJar"
        if (-not [string]::IsNullOrWhiteSpace($previousJavaToolOptions)) {
            $javaToolOptions = "$previousJavaToolOptions $javaToolOptions"
        }
        $env:JAVA_TOOL_OPTIONS = $javaToolOptions
        $env:SW_AGENT_NAME = $name
        $env:SW_LOGGING_DIR = $skywalkingLogDir
    }
    foreach ($entry in $serviceEnvOverrides.GetEnumerator()) {
        $previousServiceEnv[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key, "Process")
        Set-Item -Path ("Env:{0}" -f $entry.Key) -Value $entry.Value
    }
    foreach ($jvmOpt in ($serviceJvmOpts -split '\s+')) {
        if (-not [string]::IsNullOrWhiteSpace($jvmOpt)) {
            $argsList += $jvmOpt
        }
    }
    if ($serviceJvmOpts -notmatch "HeapDumpPath") {
        $heapDumpPath = Join-Path $serviceLogDir "heap.hprof"
        $argsList += "-XX:HeapDumpPath=$heapDumpPath"
    }
    $argsList += "-jar"
    $argsList += $jarPath
    $argsList += "--spring.profiles.active=$($svc.profiles)"
    $argsList += "--logging.file.path=$serviceLogDir"
    $start = Get-Date
    try {
        $proc = Start-Process -FilePath $java -ArgumentList $argsList -WorkingDirectory $root -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
    } finally {
        if ($skywalkingAgentAvailable) {
            if ($null -ne $previousJavaToolOptions) {
                $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
            } else {
                Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
            }
            if ($null -ne $previousAgentName) {
                $env:SW_AGENT_NAME = $previousAgentName
            } else {
                Remove-Item Env:\SW_AGENT_NAME -ErrorAction SilentlyContinue
            }
            if ($null -ne $previousLoggingDir) {
                $env:SW_LOGGING_DIR = $previousLoggingDir
            } else {
                Remove-Item Env:\SW_LOGGING_DIR -ErrorAction SilentlyContinue
            }
        }
        foreach ($entry in $serviceEnvOverrides.GetEnumerator()) {
            if ($null -ne $previousServiceEnv[$entry.Key]) {
                Set-Item -Path ("Env:{0}" -f $entry.Key) -Value $previousServiceEnv[$entry.Key]
            } else {
                Remove-Item ("Env:{0}" -f $entry.Key) -ErrorAction SilentlyContinue
            }
        }
    }

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
            $stabilized = $true
            if ($healthStabilizationSeconds -gt 0) {
                $stabilizationDeadline = (Get-Date).AddSeconds($healthStabilizationSeconds)
                while ((Get-Date) -lt $stabilizationDeadline) {
                    if ($proc.HasExited) {
                        $stabilized = $false
                        break
                    }
                    $confirmedHealth = Get-ServiceHealthState -Port $port
                    if ([string]::IsNullOrWhiteSpace($confirmedHealth)) {
                        $stabilized = $false
                        break
                    }
                    Start-Sleep -Seconds 2
                }
            }
            if ($stabilized) {
                $status = $healthState
                $healthy = $true
                break
            }
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

Update-ServiceStateFiles -Catalog $catalog -Results $results -Root $root

if ($allOk) { Write-Host "STARTUP_OK" } else { Write-Host "STARTUP_FAILED" }
$results | Format-Table -AutoSize
if (-not $allOk) {
    exit 1
}
