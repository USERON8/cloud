param(
  [string]$BaseUrl = "http://host.docker.internal:18080",
  [string]$Profile = "loadtest"
)

& (Join-Path $PSScriptRoot "run-k6.ps1") -Scenario "acceptance" -BaseUrl $BaseUrl -Profile $Profile
