$ErrorActionPreference = 'Stop'

Push-Location (Join-Path $PSScriptRoot 'HotbarCycling-Paper')
gradle build
Pop-Location

Push-Location (Join-Path $PSScriptRoot 'HotbarCycling-Fabric')
gradle build
Pop-Location

Write-Host 'Builds finished. Check each module build\libs folder.'
