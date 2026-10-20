[CmdletBinding()]
param(
    [switch]$KeepInfrastructure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$StateDir = Join-Path $ProjectRoot '.run'

function Stop-ProcessTree {
    param([int]$ProcessId, [string]$Name)
    if ($null -eq (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) { return }
    & taskkill.exe /PID $ProcessId /T /F | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$Name] stopped (PID $ProcessId)" -ForegroundColor Green
    }
}

function Stop-ManagedProcess {
    param([string]$Name, [int]$Port)

    $pidFile = Join-Path $StateDir "$Name.pid"
    if (Test-Path -LiteralPath $pidFile) {
        $savedPid = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($savedPid -match '^\d+$') {
            Stop-ProcessTree -ProcessId ([int]$savedPid) -Name $Name
        }
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
    }

    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($listenerPid in $listeners) {
        Stop-ProcessTree -ProcessId $listenerPid -Name $Name
    }
}

Stop-ManagedProcess -Name 'frontend' -Port 5173
Stop-ManagedProcess -Name 'backend' -Port 8080

if (-not $KeepInfrastructure) {
    Write-Host '[infrastructure] stopping PostgreSQL and Redis' -ForegroundColor Cyan
    Push-Location $ProjectRoot
    try {
        docker compose stop
        if ($LASTEXITCODE -ne 0) { throw 'docker compose stop failed.' }
    }
    finally {
        Pop-Location
    }
}

Write-Host 'SafeHome development services are stopped.' -ForegroundColor Green