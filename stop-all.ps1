param(
    [switch]$RemoveVolumes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$runtimeFile = Join-Path $PSScriptRoot ".runtime\services.json"

function Stop-IfRunning {
    param(
        [int]$ProcessId,
        [string]$Name
    )

    if ($ProcessId -le 0) {
        return
    }

    try {
        $proc = Get-Process -Id $ProcessId -ErrorAction Stop
        Stop-Process -Id $proc.Id -Force -ErrorAction Stop
        Write-Host "Detenido $Name (PID $ProcessId)" -ForegroundColor Green
    } catch {
        Write-Host "$Name no estaba activo (PID $ProcessId)" -ForegroundColor DarkGray
    }
}

function Stop-ByCommandPattern {
    param(
        [string]$ProcessName,
        [string]$Contains,
        [string]$Label
    )

    $matches = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -eq $ProcessName -and $_.CommandLine -like ("*" + $Contains + "*")
    }

    foreach ($proc in $matches) {
        try {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Write-Host "Detenido $Label (PID $($proc.ProcessId))" -ForegroundColor Green
        } catch {
        }
    }
}

if (Test-Path $runtimeFile) {
    try {
        $state = Get-Content -Path $runtimeFile -Raw | ConvertFrom-Json
        Stop-IfRunning -ProcessId ([int]$state.backendPid) -Name "backend"
        Stop-IfRunning -ProcessId ([int]$state.frontendPid) -Name "frontend"
    } catch {
        Write-Host "No se pudo leer .runtime/services.json, continuando con apagado de BD." -ForegroundColor Yellow
    }
} else {
    Write-Host "No existe archivo de estado .runtime/services.json, continuando con apagado de BD." -ForegroundColor DarkGray
}

Stop-ByCommandPattern -ProcessName "java.exe" -Contains "com.sistema.main.VisualDemoServerMain" -Label "backend"
Stop-ByCommandPattern -ProcessName "node.exe" -Contains "vite" -Label "frontend"

if ($RemoveVolumes) {
    docker compose -f .\db\docker-compose.yml down -v
} else {
    docker compose -f .\db\docker-compose.yml down
}

Write-Host "PostgreSQL detenido." -ForegroundColor Green
if ($RemoveVolumes) {
    Write-Host "Tambien se eliminaron los volumenes de datos." -ForegroundColor Yellow
}

if (Test-Path $runtimeFile) {
    Remove-Item -Path $runtimeFile -Force
}
