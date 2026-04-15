Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker no esta disponible en PATH." -ForegroundColor Red
    exit 1
}

if (-not (Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    Write-Host "Usando docker compose plugin..." -ForegroundColor DarkGray
    docker compose -f .\db\docker-compose.yml up -d
} else {
    docker-compose -f .\db\docker-compose.yml up -d
}

Write-Host "Base de datos PostgreSQL iniciada en localhost:5433" -ForegroundColor Green
Write-Host "DB: imageproc | USER: imageproc | PASSWORD: imageproc123" -ForegroundColor Cyan
