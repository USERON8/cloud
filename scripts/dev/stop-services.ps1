param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$Services = $null
$DryRun = $false
$ForcePorts = $true

for ($index = 0; $index -lt $CliArgs.Count; $index++) {
    $arg = $CliArgs[$index]
    if ($arg -in @("--dry-run", "-DryRun")) {
        $DryRun = $true
        continue
    }
    if ($arg -eq "--no-force-ports") {
        $ForcePorts = $false
        continue
    }
    if ($arg -eq "--force-ports") {
        $ForcePorts = $true
        continue
    }
    if ($arg -like "--services=*") {
        $Services = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -like "-Services=*") {
        $Services = ($arg -split "=", 2)[1]
        continue
    }
    if ($arg -eq "-Services") {
        if (($index + 1) -ge $CliArgs.Count) {
            throw "Missing value for -Services"
        }
        $index += 1
        $Services = $CliArgs[$index]
    }
}

function Get-ServiceCatalog {
    return @(
        [pscustomobject]@{ name = "gateway"; port = 8080; jar = "services\gateway\target\gateway-1.1.0.jar" },
        [pscustomobject]@{ name = "auth-service"; port = 8081; jar = "services\auth-service\target\auth-service-1.1.0.jar" },
        [pscustomobject]@{ name = "user-service"; port = 8082; jar = "services\user-service\target\user-service-1.1.0.jar" },
        [pscustomobject]@{ name = "order-service"; port = 8083; jar = "services\order-service\target\order-service-1.1.0.jar" },
        [pscustomobject]@{ name = "product-service"; port = 8084; jar = "services\product-service\target\product-service-1.1.0.jar" },
        [pscustomobject]@{ name = "stock-service"; port = 8085; jar = "services\stock-service\target\stock-service-1.1.0.jar" },
        [pscustomobject]@{ name = "payment-service"; port = 8086; jar = "services\payment-service\target\payment-service-1.1.0.jar" },
        [pscustomobject]@{ name = "search-service"; port = 8087; jar = "services\search-service\target\search-service-1.1.0.jar" }
    )
}

function Resolve-SelectedServices {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Catalog,
        [string]$RequestedServices
    )

    if ([string]::IsNullOrWhiteSpace($RequestedServices)) {
        return $Catalog
    }

    $index = @{}
    foreach ($service in $Catalog) {
        $index[$service.name] = $service
    }

    $selected = New-Object System.Collections.Generic.List[object]
    $seen = @{}
    foreach ($name in ($RequestedServices -split "," | ForEach-Object { $_.Trim() })) {
        if ([string]::IsNullOrWhiteSpace($name) -or $seen.ContainsKey($name)) {
            continue
        }
        if (-not $index.ContainsKey($name)) {
            throw ("Unknown service: {0}" -f $name)
        }
        $selected.Add($index[$name])
        $seen[$name] = $true
    }

    if ($selected.Count -eq 0) {
        throw "No services selected. Use --services=name1,name2."
    }

    return $selected
}

function Remove-StateRows {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string[]]$ServiceNames,
        [switch]$HasHeader
    )

    if (-not (Test-Path $Path)) {
        return
    }

    $selected = @{}
    foreach ($serviceName in $ServiceNames) {
        $selected[$serviceName] = $true
    }

    $lines = Get-Content $Path
    $filtered = New-Object System.Collections.Generic.List[string]
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        if ($HasHeader -and $line -eq "service,port,pid,status,startup_seconds,out_log,err_log") {
            $filtered.Add($line)
            continue
        }
        $serviceName = ($line -split ",", 2)[0]
        if (-not $selected.ContainsKey($serviceName)) {
            $filtered.Add($line)
        }
    }

    Set-Content -Path $Path -Value $filtered -Encoding UTF8
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $PSScriptRoot "lib\port-guard.ps1")

$catalog = Get-ServiceCatalog
$services = Resolve-SelectedServices -Catalog $catalog -RequestedServices $Services
$serviceNames = $services | ForEach-Object { $_.name }
Write-Host ("SERVICE_SCOPE services={0}" -f ($serviceNames -join ","))

$pidsPath = Join-Path $root ".tmp\acceptance\pids.txt"
$pidIndex = @{}
if (Test-Path $pidsPath) {
    foreach ($line in Get-Content $pidsPath) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $parts = $line -split ",", 4
        if ($parts.Count -ge 3) {
            $pidIndex[$parts[0]] = [long]$parts[2]
        }
    }
}

foreach ($service in $services) {
    $stopped = $false
    if ($pidIndex.ContainsKey($service.name)) {
        $servicePid = $pidIndex[$service.name]
        if ($DryRun) {
            Write-Host ("SERVICE_STOP service={0} port={1} pid={2} source=pid-file action=dry-run" -f $service.name, $service.port, $servicePid)
            $stopped = $true
        } else {
            try {
                $process = Get-Process -Id $servicePid -ErrorAction Stop
                Stop-Process -Id $servicePid -Force -ErrorAction Stop
                Write-Host ("SERVICE_STOP service={0} port={1} pid={2} source=pid-file action=stopped process={3}" -f $service.name, $service.port, $servicePid, $process.ProcessName)
                $stopped = $true
            } catch {
                Write-Host ("SERVICE_STOP service={0} port={1} pid={2} source=pid-file action=skip reason={3}" -f $service.name, $service.port, $servicePid, $_.Exception.Message)
            }
        }
    }

    if ($ForcePorts) {
        Kill-PortOwner -Port $service.port -DryRun:$DryRun
    } elseif (-not $stopped) {
        Write-Host ("SERVICE_STOP service={0} port={1} pid=- action=skip reason=no-pid-file" -f $service.name, $service.port)
    }
}

if (-not $DryRun) {
    Remove-StateRows -Path (Join-Path $root ".tmp\acceptance\pids.txt") -ServiceNames $serviceNames
    Remove-StateRows -Path (Join-Path $root ".tmp\acceptance\startup.csv") -ServiceNames $serviceNames -HasHeader
}

Write-Host "STOP_OK"
