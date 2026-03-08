$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "start-platform.ps1") @args
exit $LASTEXITCODE
