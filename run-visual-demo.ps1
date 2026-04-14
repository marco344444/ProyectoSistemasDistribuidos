Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

function Resolve-JavaTool {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ToolName
    )

    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\$ToolName.exe")
        $candidates += (Join-Path $env:JAVA_HOME "$ToolName.exe")
    }

    try {
        $command = Get-Command $ToolName -ErrorAction Stop
        if ($command -and $command.Source) {
            $candidates += $command.Source
        }
    } catch {
    }

    $candidates += @(
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\$ToolName.exe",
        "C:\Program Files\Java\jdk-17\bin\$ToolName.exe",
        "C:\Program Files\Java\jdk-21\bin\$ToolName.exe"
    )

    foreach ($candidate in ($candidates | Where-Object { $_ } | Select-Object -Unique)) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

$javaExe = Resolve-JavaTool -ToolName "java"
$javacExe = Resolve-JavaTool -ToolName "javac"

if (!$javaExe -or !$javacExe) {
    Write-Host "No se pudo localizar java.exe y javac.exe." -ForegroundColor Red
    Write-Host "Opciones para corregirlo:" -ForegroundColor Yellow
    Write-Host "1. Define JAVA_HOME apuntando a tu JDK" -ForegroundColor Yellow
    Write-Host "2. Agrega java y javac al PATH" -ForegroundColor Yellow
    Write-Host "3. Ajusta las rutas fallback dentro de run-visual-demo.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "Usando java:  $javaExe" -ForegroundColor DarkGray
Write-Host "Usando javac: $javacExe" -ForegroundColor DarkGray

if (Test-Path out) {
    Remove-Item -Recurse -Force out
}
New-Item -ItemType Directory -Path out | Out-Null

$srcFiles = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
if (-not $srcFiles -or $srcFiles.Count -eq 0) {
    Write-Host "No se encontraron archivos .java en src" -ForegroundColor Red
    exit 1
}

Write-Host "Compilando proyecto..." -ForegroundColor Cyan
& $javacExe -encoding UTF-8 -d out $srcFiles

Write-Host "Compilacion OK" -ForegroundColor Green
Write-Host "Levantando servidor visual en puerto 8080..." -ForegroundColor Cyan
& $javaExe -cp out com.sistema.main.VisualDemoServerMain
