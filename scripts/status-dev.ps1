[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$ProjectRoot = Split-Path -Parent $PSScriptRoot

function Get-HttpStatus {
    param([string]$Uri)
    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) { return 'RUNNING' }
        return "HTTP $($response.StatusCode)"
    }
    catch {
        return 'STOPPED'
    }
}

function Get-ContainerStatus {
    param([string]$ContainerName)
    try {
        $value = docker inspect --format '{{if .State.Health}}{{.State.Status}} / {{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $ContainerName 2>$null
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($value)) { return 'NOT FOUND' }
        return $value.Trim()
    }
    catch {
        return 'DOCKER UNAVAILABLE'
    }
}

$rows = @(
    [pscustomobject]@{ Component = 'Frontend'; Endpoint = 'http://localhost:5173'; Status = Get-HttpStatus 'http://localhost:5173' }
    [pscustomobject]@{ Component = 'Backend'; Endpoint = 'http://localhost:8080/actuator/health'; Status = Get-HttpStatus 'http://localhost:8080/actuator/health' }
    [pscustomobject]@{ Component = 'PostgreSQL'; Endpoint = '127.0.0.1:5432'; Status = Get-ContainerStatus 'safehome-postgres' }
    [pscustomobject]@{ Component = 'Redis'; Endpoint = '127.0.0.1:6379'; Status = Get-ContainerStatus 'safehome-redis' }
)

Write-Host ''
Write-Host 'SafeHome development service status' -ForegroundColor Cyan
$rows | Format-Table -AutoSize

$allReady = ($rows[0].Status -eq 'RUNNING') -and
    ($rows[1].Status -eq 'RUNNING') -and
    ($rows[2].Status -match '^running / healthy$') -and
    ($rows[3].Status -match '^running / healthy$')

if (-not $allReady) { exit 1 }