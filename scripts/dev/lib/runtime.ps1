$ErrorActionPreference = "Stop"

function Get-DockerEnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$DefaultValue = ""
    )

    $envPath = Join-Path $Root "docker\.env"
    if (-not (Test-Path $envPath)) {
        return $DefaultValue
    }

    $match = Get-Content $envPath |
        Where-Object { $_ -match ("^{0}=(.*)$" -f [Regex]::Escape($Name)) } |
        Select-Object -First 1
    if ($null -eq $match) {
        return $DefaultValue
    }

    return ($match -split "=", 2)[1]
}

function Get-DockerPortValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [int]$DefaultValue
    )

    $rawValue = Get-DockerEnvValue -Root $Root -Name $Name -DefaultValue ([string]$DefaultValue)
    $parsed = 0
    if ([int]::TryParse($rawValue, [ref]$parsed)) {
        return $parsed
    }
    return $DefaultValue
}

function Wait-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TargetHost,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 90,
        [int]$SleepMilliseconds = 1000
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $task = $client.ConnectAsync($TargetHost, $Port)
            if ($task.Wait([Math]::Min($SleepMilliseconds, 1000)) -and $client.Connected) {
                return $true
            }
        } catch {
        } finally {
            $client.Dispose()
        }
        Start-Sleep -Milliseconds $SleepMilliseconds
    }
    return $false
}

function Open-LocalUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    try {
        Start-Process $Url | Out-Null
    } catch {
        Write-Host ("OPEN_URL url={0} status=failed reason={1}" -f $Url, $_.Exception.Message)
    }
}

function Set-ServiceRuntimeEnvironment {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    $nacosPort = Get-DockerPortValue -Root $Root -Name "PORT_NACOS_HTTP" -DefaultValue 18848
    $rocketMqNamesrvPort = Get-DockerPortValue -Root $Root -Name "PORT_RMQ_NAMESRV" -DefaultValue 19876
    $seataPort = Get-DockerPortValue -Root $Root -Name "PORT_SEATA_SERVER" -DefaultValue 18091

    $env:NACOS_HOST = "127.0.0.1"
    $env:NACOS_PORT = [string]$nacosPort
    $env:NACOS_SERVER_ADDR = "127.0.0.1:$nacosPort"

    $env:ROCKETMQ_NAMESRV_HOST = "127.0.0.1"
    $env:ROCKETMQ_NAMESRV_PORT = [string]$rocketMqNamesrvPort
    $env:ROCKETMQ_NAME_SERVER = "127.0.0.1:$rocketMqNamesrvPort"

    $env:SEATA_SERVER_ADDR = "127.0.0.1:$seataPort"
    $env:SEATA_REGISTRY_TYPE = "file"

    Write-Host ("SERVICE_ENV nacos={0} rocketmq={1} seata={2}" -f $env:NACOS_SERVER_ADDR, $env:ROCKETMQ_NAME_SERVER, $env:SEATA_SERVER_ADDR)
}
