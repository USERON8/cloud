param(
    [int]$HttpTimeoutSeconds = 20
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
}

function Get-EnvValueFromFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$DefaultValue = ""
    )

    if (-not (Test-Path $Path)) {
        return $DefaultValue
    }

    $match = Get-Content $Path |
        Where-Object { $_ -match ("^{0}=(.*)$" -f [Regex]::Escape($Name)) } |
        Select-Object -First 1
    if ($null -eq $match) {
        return $DefaultValue
    }

    return ($match -split "=", 2)[1]
}

function Get-NginxHttpPort {
    $direct = [string]$env:PORT_NGINX_HTTP
    $parsed = 0
    if ([int]::TryParse($direct, [ref]$parsed)) {
        return $parsed
    }

    $repoRoot = Get-RepoRoot
    $fromFile = Get-EnvValueFromFile -Path (Join-Path $repoRoot ".env") -Name "PORT_NGINX_HTTP" -DefaultValue "18080"
    if ([int]::TryParse([string]$fromFile, [ref]$parsed)) {
        return $parsed
    }

    return 18080
}

function Get-HealthStatus {
    param($ResponseObject)

    if ($null -eq $ResponseObject) { return $null }
    if ($ResponseObject.PSObject.Properties.Name -contains "status") { return $ResponseObject.status }
    if ($ResponseObject.PSObject.Properties.Name -contains "data" -and $null -ne $ResponseObject.data) {
        if ($ResponseObject.data.PSObject.Properties.Name -contains "status") { return $ResponseObject.data.status }
    }

    return $null
}

function Test-HttpUp {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 10
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $request = [System.Net.HttpWebRequest]::Create($Url)
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
                }
            }

            if ($null -eq $response) {
                Start-Sleep -Seconds 1
                continue
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
                        Write-Host ("SMOKE_OK {0} {1} status=UP" -f $Name, $Url)
                        return
                    }
                } catch {
                }

                if (
                    (($responsePath) -and ($responsePath -ne "/actuator/health")) -or
                    ($content -match '<title>\s*Please sign in\s*</title>') -or
                    ($content -match 'action="/login"')
                ) {
                    Write-Host ("SMOKE_OK {0} {1} status=UP_SECURED http={2}" -f $Name, $Url, $statusCode)
                    return
                }
            }

            if ($statusCode -in @(301, 302, 303, 307, 308, 401, 403)) {
                Write-Host ("SMOKE_OK {0} {1} status=UP_SECURED http={2}" -f $Name, $Url, $statusCode)
                return
            }
        } finally {
            if ($null -ne $response) {
                $response.Dispose()
            }
        }

        Start-Sleep -Seconds 1
    }

    throw ("SMOKE_FAIL {0} {1}" -f $Name, $Url)
}

function Test-ContainerRunning {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$ContainerNames
    )

    foreach ($containerName in $ContainerNames) {
        try {
            $status = docker inspect --format '{{.State.Status}}' $containerName 2>$null
        } catch {
            continue
        }
        if ($LASTEXITCODE -eq 0 -and $status.Trim() -eq "running") {
            return $true
        }
    }

    return $false
}

function Assert-ContainerRunning {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$ContainerNames
    )

    foreach ($containerName in $ContainerNames) {
        $status = docker inspect --format '{{.State.Status}}' $containerName 2>$null
        if ($LASTEXITCODE -eq 0 -and $status.Trim() -eq "running") {
            Write-Host ("SMOKE_OK container={0} status=running" -f $containerName)
            return
        }
    }

    throw ("SMOKE_FAIL containers={0}" -f ($ContainerNames -join ","))
}

Write-Host "Smoke: verify backend services"
$publicEntryPort = Get-NginxHttpPort
if (Test-ContainerRunning -ContainerNames @("nginx", "cloud-nginx-gateway")) {
    Test-HttpUp -Name "public-entry" -Url ("http://127.0.0.1:{0}/actuator/health" -f $publicEntryPort) -TimeoutSeconds $HttpTimeoutSeconds
} else {
    Write-Host ("SMOKE_SKIP public-entry http://127.0.0.1:{0}/actuator/health container=nginx-not-running" -f $publicEntryPort)
}
Test-HttpUp -Name "gateway" -Url "http://127.0.0.1:8080/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "auth-service" -Url "http://127.0.0.1:8081/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "user-service" -Url "http://127.0.0.1:8082/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "order-service" -Url "http://127.0.0.1:8083/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "product-service" -Url "http://127.0.0.1:8084/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "stock-service" -Url "http://127.0.0.1:8085/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "payment-service" -Url "http://127.0.0.1:8086/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "search-service" -Url "http://127.0.0.1:8087/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds

Write-Host "Smoke: verify docker core containers"
Assert-ContainerRunning -ContainerNames @("mysql", "cloud-mysql")
Assert-ContainerRunning -ContainerNames @("redis", "cloud-redis")
Assert-ContainerRunning -ContainerNames @("nacos", "cloud-nacos")
Assert-ContainerRunning -ContainerNames @("rmq-namesrv", "cloud-rmq-namesrv")
Assert-ContainerRunning -ContainerNames @("rmq-broker", "cloud-rmq-broker")

Write-Host "SMOKE_ALL_OK"
