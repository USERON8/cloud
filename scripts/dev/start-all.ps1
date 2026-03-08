param(
    [switch]$WithMonitoring
)

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "=== START CONTAINERS ==="

& "$root\scripts\infra\start-containers.ps1" `
    $(if ($WithMonitoring) { "--with-monitoring" })

if ($LASTEXITCODE -ne 0) {
    throw "Container startup failed"
}

Write-Host "=== START MICROSERVICES ==="

& "$root\scripts\infra\start-services.ps1"

if ($LASTEXITCODE -ne 0) {
    throw "Service startup failed"
}

Write-Host "=== SYSTEM STARTED SUCCESSFULLY ==="
