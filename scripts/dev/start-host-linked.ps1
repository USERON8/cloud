param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

function Test-LogHasCriticalError {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        return $false
    }

    $patterns = @(
        "APPLICATION FAILED TO START",
        "Exception in thread",
        "Caused by:",
        "^\s*ERROR\b",
        "\bOutOfMemoryError\b"
    )

    foreach ($line in Get-Content $Path) {
        foreach ($pattern in $patterns) {
            if ($line -match $pattern) {
                return $true
            }
        }
    }

    return $false
}

function Assert-HostAcceptance {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [string]$RequestedServices
    )

    $startupPath = Join-Path $Root ".tmp\acceptance\startup.csv"
    if (-not (Test-Path $startupPath)) {
        throw "startup acceptance file missing: $startupPath"
    }

    $rows = Import-Csv $startupPath
    if ([string]::IsNullOrWhiteSpace($RequestedServices)) {
        $selectedRows = $rows
    } else {
        $selected = @{}
        foreach ($name in ($RequestedServices -split "," | ForEach-Object { $_.Trim() })) {
            if (-not [string]::IsNullOrWhiteSpace($name)) {
                $selected[$name] = $true
            }
        }
        $selectedRows = $rows | Where-Object { $selected.ContainsKey($_.service) }
    }

    if ($selectedRows.Count -eq 0) {
        throw "no accepted services found in $startupPath"
    }

    foreach ($row in $selectedRows) {
        if ($row.status -notin @("UP", "UP_SECURED")) {
            throw ("service not healthy after host startup: {0} status={1}" -f $row.service, $row.status)
        }
        if (-not [string]::IsNullOrWhiteSpace($row.err_log) -and (Test-Path $row.err_log)) {
            $errFile = Get-Item $row.err_log
            if ($errFile.Length -gt 0) {
                throw ("stderr is not empty: {0} log={1}" -f $row.service, $row.err_log)
            }
        }
        if (-not [string]::IsNullOrWhiteSpace($row.out_log) -and (Test-LogHasCriticalError -Path $row.out_log)) {
            throw ("critical error pattern found in stdout log: {0} log={1}" -f $row.service, $row.out_log)
        }
    }
}

$platformArgs = @()
$requestedServices = $null
foreach ($arg in $CliArgs) {
    $platformArgs += $arg
    if ($arg -like "--services=*") {
        $requestedServices = ($arg -split "=", 2)[1]
    }
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

& (Join-Path $PSScriptRoot "start-platform.ps1") @platformArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Assert-HostAcceptance -Root $root -RequestedServices $requestedServices
Write-Host "HOST_LINKED_READY"
