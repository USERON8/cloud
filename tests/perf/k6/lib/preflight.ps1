Set-StrictMode -Version Latest

function Test-TcpPortOpen {
    param(
        [Parameter(Mandatory = $true)][string]$HostName,
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutMs = 1500
    )
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            $client.Close()
            return $false
        }
        $client.EndConnect($async)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Get-RequiredContainers {
    param([string]$Mode)
    switch ($Mode) {
        "search" {
            return @(
                @("nginx", "cloud-nginx-gateway"),
                @("redis", "cloud-redis"),
                @("es-search", "cloud-es-search"),
                @("cloud-prometheus")
            )
        }
        default {
            return @(
                @("mysql", "cloud-mysql"),
                @("redis", "cloud-redis"),
                @("nacos", "cloud-nacos"),
                @("rmq-namesrv", "cloud-rmq-namesrv"),
                @("rmq-broker", "cloud-rmq-broker"),
                @("nginx", "cloud-nginx-gateway"),
                @("es-search", "cloud-es-search"),
                @("cloud-prometheus")
            )
        }
    }
}

function Get-RequiredServicePorts {
    param([string]$Mode)
    switch ($Mode) {
        "search" { return @(8080, 8084, 8087) }
        default { return @(8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087) }
    }
}

function Assert-K6Preflight {
    param(
        [Parameter(Mandatory = $true)][string]$Mode,
        [string]$GatewayHealthUrl = "http://127.0.0.1:8080/actuator/health"
    )

    if ($env:K6_SKIP_PREFLIGHT -eq "1") {
        Write-Host "[k6-preflight] skipped by K6_SKIP_PREFLIGHT=1"
        return
    }

    $requiredContainers = Get-RequiredContainers -Mode $Mode
    $requiredPorts = Get-RequiredServicePorts -Mode $Mode

    $runningContainers = @(docker ps --format "{{.Names}}")
    $missingContainers = @()
    foreach ($containerAliases in $requiredContainers) {
        if (@($containerAliases | Where-Object { $runningContainers -contains $_ }).Count -eq 0) {
            $missingContainers += ($containerAliases -join "|")
        }
    }
    if ($missingContainers.Count -gt 0) {
        throw ("[k6-preflight] required containers are not running: {0}" -f ($missingContainers -join ", "))
    }

    $closedPorts = @()
    foreach ($port in $requiredPorts) {
        if (-not (Test-TcpPortOpen -HostName "127.0.0.1" -Port $port)) {
            $closedPorts += $port
        }
    }
    if ($closedPorts.Count -gt 0) {
        throw ("[k6-preflight] required service ports are not reachable: {0}" -f ($closedPorts -join ", "))
    }

    try {
        $response = Invoke-WebRequest -Method GET -Uri $GatewayHealthUrl -TimeoutSec 5 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            $payload = $null
            try {
                $payload = $response.Content | ConvertFrom-Json
            } catch {
                $payload = $null
            }
            if ($null -ne $payload -and $payload.PSObject.Properties.Name -contains "status" -and $payload.status -ne "UP") {
                throw ("gateway health status is not UP: {0}" -f $payload.status)
            }
        }
    } catch [System.Net.WebException] {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "[k6-preflight] gateway health endpoint is protected (401), continue preflight"
        } else {
            throw ("[k6-preflight] gateway health check failed: {0}" -f $_.Exception.Message)
        }
    } catch {
        throw ("[k6-preflight] gateway health check failed: {0}" -f $_.Exception.Message)
    }

    Write-Host ("[k6-preflight] passed mode={0}" -f $Mode)
}
