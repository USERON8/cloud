param(
    [switch]$WithMonitoring
)

$ErrorActionPreference="Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host ""
Write-Host "==============================="
Write-Host " START MICROSERVICE PLATFORM "
Write-Host "==============================="
Write-Host ""

# 1 启动 Docker 中间件
Write-Host "STEP 1: Start Containers"

& "$root\scripts\infra\start-containers.ps1" `
    $(if ($WithMonitoring) { "--with-monitoring" })

if ($LASTEXITCODE -ne 0) {
    throw "Container startup failed"
}

Write-Host "Containers started"
Write-Host ""

# 2 等待中间件 Ready
Write-Host "STEP 2: Waiting Infrastructure Ready"

function Wait-Port {
    param($port)

    while ($true) {
        $conn = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -WarningAction SilentlyContinue
        if ($conn.TcpTestSucceeded) {
            Write-Host "Port $port ready"
            break
        }
        Start-Sleep 2
    }
}

Wait-Port 3306   # MySQL
Wait-Port 6379   # Redis
Wait-Port 8848   # Nacos
Wait-Port 9876   # RocketMQ

Write-Host "Infrastructure ready"
Write-Host ""

# 3 启动 SkyWalking
Write-Host "STEP 3: Start SkyWalking"

$env:SKYWALKING_AGENT_PATH="$root\infra\skywalking\agent\skywalking-agent.jar"
$env:SKYWALKING_COLLECTOR_BACKEND_SERVICE="127.0.0.1:11800"

Write-Host "SkyWalking Agent Enabled"

Write-Host ""

# 4 启动微服务
Write-Host "STEP 4: Start Microservices"

& "$root\scripts\infra\start-services.ps1"

if ($LASTEXITCODE -ne 0) {
    throw "Service startup failed"
}

Write-Host "Microservices started"
Write-Host ""

# 5 打开监控页面
Write-Host "STEP 5: Open Dashboards"

Start-Process "http://localhost:8080"   # SkyWalking
Start-Process "http://localhost:3000"   # Grafana
Start-Process "http://localhost:5601"   # Kibana
Start-Process "http://localhost:8848"   # Nacos

Write-Host ""
Write-Host "==============================="
Write-Host " PLATFORM STARTED SUCCESSFULLY "
Write-Host "==============================="
