[CmdletBinding()]
param(
    [switch]$SkipInfrastructure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$StateDir = Join-Path $ProjectRoot '.run'
$LogDir = Join-Path $ProjectRoot '.runlogs'

New-Item -ItemType Directory -Force -Path $StateDir, $LogDir | Out-Null

function Test-HttpEndpoint {
    param([string]$Uri)
    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -eq 200
    }
    catch {
        return $false
    }
}

function Get-ListeningProcessId {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $connection) { return $null }
    return $connection.OwningProcess
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string]$Command,
        [int]$Port,
        [string]$HealthUri
    )

    if (Test-HttpEndpoint -Uri $HealthUri) {
        Write-Host "[$Name] already running at $HealthUri" -ForegroundColor Yellow
        return
    }

    $listenerPid = Get-ListeningProcessId -Port $Port
    if ($null -ne $listenerPid) {
        throw "[$Name] port $Port is already used by PID $listenerPid, but the health check failed."
    }

    $stdout = Join-Path $LogDir "$Name.out.log"
    $stderr = Join-Path $LogDir "$Name.err.log"
    $process = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', $Command `
        -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr

    Set-Content -LiteralPath (Join-Path $StateDir "$Name.pid") -Value $process.Id -Encoding ascii
    Write-Host "[$Name] starting (PID $($process.Id))" -ForegroundColor Cyan
}

function Wait-ForEndpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Test-HttpEndpoint -Uri $Uri) {
            Write-Host "[$Name] ready: $Uri" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    $errorLog = Join-Path $LogDir "$Name.err.log"
    if (Test-Path -LiteralPath $errorLog) {
        Write-Host "[$Name] recent error log:" -ForegroundColor Red
        Get-Content -LiteralPath $errorLog -Tail 30
    }
    throw "[$Name] did not become ready within $TimeoutSeconds seconds."
}

if (-not $SkipInfrastructure) {
    Write-Host '[infrastructure] starting PostgreSQL and Redis' -ForegroundColor Cyan
    Push-Location $ProjectRoot
    try {
        docker compose up -d
        if ($LASTEXITCODE -ne 0) { throw 'docker compose up failed.' }
    }
    finally {
        Pop-Location
    }
}

Start-ManagedProcess -Name 'backend' `
    -WorkingDirectory (Join-Path $ProjectRoot 'backend') `
    -Command 'gradlew.bat bootRun' -Port 8080 `
    -HealthUri 'http://localhost:8080/actuator/health'

Start-ManagedProcess -Name 'frontend' `
    -WorkingDirectory (Join-Path $ProjectRoot 'frontend') `
    -Command 'npm run dev' -Port 5173 `
    -HealthUri 'http://localhost:5173'

Wait-ForEndpoint -Name 'backend' -Uri 'http://localhost:8080/actuator/health' -TimeoutSeconds 90
Wait-ForEndpoint -Name 'frontend' -Uri 'http://localhost:5173' -TimeoutSeconds 45

Write-Host ''
& (Join-Path $PSScriptRoot 'status-dev.ps1')