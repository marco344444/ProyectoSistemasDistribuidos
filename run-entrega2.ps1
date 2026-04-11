Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin"
$javaExe = Join-Path $javaHome "java.exe"
$javacExe = Join-Path $javaHome "javac.exe"

if (!(Test-Path $javaExe) -or !(Test-Path $javacExe)) {
    Write-Host "No se encontro JDK en: $javaHome" -ForegroundColor Red
    exit 1
}

if (Test-Path out) {
    Remove-Item -Recurse -Force out
}
New-Item -ItemType Directory -Path out | Out-Null

$srcFiles = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

Write-Host "Compilando proyecto para Entrega 2..." -ForegroundColor Cyan
& $javacExe -encoding UTF-8 -d out $srcFiles

Write-Host "Ejecutando smoke test Entrega 2..." -ForegroundColor Cyan
& $javaExe -cp out com.sistema.main.Entrega2SmokeTestMain
