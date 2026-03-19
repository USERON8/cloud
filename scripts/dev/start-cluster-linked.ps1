param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$keepHostServices = $false
$dryRun = $false

function Assert-DockerDaemonReady {
    $dockerCommand = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -eq $dockerCommand) {
        throw "docker command not found, start Docker Desktop first"
    }

    & $dockerCommand.Source version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker daemon is not ready. Start Docker Desktop first."
    }
}

function Resolve-ServiceCatalog {
    return @(
        [pscustomobject]@{ name = "auth-service"; hostPortVar = "PORT_AUTH_SERVICE_HOST"; hostPort = 28081; jar = "services\auth-service\target\auth-service-1.1.0.jar" },
        [pscustomobject]@{ name = "user-service"; hostPortVar = "PORT_USER_SERVICE_HOST"; hostPort = 28082; jar = "services\user-service\target\user-service-1.1.0.jar" },
        [pscustomobject]@{ name = "product-service"; hostPortVar = "PORT_PRODUCT_SERVICE_HOST"; hostPort = 28084; jar = "services\product-service\target\product-service-1.1.0.jar" },
        [pscustomobject]@{ name = "stock-service"; hostPortVar = "PORT_STOCK_SERVICE_HOST"; hostPort = 28085; jar = "services\stock-service\target\stock-service-1.1.0.jar" },
        [pscustomobject]@{ name = "payment-service"; hostPortVar = "PORT_PAYMENT_SERVICE_HOST"; hostPort = 28086; jar = "services\payment-service\target\payment-service-1.1.0.jar" },
        [pscustomobject]@{ name = "order-service"; hostPortVar = "PORT_ORDER_SERVICE_HOST"; hostPort = 28083; jar = "services\order-service\target\order-service-1.1.0.jar" },
        [pscustomobject]@{ name = "search-service"; hostPortVar = "PORT_SEARCH_SERVICE_HOST"; hostPort = 28087; jar = "services\search-service\target\search-service-1.1.0.jar" },
        [pscustomobject]@{ name = "gateway"; hostPortVar = "PORT_GATEWAY_SERVICE_HOST"; hostPort = 28080; jar = "services\gateway\target\gateway-1.1.0.jar" }
    )
}

function Resolve-SelectedServices {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Catalog,
        [string]$RequestedServices
    )

    $index = @{}
    foreach ($service in $Catalog) {
        $index[$service.name] = $service
    }

    if ([string]::IsNullOrWhiteSpace($RequestedServices)) {
        return $Catalog
    }

    $selected = New-Object System.Collections.Generic.List[object]
    $seen = @{}
    foreach ($name in ($RequestedServices -split "," | ForEach-Object { $_.Trim() })) {
        if ([string]::IsNullOrWhiteSpace($name) -or $seen.ContainsKey($name)) {
            continue
        }
        $seen[$name] = $true
        if (-not $index.ContainsKey($name)) {
            throw "unknown service: $name"
        }
        $selected.Add($index[$name])
    }

    if (-not $seen.ContainsKey("gateway")) {
        $selected.Add($index["gateway"])
    }

    return $selected
}

function Assert-HostAcceptance {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [object[]]$Services
    )

    $startupPath = Join-Path $Root ".tmp\acceptance\startup.csv"
    if (-not (Test-Path $startupPath)) {
        throw "host acceptance not found, run start-host-linked first"
    }

    $rows = Import-Csv $startupPath
    $rowIndex = @{}
    foreach ($row in $rows) {
        $rowIndex[$row.service] = $row
    }

    foreach ($service in $Services) {
        if (-not $rowIndex.ContainsKey($service.name)) {
            throw ("host acceptance missing service: {0}" -f $service.name)
        }
        if ($rowIndex[$service.name].status -notin @("UP", "UP_SECURED")) {
            throw ("host acceptance failed for service: {0} status={1}" -f $service.name, $rowIndex[$service.name].status)
        }
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
        & $mvnCommand.Source "-DskipTests" "-T" "1C" "-pl" $requestedModules "-am" "package"
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

function Wait-ServicePorts {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Services,
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    . (Join-Path $PSScriptRoot "lib\runtime.ps1")

    $clusterStartupTimeoutSeconds = 900
    if (-not [string]::IsNullOrWhiteSpace($env:CLUSTER_STARTUP_TIMEOUT_SECONDS)) {
        $parsedTimeout = 0
        if ([int]::TryParse($env:CLUSTER_STARTUP_TIMEOUT_SECONDS, [ref]$parsedTimeout) -and $parsedTimeout -gt 0) {
            $clusterStartupTimeoutSeconds = $parsedTimeout
        }
    }

    function Get-HealthStatus {
        param($ResponseBody)

        if ($null -eq $ResponseBody) {
            return $null
        }
        if ($ResponseBody.PSObject.Properties.Name -contains "status") {
            return $ResponseBody.status
        }
        if ($ResponseBody.PSObject.Properties.Name -contains "data" -and $null -ne $ResponseBody.data) {
            if ($ResponseBody.data.PSObject.Properties.Name -contains "status") {
                return $ResponseBody.data.status
            }
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

    foreach ($service in $Services) {
        Write-Host ("WAIT_CLUSTER service={0} port={1} status=waiting" -f $service.name, $service.hostPort)
    }

    $deadline = (Get-Date).AddSeconds($clusterStartupTimeoutSeconds)
    $readyServices = @{}
    while ((Get-Date) -lt $deadline -and $readyServices.Count -lt $Services.Count) {
        foreach ($service in $Services) {
            if ($readyServices.ContainsKey($service.name)) {
                continue
            }

            $healthy = Get-ServiceHealthState -Port $service.hostPort
            if ($healthy -in @("UP", "UP_SECURED")) {
                $readyServices[$service.name] = $healthy
                Write-Host ("WAIT_CLUSTER service={0} port={1} status={2}" -f $service.name, $service.hostPort, $healthy)
            }
        }

        if ($readyServices.Count -lt $Services.Count) {
            Start-Sleep -Seconds 2
        }
    }

    if ($readyServices.Count -lt $Services.Count) {
        $pending = $Services |
            Where-Object { -not $readyServices.ContainsKey($_.name) } |
            ForEach-Object { "{0}:{1}" -f $_.name, $_.hostPort }
        throw ("cluster service health not ready: {0}" -f ($pending -join ", "))
    }

    $nginxPort = Get-DockerPortValue -Root $Root -Name "PORT_NGINX_HTTP" -DefaultValue 18080
    Write-Host ("WAIT_CLUSTER service=nginx port={0} status=waiting" -f $nginxPort)
    $nginxDeadline = (Get-Date).AddSeconds($clusterStartupTimeoutSeconds)
    $nginxHealthy = $null
    while ((Get-Date) -lt $nginxDeadline) {
        $nginxHealthy = Get-ServiceHealthState -Port $nginxPort
        if ($nginxHealthy -in @("UP", "UP_SECURED")) {
            break
        }
        Start-Sleep -Seconds 2
    }
    if ($nginxHealthy -notin @("UP", "UP_SECURED")) {
        throw ("nginx gateway health not ready: {0}" -f $nginxPort)
    }
    Write-Host ("WAIT_CLUSTER service=nginx port={0} status={1}" -f $nginxPort, $nginxHealthy)
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$catalog = Resolve-ServiceCatalog
$requestedServices = $null
foreach ($arg in $CliArgs) {
    if ($arg -like "--services=*") {
        $requestedServices = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -eq "--keep-host-services") {
        $keepHostServices = $true
        continue
    }
    if ($arg -in @("--dry-run", "-DryRun")) {
        $dryRun = $true
    }
}
$services = Resolve-SelectedServices -Catalog $catalog -RequestedServices $requestedServices
$serviceNames = $services | ForEach-Object { $_.name }

Write-Host ("SERVICE_SCOPE services={0}" -f ($serviceNames -join ","))

Assert-HostAcceptance -Root $root -Services $services

if ($dryRun) {
    if (-not $keepHostServices) {
        & (Join-Path $PSScriptRoot "stop-services.ps1") "--dry-run" ("--services={0}" -f ($serviceNames -join ","))
        if ($LASTEXITCODE -ne 0) {
            throw "host service dry-run stop failed before cluster deployment"
        }
    }
    Write-Host ("DRY_RUN_DONE script=start-cluster-linked services={0} keepHostServices={1}" -f ($serviceNames -join ","), $keepHostServices)
    exit 0
}

Assert-DockerDaemonReady
Ensure-ServiceArtifacts -Services $services -Root $root

if (-not $keepHostServices) {
    & (Join-Path $PSScriptRoot "stop-services.ps1") ("--services={0}" -f ($serviceNames -join ","))
    if ($LASTEXITCODE -ne 0) {
        throw "host service stop failed before cluster deployment"
    }
    Start-Sleep -Seconds 5
}

$dockerDir = Join-Path $root "docker"

foreach ($service in $services) {
    Set-Item -Path ("Env:{0}" -f $service.hostPortVar) -Value ([string]$service.hostPort)
}
Set-Item -Path Env:NGINX_GATEWAY_UPSTREAM -Value "gateway:8080"

Push-Location $dockerDir
try {
    $dockerArgs = @("compose", "-f", "docker-compose.yml", "--profile", "services", "up", "-d", "--pull", "missing", "--force-recreate", "nginx") + $serviceNames
    & docker @dockerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker services deployment failed"
    }
} finally {
    Pop-Location
}

Wait-ServicePorts -Services $services -Root $root
Write-Host "CLUSTER_LINKED_READY"
