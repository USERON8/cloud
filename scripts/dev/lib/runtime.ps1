$ErrorActionPreference = "Stop"

function Read-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $values = [ordered]@{}
    if (-not (Test-Path $Path)) {
        return $values
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }
        $values[$parts[0].Trim()] = $parts[1]
    }

    return $values
}

function Write-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [System.Collections.IDictionary]$Values
    )

    $content = foreach ($key in $Values.Keys) {
        "{0}={1}" -f $key, $Values[$key]
    }
    Set-Content -Path $Path -Value $content -Encoding UTF8
}

function Set-EnvValueIfMissing {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Values,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$Value = ""
    )

    if (-not $Values.Contains($Name) -or [string]::IsNullOrWhiteSpace([string]$Values[$Name])) {
        $Values[$Name] = $Value
    }
}

function Set-EnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [System.Collections.IDictionary]$Values,
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [string]$Value = ""
    )

    $Values[$Name] = $Value
}

function Get-PreferredLocalIpv4 {
    try {
        $defaultRoute = Get-NetRoute -DestinationPrefix "0.0.0.0/0" -AddressFamily IPv4 -ErrorAction Stop |
            Where-Object { $_.NextHop -and $_.NextHop -ne "0.0.0.0" } |
            Sort-Object RouteMetric, ifMetric |
            Select-Object -First 1
        if ($null -ne $defaultRoute) {
            $ipAddress = Get-NetIPAddress -InterfaceIndex $defaultRoute.InterfaceIndex -AddressFamily IPv4 -ErrorAction Stop |
                Where-Object {
                    $_.IPAddress -and
                    $_.IPAddress -notlike "127.*" -and
                    $_.IPAddress -notlike "169.254.*"
                } |
                Select-Object -First 1
            if ($null -ne $ipAddress) {
                return [string]$ipAddress.IPAddress
            }
        }
    } catch {
    }

    return ""
}

function Resolve-PublicBaseUrl {
    param(
        [string]$Url
    )

    if ([string]::IsNullOrWhiteSpace($Url)) {
        return ""
    }

    $trimmedUrl = $Url.Trim()
    try {
        $uri = [System.Uri]$trimmedUrl
        if ($uri.IsAbsoluteUri) {
            return $uri.GetLeftPart([System.UriPartial]::Authority).TrimEnd("/")
        }
    } catch {
    }

    return $trimmedUrl.TrimEnd("/")
}

function Import-EnvMapFromObject {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Values
    )

    foreach ($property in $Values.PSObject.Properties) {
        Set-Item -Path ("Env:{0}" -f $property.Name) -Value ([string]$property.Value)
    }
}

function Sync-EnvironmentFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [switch]$ImportProcessEnvironment
    )

    $nodeCommand = Get-Command node -ErrorAction SilentlyContinue
    if ($null -eq $nodeCommand) {
        throw "node is required to synchronize development environment files"
    }

    $scriptPath = Join-Path $Root "scripts\dev\lib\runtime-sync.mjs"
    $preferredLocalIpv4 = Get-PreferredLocalIpv4
    $arguments = @($scriptPath, $Root)
    if (-not [string]::IsNullOrWhiteSpace($preferredLocalIpv4)) {
        $arguments += "--preferred-local-ipv4=$preferredLocalIpv4"
    }

    $json = & $nodeCommand.Source @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "failed to synchronize development environment files"
    }

    $syncResult = $json | ConvertFrom-Json
    if ($ImportProcessEnvironment) {
        Import-EnvMapFromObject -Values $syncResult.dockerEnv
        Import-EnvMapFromObject -Values $syncResult.frontendEnv
    }

    Write-Host ("ENV_SYNC root={0} docker={1} frontend={2}" -f $syncResult.rootEnvPath, $syncResult.dockerEnvPath, $syncResult.frontendDevEnvPath)
    return $syncResult.dockerEnv
}

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

    $repoEnv = Sync-EnvironmentFiles -Root $Root -ImportProcessEnvironment

    $nacosPort = Get-DockerPortValue -Root $Root -Name "PORT_NACOS_HTTP" -DefaultValue 18848
    $nacosGrpcPort = Get-DockerPortValue -Root $Root -Name "PORT_NACOS_GRPC" -DefaultValue ($nacosPort + 1000)
    $nacosGrpcOffset = $nacosGrpcPort - $nacosPort
    $redisPort = Get-DockerPortValue -Root $Root -Name "PORT_REDIS" -DefaultValue 16379
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

    $env:REDIS_HOST = "127.0.0.1"
    $env:REDIS_PORT = [string]$redisPort
    $env:SPRING_DATA_REDIS_HOST = $env:REDIS_HOST
    $env:SPRING_DATA_REDIS_PORT = $env:REDIS_PORT
    $env:SPRING_REDIS_HOST = $env:REDIS_HOST
    $env:SPRING_REDIS_PORT = $env:REDIS_PORT

    $env:ROCKETMQ_NAMESRV_HOST = "127.0.0.1"
    $env:ROCKETMQ_NAMESRV_PORT = [string]$rocketMqNamesrvPort
    $env:ROCKETMQ_NAME_SERVER = "127.0.0.1:$rocketMqNamesrvPort"
    $env:DUBBO_REGISTRY_ADDRESS = "nacos://$nacosServerAddr"
    $env:DUBBO_REGISTRY_USERNAME = $nacosUsername
    $env:DUBBO_REGISTRY_PASSWORD = $nacosPassword
    $env:DUBBO_REGISTRY_PARAMETERS_NAMESPACE = $nacosNamespace
    $env:DUBBO_REGISTRY_PARAMETERS_GROUP = "DUBBO_GROUP"
    $env:GATEWAY_ROUTE_AUTH_URI = "http://127.0.0.1:8081"
    $env:GATEWAY_ROUTE_USER_URI = "http://127.0.0.1:8082"
    $env:GATEWAY_ROUTE_ORDER_URI = "http://127.0.0.1:8083"
    $env:GATEWAY_ROUTE_PRODUCT_URI = "http://127.0.0.1:8084"
    $env:GATEWAY_ROUTE_STOCK_URI = "http://127.0.0.1:8085"
    $env:GATEWAY_ROUTE_PAYMENT_URI = "http://127.0.0.1:8086"
    $env:GATEWAY_ROUTE_SEARCH_URI = "http://127.0.0.1:8087"
    $env:GATEWAY_ROUTE_GOVERNANCE_URI = "http://127.0.0.1:8088"
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
