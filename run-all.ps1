Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$runtimeDir = Join-Path $PSScriptRoot ".runtime"
if (!(Test-Path $runtimeDir)) {
    New-Item -ItemType Directory -Path $runtimeDir | Out-Null
}

# Evita instancias duplicadas del backend visual y del frontend Vite.
$oldBackend = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq "java.exe" -and $_.CommandLine -like "*com.sistema.main.VisualDemoServerMain*"
}
foreach ($proc in $oldBackend) {
    try {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
        Write-Host "Instancia backend previa detenida (PID $($proc.ProcessId))" -ForegroundColor DarkGray
    } catch {
    }
}

$oldFrontend = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq "node.exe" -and $_.CommandLine -like "*vite*"
}
foreach ($proc in $oldFrontend) {
    try {
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
        Write-Host "Instancia frontend previa detenida (PID $($proc.ProcessId))" -ForegroundColor DarkGray
    } catch {
    }
}

Write-Host "[1/3] Iniciando PostgreSQL..." -ForegroundColor Cyan
& "$PSScriptRoot\run-db.ps1"

Write-Host "[2/3] Iniciando backend visual..." -ForegroundColor Cyan
$backendCmd = @(
    "Set-Location '$PSScriptRoot'",
    "$env:DB_URL='jdbc:postgresql://localhost:5433/imageproc'",
    "$env:DB_USER='imageproc'",
    "$env:DB_PASSWORD='imageproc123'",
    ".\\run-visual-demo.ps1"
) -join "; "
$backendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd -PassThru

Write-Host "[3/3] Iniciando frontend..." -ForegroundColor Cyan
$frontendCmd = @(
    "Set-Location '$PSScriptRoot\\web-client'",
    "npm install",
    "npm run dev"
) -join "; "
$frontendProcess = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd -PassThru

$serviceState = [ordered]@{
    startedAt = (Get-Date).ToString("o")
    backendPid = $backendProcess.Id
    frontendPid = $frontendProcess.Id
}
$serviceState | ConvertTo-Json | Set-Content -Path (Join-Path $runtimeDir "services.json") -Encoding UTF8

Write-Host "Servicios lanzados." -ForegroundColor Green
Write-Host "- PostgreSQL: localhost:5433" -ForegroundColor DarkGray
Write-Host "- Backend:    http://localhost:8080" -ForegroundColor DarkGray
Write-Host "- Frontend:   http://localhost:5173" -ForegroundColor DarkGray
Write-Host "Para detener todo: .\\stop-all.ps1" -ForegroundColor Yellow
