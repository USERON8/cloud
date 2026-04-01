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
    $nacosGrpcPort = Get-DockerPortValue -Root $Root -Name "PORT_NACOS_GRPC" -DefaultValue ($nacosPort + 1000)
    $nacosGrpcOffset = $nacosGrpcPort - $nacosPort
    $rocketMqNamesrvPort = Get-DockerPortValue -Root $Root -Name "PORT_RMQ_NAMESRV" -DefaultValue 20011
    $minioPort = Get-DockerPortValue -Root $Root -Name "PORT_MINIO_API" -DefaultValue 19000

    $nacosServerAddr = "127.0.0.1:$nacosPort"
    $nacosUsername = if ([string]::IsNullOrWhiteSpace($env:NACOS_USERNAME)) { "nacos" } else { $env:NACOS_USERNAME }
    $nacosPassword = if ([string]::IsNullOrWhiteSpace($env:NACOS_PASSWORD)) { "nacos" } else { $env:NACOS_PASSWORD }
    $nacosNamespace = if ([string]::IsNullOrWhiteSpace($env:NACOS_NAMESPACE)) { "public" } else { $env:NACOS_NAMESPACE }
    $nacosGroup = if ([string]::IsNullOrWhiteSpace($env:NACOS_GROUP)) { "DEFAULT_GROUP" } else { $env:NACOS_GROUP }

    $env:NACOS_HOST = "127.0.0.1"
    $env:NACOS_PORT = [string]$nacosPort
    $env:NACOS_SERVER_ADDR = $nacosServerAddr
    $env:NACOS_CONFIG_SERVER_ADDR = $nacosServerAddr
    $env:NACOS_DISCOVERY_SERVER_ADDR = $nacosServerAddr
    $env:NACOS_USERNAME = $nacosUsername
    $env:NACOS_PASSWORD = $nacosPassword
    $env:NACOS_NAMESPACE = $nacosNamespace
    $env:NACOS_GROUP = $nacosGroup
    $env:NACOS_CONFIG_NAMESPACE = $nacosNamespace
    $env:NACOS_CONFIG_GROUP = $nacosGroup
    $env:NACOS_DISCOVERY_NAMESPACE = $nacosNamespace
    $env:NACOS_DISCOVERY_GROUP = $nacosGroup
    $env:SPRING_CLOUD_NACOS_SERVER_ADDR = $nacosServerAddr
    $env:SPRING_CLOUD_NACOS_USERNAME = $nacosUsername
    $env:SPRING_CLOUD_NACOS_PASSWORD = $nacosPassword
    $env:SPRING_CLOUD_NACOS_NAMESPACE = $nacosNamespace
    $env:SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR = $nacosServerAddr
    $env:SPRING_CLOUD_NACOS_CONFIG_USERNAME = $nacosUsername
    $env:SPRING_CLOUD_NACOS_CONFIG_PASSWORD = $nacosPassword
    $env:SPRING_CLOUD_NACOS_CONFIG_NAMESPACE = $nacosNamespace
    $env:SPRING_CLOUD_NACOS_CONFIG_GROUP = $nacosGroup
    $env:SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION = "yaml"
    $env:SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR = $nacosServerAddr
    $env:SPRING_CLOUD_NACOS_DISCOVERY_USERNAME = $nacosUsername
    $env:SPRING_CLOUD_NACOS_DISCOVERY_PASSWORD = $nacosPassword
    $env:SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE = $nacosNamespace
    $env:SPRING_CLOUD_NACOS_DISCOVERY_GROUP = $nacosGroup
    if ($nacosGrpcOffset -gt 0) {
        $env:NACOS_GRPC_PORT_OFFSET = [string]$nacosGrpcOffset
    }

    $env:ROCKETMQ_NAMESRV_HOST = "127.0.0.1"
    $env:ROCKETMQ_NAMESRV_PORT = [string]$rocketMqNamesrvPort
    $env:ROCKETMQ_NAME_SERVER = "127.0.0.1:$rocketMqNamesrvPort"
    $env:DUBBO_REGISTRY_ADDRESS = "nacos://$nacosServerAddr"
    $env:DUBBO_REGISTRY_USERNAME = $nacosUsername
    $env:DUBBO_REGISTRY_PASSWORD = $nacosPassword
    $env:DUBBO_REGISTRY_PARAMETERS_NAMESPACE = $nacosNamespace
    $env:DUBBO_REGISTRY_PARAMETERS_GROUP = "DUBBO_GROUP"
    if ([string]::IsNullOrWhiteSpace($env:DUBBO_APPLICATION_QOS_ENABLE)) {
        $env:DUBBO_APPLICATION_QOS_ENABLE = "false"
    }

    if ([string]::IsNullOrWhiteSpace($env:XXL_JOB_ENABLED)) {
        $env:XXL_JOB_ENABLED = "false"
    }
    if ([string]::IsNullOrWhiteSpace($env:SKYWALKING_ENABLED)) {
        $env:SKYWALKING_ENABLED = "false"
    }
    if ([string]::IsNullOrWhiteSpace($env:MINIO_ENDPOINT)) {
        $env:MINIO_ENDPOINT = "http://127.0.0.1:$minioPort"
    }
    if ([string]::IsNullOrWhiteSpace($env:MINIO_PUBLIC_ENDPOINT)) {
        $env:MINIO_PUBLIC_ENDPOINT = $env:MINIO_ENDPOINT
    }
    if ([string]::IsNullOrWhiteSpace($env:MINIO_ACCESS_KEY)) {
        $env:MINIO_ACCESS_KEY = "minioadmin"
    }
    if ([string]::IsNullOrWhiteSpace($env:MINIO_SECRET_KEY)) {
        $env:MINIO_SECRET_KEY = "minioadmin"
    }
    if ([string]::IsNullOrWhiteSpace($env:GATEWAY_SIGNATURE_SECRET)) {
        $env:GATEWAY_SIGNATURE_SECRET = "cloud-gateway-signature-dev"
    }
    if ([string]::IsNullOrWhiteSpace($env:GATEWAY_INTERNAL_IDENTITY_SECRET)) {
        $env:GATEWAY_INTERNAL_IDENTITY_SECRET = $env:GATEWAY_SIGNATURE_SECRET
    }
    if ([string]::IsNullOrWhiteSpace($env:CLIENT_SERVICE_SECRET)) {
        $env:CLIENT_SERVICE_SECRET = "cloud-client-service-secret-dev"
    }
    if ([string]::IsNullOrWhiteSpace($env:APP_OAUTH2_SERVICE_CLIENT_SECRET)) {
        $env:APP_OAUTH2_SERVICE_CLIENT_SECRET = $env:CLIENT_SERVICE_SECRET
    }
    if ([string]::IsNullOrWhiteSpace($env:APP_OAUTH2_INTERNAL_CLIENT_SECRET)) {
        $env:APP_OAUTH2_INTERNAL_CLIENT_SECRET = "cloud-internal-client-secret-dev"
    }
    if ([string]::IsNullOrWhiteSpace($env:APP_JWT_ALLOW_GENERATED_KEYPAIR)) {
        $env:APP_JWT_ALLOW_GENERATED_KEYPAIR = "true"
    }
    if ([string]::IsNullOrWhiteSpace($env:GITHUB_CLIENT_ID)) {
        $env:GITHUB_CLIENT_ID = "cloud-github-client-dev"
    }
    if ([string]::IsNullOrWhiteSpace($env:GITHUB_CLIENT_SECRET)) {
        $env:GITHUB_CLIENT_SECRET = "cloud-github-secret-dev"
    }

    $seataAutoConfigExcludes = @(
        "org.apache.seata.spring.boot.autoconfigure.SeataAutoConfiguration",
        "org.apache.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration"
    )
    $existingAutoConfigExcludes = @()
    if (-not [string]::IsNullOrWhiteSpace($env:SPRING_AUTOCONFIGURE_EXCLUDE)) {
        $existingAutoConfigExcludes = $env:SPRING_AUTOCONFIGURE_EXCLUDE.Split(",") |
            ForEach-Object { $_.Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    }
    foreach ($exclude in $seataAutoConfigExcludes) {
        if ($existingAutoConfigExcludes -notcontains $exclude) {
            $existingAutoConfigExcludes += $exclude
        }
    }
    $env:SPRING_AUTOCONFIGURE_EXCLUDE = ($existingAutoConfigExcludes -join ",")

    Write-Host ("SERVICE_ENV nacos={0} rocketmq={1} gatewaySignature=configured authSecrets=configured" -f $env:NACOS_SERVER_ADDR, $env:ROCKETMQ_NAME_SERVER)
}
