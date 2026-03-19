param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

function Resolve-ServiceCatalog {
    return @(
        [pscustomobject]@{ name = "gateway"; hostPortVar = "PORT_GATEWAY_SERVICE_HOST"; hostPort = 28080 },
        [pscustomobject]@{ name = "auth-service"; hostPortVar = "PORT_AUTH_SERVICE_HOST"; hostPort = 28081 },
        [pscustomobject]@{ name = "user-service"; hostPortVar = "PORT_USER_SERVICE_HOST"; hostPort = 28082 },
        [pscustomobject]@{ name = "order-service"; hostPortVar = "PORT_ORDER_SERVICE_HOST"; hostPort = 28083 },
        [pscustomobject]@{ name = "product-service"; hostPortVar = "PORT_PRODUCT_SERVICE_HOST"; hostPort = 28084 },
        [pscustomobject]@{ name = "stock-service"; hostPortVar = "PORT_STOCK_SERVICE_HOST"; hostPort = 28085 },
        [pscustomobject]@{ name = "payment-service"; hostPortVar = "PORT_PAYMENT_SERVICE_HOST"; hostPort = 28086 },
        [pscustomobject]@{ name = "search-service"; hostPortVar = "PORT_SEARCH_SERVICE_HOST"; hostPort = 28087 }
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
        $selected.Insert(0, $index["gateway"])
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

function Wait-ServicePorts {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Services,
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    . (Join-Path $PSScriptRoot "lib\runtime.ps1")

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
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            $content = $reader.ReadToEnd()
            $reader.Dispose()

            if ($statusCode -eq 200 -and $content -match '"status"\s*:\s*"UP"') {
                return "UP"
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
        $deadline = (Get-Date).AddSeconds(120)
        $healthy = $null
        while ((Get-Date) -lt $deadline) {
            $healthy = Get-ServiceHealthState -Port $service.hostPort
            if ($healthy -in @("UP", "UP_SECURED")) {
                break
            }
            Start-Sleep -Seconds 2
        }
        if ($healthy -notin @("UP", "UP_SECURED")) {
            throw ("cluster service health not ready: {0}:{1}" -f $service.name, $service.hostPort)
        }
        Write-Host ("WAIT_CLUSTER service={0} port={1} status={2}" -f $service.name, $service.hostPort, $healthy)
    }

    $gatewayPort = ($Services | Where-Object { $_.name -eq "gateway" } | Select-Object -First 1).hostPort
    if (-not (Wait-TcpPort -TargetHost "127.0.0.1" -Port (Get-DockerPortValue -Root $Root -Name "PORT_NGINX_HTTP" -DefaultValue 18080) -TimeoutSeconds 60 -SleepMilliseconds 1000)) {
        throw "nginx http port not ready after cluster deployment"
    }
    Write-Host ("CLUSTER_GATEWAY hostPort={0}" -f $gatewayPort)
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$catalog = Resolve-ServiceCatalog
$requestedServices = $null
foreach ($arg in $CliArgs) {
    if ($arg -like "--services=*") {
        $requestedServices = ($arg -split "=", 2)[1]
    }
}
$services = Resolve-SelectedServices -Catalog $catalog -RequestedServices $requestedServices

Assert-HostAcceptance -Root $root -Services $services

$dockerDir = Join-Path $root "docker"
$serviceNames = $services | ForEach-Object { $_.name }

foreach ($service in $services) {
    Set-Item -Path ("Env:{0}" -f $service.hostPortVar) -Value ([string]$service.hostPort)
}
Set-Item -Path Env:NGINX_GATEWAY_UPSTREAM -Value "gateway:8080"

Push-Location $dockerDir
try {
    $dockerArgs = @("compose", "-f", "docker-compose.yml", "--profile", "services", "up", "-d", "--pull", "never", "--force-recreate", "nginx") + $serviceNames
    & docker @dockerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker services deployment failed"
    }
} finally {
    Pop-Location
}

Wait-ServicePorts -Services $services -Root $root
Write-Host "CLUSTER_LINKED_READY"
