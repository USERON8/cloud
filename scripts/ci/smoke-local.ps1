param(
    [int]$HttpTimeoutSeconds = 20
)

$ErrorActionPreference = "Stop"

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

function Assert-ContainerRunning {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ContainerName
    )

    $status = docker inspect --format '{{.State.Status}}' $ContainerName 2>$null
    if ($LASTEXITCODE -ne 0 -or $status.Trim() -ne "running") {
        throw ("SMOKE_FAIL container={0} status={1}" -f $ContainerName, $status)
    }

    Write-Host ("SMOKE_OK container={0} status=running" -f $ContainerName)
}

Write-Host "Smoke: verify backend services"
Test-HttpUp -Name "gateway" -Url "http://127.0.0.1:8080/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "auth-service" -Url "http://127.0.0.1:8081/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "user-service" -Url "http://127.0.0.1:8082/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "order-service" -Url "http://127.0.0.1:8083/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "product-service" -Url "http://127.0.0.1:8084/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "stock-service" -Url "http://127.0.0.1:8085/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "payment-service" -Url "http://127.0.0.1:8086/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds
Test-HttpUp -Name "search-service" -Url "http://127.0.0.1:8087/actuator/health" -TimeoutSeconds $HttpTimeoutSeconds

Write-Host "Smoke: verify docker core containers"
Assert-ContainerRunning -ContainerName "cloud-mysql"
Assert-ContainerRunning -ContainerName "cloud-redis"
Assert-ContainerRunning -ContainerName "cloud-nacos"
Assert-ContainerRunning -ContainerName "cloud-rmq-namesrv"
Assert-ContainerRunning -ContainerName "cloud-rmq-broker"

Write-Host "SMOKE_ALL_OK"
