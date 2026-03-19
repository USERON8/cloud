$ErrorActionPreference = "Stop"

function Test-SkyWalkingTruthy {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }

    switch ($Value.Trim().ToLowerInvariant()) {
        "1" { return $true }
        "true" { return $true }
        "yes" { return $true }
        "on" { return $true }
        default { return $false }
    }
}

function Resolve-SkyWalkingExistingFile {
    param(
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $null
    }

    try {
        return (Resolve-Path $Path -ErrorAction Stop).Path
    } catch {
        return $null
    }
}

function Get-SkyWalkingAgentVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_AGENT_VERSION)) {
        return $env:SKYWALKING_AGENT_VERSION
    }

    $pomPath = Join-Path $Root "pom.xml"
    if (Test-Path $pomPath) {
        $pomVersion = Get-Content $pomPath |
            Where-Object { $_ -match "<skywalking.version>(.+)</skywalking.version>" } |
            ForEach-Object { $matches[1].Trim() } |
            Select-Object -First 1
        if (-not [string]::IsNullOrWhiteSpace($pomVersion)) {
            return $pomVersion
        }
    }

    return "9.6.0"
}

function Get-SkyWalkingCacheRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root
    )

    if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_CACHE_DIR)) {
        return $env:SKYWALKING_CACHE_DIR
    }
    return (Join-Path $Root ".tmp\skywalking")
}

function Find-CachedSkyWalkingAgent {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $installRoot = Join-Path (Get-SkyWalkingCacheRoot -Root $Root) ("agent\{0}" -f $Version)
    if (-not (Test-Path $installRoot)) {
        return $null
    }

    $agent = Get-ChildItem -Path $installRoot -Recurse -Filter "skywalking-agent.jar" -File -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $agent) {
        return $null
    }
    return $agent.FullName
}

function Test-SkyWalkingArchive {
    param(
        [string]$ArchivePath
    )

    if ([string]::IsNullOrWhiteSpace($ArchivePath) -or -not (Test-Path $ArchivePath)) {
        return $false
    }

    try {
        tar -tzf $ArchivePath | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Enable-SkyWalkingOptionalPlugins {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AgentJarPath
    )

    $agentHome = Split-Path $AgentJarPath -Parent
    $optionalDir = Join-Path $agentHome "optional-plugins"
    $pluginsDir = Join-Path $agentHome "plugins"
    if (-not (Test-Path $optionalDir) -or -not (Test-Path $pluginsDir)) {
        return
    }

    $patterns = if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_OPTIONAL_PLUGINS)) {
        $env:SKYWALKING_OPTIONAL_PLUGINS -split "\s+"
    } else {
        @()
    }

    foreach ($pattern in $patterns) {
        if ([string]::IsNullOrWhiteSpace($pattern)) {
            continue
        }
        Get-ChildItem -Path $optionalDir -Filter $pattern -File -ErrorAction SilentlyContinue |
            ForEach-Object { Copy-Item -Force $_.FullName -Destination $pluginsDir }
    }
}

function Install-SkyWalkingAgent {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $cacheRoot = Get-SkyWalkingCacheRoot -Root $Root
    $downloadDir = Join-Path $cacheRoot "downloads"
    $archivePath = Join-Path $downloadDir ("apache-skywalking-java-agent-{0}.tgz" -f $Version)
    $archiveTempPath = "{0}.part" -f $archivePath
    $extractRoot = Join-Path $cacheRoot ("agent\{0}" -f $Version)
    $downloadUrls = if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_AGENT_DOWNLOAD_URL)) {
        @($env:SKYWALKING_AGENT_DOWNLOAD_URL)
    } else {
        @(
            "https://downloads.apache.org/skywalking/java-agent/{0}/apache-skywalking-java-agent-{0}.tgz" -f $Version,
            "https://archive.apache.org/dist/skywalking/java-agent/{0}/apache-skywalking-java-agent-{0}.tgz" -f $Version
        )
    }
    $downloadTimeout = 180
    if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_AGENT_DOWNLOAD_TIMEOUT_SECONDS)) {
        [void][int]::TryParse($env:SKYWALKING_AGENT_DOWNLOAD_TIMEOUT_SECONDS, [ref]$downloadTimeout)
    }

    New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null
    if (-not (Test-SkyWalkingArchive -ArchivePath $archivePath)) {
        Remove-Item -Force $archivePath, $archiveTempPath -ErrorAction SilentlyContinue
        $downloadSucceeded = $false
        foreach ($downloadUrl in $downloadUrls) {
            try {
                Invoke-WebRequest -Uri $downloadUrl -OutFile $archiveTempPath -TimeoutSec $downloadTimeout
                Move-Item -Force $archiveTempPath $archivePath
                $downloadSucceeded = $true
                break
            } catch {
                Remove-Item -Force $archiveTempPath -ErrorAction SilentlyContinue
            }
        }
        if (-not $downloadSucceeded) {
            throw ("SkyWalking agent download failed for version {0}." -f $Version)
        }
    }

    if (-not (Test-SkyWalkingArchive -ArchivePath $archivePath)) {
        Remove-Item -Force $archivePath -ErrorAction SilentlyContinue
        throw "SkyWalking agent archive validation failed."
    }

    $stagingDir = Join-Path $cacheRoot ("extract-{0}-{1}" -f $Version, [guid]::NewGuid().ToString("N"))
    if (Test-Path $stagingDir) {
        Remove-Item -Recurse -Force $stagingDir
    }
    if (Test-Path $extractRoot) {
        Remove-Item -Recurse -Force $extractRoot
    }
    New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
    New-Item -ItemType Directory -Force -Path $extractRoot | Out-Null

    tar -xzf $archivePath -C $stagingDir
    if ($LASTEXITCODE -ne 0) {
        throw ("SkyWalking agent archive extraction failed: {0}" -f $archivePath)
    }

    Get-ChildItem -Path $stagingDir -Force | Move-Item -Destination $extractRoot
    Remove-Item -Recurse -Force $stagingDir

    $resolvedAgent = Find-CachedSkyWalkingAgent -Root $Root -Version $Version
    if ([string]::IsNullOrWhiteSpace($resolvedAgent)) {
        throw "SkyWalking agent archive extracted but skywalking-agent.jar was not found."
    }
    Enable-SkyWalkingOptionalPlugins -AgentJarPath $resolvedAgent
    return $resolvedAgent
}

function Configure-SkyWalkingRuntime {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [switch]$Requested,
        [string]$AgentPath,
        [string]$CollectorBackend,
        [switch]$AllowDownload
    )

    $version = Get-SkyWalkingAgentVersion -Root $RepoRoot
    $explicitAgentPath = if (-not [string]::IsNullOrWhiteSpace($AgentPath)) {
        $AgentPath
    } else {
        $env:SKYWALKING_AGENT_PATH
    }
    $autoEnable = $true
    if (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_AUTO_ENABLE) -and -not (Test-SkyWalkingTruthy -Value $env:SKYWALKING_AUTO_ENABLE)) {
        $autoEnable = $false
    }

    $resolvedAgentPath = $null
    if (-not [string]::IsNullOrWhiteSpace($explicitAgentPath)) {
        $resolvedAgentPath = Resolve-SkyWalkingExistingFile -Path $explicitAgentPath
        if ([string]::IsNullOrWhiteSpace($resolvedAgentPath)) {
            if ($Requested) {
                throw ("SkyWalking agent not found: {0}" -f $explicitAgentPath)
            }
            Write-Host ("SKYWALKING enabled=false reason=agent-not-found path={0}" -f $explicitAgentPath)
            return $false
        }
    } elseif ($Requested -or $autoEnable) {
        $resolvedAgentPath = Find-CachedSkyWalkingAgent -Root $RepoRoot -Version $version
        if ([string]::IsNullOrWhiteSpace($resolvedAgentPath) -and $AllowDownload) {
            try {
                $resolvedAgentPath = Install-SkyWalkingAgent -Root $RepoRoot -Version $version
            } catch {
                if ($Requested) {
                    throw
                }
                Write-Host ("SKYWALKING enabled=false reason=download-failed version={0} message={1}" -f $version, $_.Exception.Message)
                return $false
            }
        }
    } else {
        Write-Host "SKYWALKING enabled=false reason=disabled"
        return $false
    }

    if ([string]::IsNullOrWhiteSpace($resolvedAgentPath)) {
        if ($Requested) {
            throw "SkyWalking requested but the agent is unavailable."
        }
        Write-Host ("SKYWALKING enabled=false reason=agent-unavailable version={0}" -f $version)
        return $false
    }

    $resolvedCollector = if (-not [string]::IsNullOrWhiteSpace($CollectorBackend)) {
        $CollectorBackend
    } elseif (-not [string]::IsNullOrWhiteSpace($env:SKYWALKING_COLLECTOR_BACKEND_SERVICE)) {
        $env:SKYWALKING_COLLECTOR_BACKEND_SERVICE
    } else {
        "127.0.0.1:{0}" -f (Get-DockerPortValue -Root $RepoRoot -Name "PORT_SKYWALKING_OAP_GRPC" -DefaultValue 11800)
    }

    Enable-SkyWalkingOptionalPlugins -AgentJarPath $resolvedAgentPath
    $env:SKYWALKING_AGENT_PATH = $resolvedAgentPath
    $env:SKYWALKING_COLLECTOR_BACKEND_SERVICE = $resolvedCollector
    $env:SKYWALKING_AGENT_VERSION = $version
    Write-Host ("SKYWALKING enabled=true agent={0} backend={1} version={2}" -f $resolvedAgentPath, $resolvedCollector, $version)
    return $true
}
