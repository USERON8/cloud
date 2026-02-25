Set-StrictMode -Version Latest

function Test-TcpPortOpen {
    param(
        [Parameter(Mandatory = $true)][string]$Host,
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutMs = 1500
    )
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $async = $client.BeginConnect($Host, $Port, $null, $null)
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
        "search" { return @("cloud-nginx-gateway", "cloud-redis", "cloud-es-search", "cloud-prometheus") }
        default { return @("cloud-mysql", "cloud-redis", "cloud-nacos", "cloud-rmq-namesrv", "cloud-rmq-broker", "cloud-nginx-gateway", "cloud-es-search", "cloud-prometheus") }
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
    foreach ($container in $requiredContainers) {
        if ($runningContainers -notcontains $container) {
            $missingContainers += $container
        }
    }
    if ($missingContainers.Count -gt 0) {
        throw ("[k6-preflight] required containers are not running: {0}" -f ($missingContainers -join ", "))
    }

    $closedPorts = @()
    foreach ($port in $requiredPorts) {
        if (-not (Test-TcpPortOpen -Host "127.0.0.1" -Port $port)) {
            $closedPorts += $port
        }
    }
    if ($closedPorts.Count -gt 0) {
        throw ("[k6-preflight] required service ports are not reachable: {0}" -f ($closedPorts -join ", "))
    }

    try {
        $resp = Invoke-RestMethod -Method GET -Uri $GatewayHealthUrl -TimeoutSec 5
        $status = if ($resp.PSObject.Properties.Name -contains "status") { $resp.status } else { $null }
        if ($status -ne "UP") {
            throw ("gateway health status is not UP: {0}" -f $status)
        }
    } catch {
        throw ("[k6-preflight] gateway health check failed: {0}" -f $_.Exception.Message)
    }

    Write-Host ("[k6-preflight] passed mode={0}" -f $Mode)
}
