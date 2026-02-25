$ErrorActionPreference = "Stop"

function Get-PortOwners {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $owners = @()
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
                Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($pid in $connections) {
            try {
                $proc = Get-Process -Id $pid -ErrorAction Stop
                $owners += [pscustomobject]@{
                    Port        = $Port
                    Pid         = $pid
                    ProcessName = $proc.ProcessName
                    Source      = "Get-NetTCPConnection"
                }
            } catch {
                $owners += [pscustomobject]@{
                    Port        = $Port
                    Pid         = $pid
                    ProcessName = "unknown"
                    Source      = "Get-NetTCPConnection"
                }
            }
        }
    } catch {
        $lines = netstat -ano -p tcp | Select-String -Pattern "LISTENING\s+(\d+)$"
        foreach ($line in $lines) {
            $text = $line.Line.Trim()
            if ($text -notmatch ":(\d+)\s+") {
                continue
            }
            $localPort = [int]$matches[1]
            if ($localPort -ne $Port) {
                continue
            }
            if ($text -notmatch "LISTENING\s+(\d+)$") {
                continue
            }
            $pid = [int]$matches[1]
            try {
                $proc = Get-Process -Id $pid -ErrorAction Stop
                $owners += [pscustomobject]@{
                    Port        = $Port
                    Pid         = $pid
                    ProcessName = $proc.ProcessName
                    Source      = "netstat"
                }
            } catch {
                $owners += [pscustomobject]@{
                    Port        = $Port
                    Pid         = $pid
                    ProcessName = "unknown"
                    Source      = "netstat"
                }
            }
        }
    }

    $owners | Sort-Object Pid -Unique
}

function Wait-PortFree {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 8
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $owners = Get-PortOwners -Port $Port
        if (-not $owners -or $owners.Count -eq 0) {
            return $true
        }
        Start-Sleep -Milliseconds 300
    }
    return $false
}

function Kill-PortOwner {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [switch]$DryRun
    )

    $owners = Get-PortOwners -Port $Port
    if (-not $owners -or $owners.Count -eq 0) {
        Write-Host ("PORT_GUARD port={0} pid=- process=- action=skip reason=free" -f $Port)
        return
    }

    foreach ($owner in $owners) {
        if ($DryRun) {
            Write-Host ("PORT_GUARD port={0} pid={1} process={2} action=dry-run-kill" -f $owner.Port, $owner.Pid, $owner.ProcessName)
            continue
        }
        try {
            Stop-Process -Id $owner.Pid -Force -ErrorAction Stop
            $freed = Wait-PortFree -Port $Port -TimeoutSeconds 8
            $state = if ($freed) { "freed" } else { "occupied" }
            Write-Host ("PORT_GUARD port={0} pid={1} process={2} action=killed state={3}" -f $owner.Port, $owner.Pid, $owner.ProcessName, $state)
        } catch {
            Write-Host ("PORT_GUARD port={0} pid={1} process={2} action=kill-failed reason={3}" -f $owner.Port, $owner.Pid, $owner.ProcessName, $_.Exception.Message)
        }
    }
}

function Is-DockerOwner {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $owners = Get-PortOwners -Port $Port
    foreach ($owner in $owners) {
        $name = ($owner.ProcessName | Out-String).Trim().ToLowerInvariant()
        if ($name -like "*docker*" -or $name -like "*com.docker*") {
            return $true
        }
    }
    return $false
}
