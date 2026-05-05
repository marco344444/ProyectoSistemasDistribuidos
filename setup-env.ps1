#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Script de Validación y Setup del Entorno
.DESCRIPTION
    Verifica requisitos del sistema y genera archivos de configuración
.EXAMPLE
    .\setup-env.ps1
#>

param(
    [switch]$Force = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommandPath

# ============================================================================
# COLORES
# ============================================================================

$colors = @{
    Success = "Green"
    Error   = "Red"
    Warning = "Yellow"
    Info    = "Cyan"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [ValidateSet("Success", "Error", "Warning", "Info")]
        [string]$Color = "Info"
    )
    Write-Host $Message -ForegroundColor $colors[$Color]
}

# ============================================================================
# VALIDACIONES
# ============================================================================

Write-ColorOutput "╔════════════════════════════════════════════════════════════════╗" "Info"
Write-ColorOutput "║  VALIDACIÓN Y SETUP DEL ENTORNO - Sistema Distribuido         ║" "Info"
Write-ColorOutput "╚════════════════════════════════════════════════════════════════╝" "Info"
Write-Host ""

$allValid = $true

# 1. Java
Write-ColorOutput "[1/8] Verificando Java..." "Info"
if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-ColorOutput "✅ Java: $javaVersion" "Success"
} else {
    Write-ColorOutput "❌ Java no encontrado" "Error"
    $allValid = $false
}

if (Get-Command javac -ErrorAction SilentlyContinue) {
    Write-ColorOutput "✅ Javac encontrado" "Success"
} else {
    Write-ColorOutput "❌ Javac no encontrado" "Error"
    $allValid = $false
}

# 2. Node.js
Write-ColorOutput "[2/8] Verificando Node.js..." "Info"
if (Get-Command node -ErrorAction SilentlyContinue) {
    $nodeVersion = node --version
    Write-ColorOutput "✅ Node.js $nodeVersion" "Success"
} else {
    Write-ColorOutput "❌ Node.js no encontrado" "Error"
    $allValid = $false
}

if (Get-Command npm -ErrorAction SilentlyContinue) {
    $npmVersion = npm --version
    Write-ColorOutput "✅ npm $npmVersion" "Success"
} else {
    Write-ColorOutput "❌ npm no encontrado" "Error"
    $allValid = $false
}

# 3. Docker
Write-ColorOutput "[3/8] Verificando Docker..." "Info"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $dockerVersion = docker --version
    Write-ColorOutput "✅ $dockerVersion" "Success"
} else {
    Write-ColorOutput "⚠️  Docker no encontrado (usará BD en memoria)" "Warning"
}

# 4. Directorios
Write-ColorOutput "[4/8] Verificando directorios..." "Info"
$dirs = @("src", "web-client", "db", "lib", "Contratos")
foreach ($dir in $dirs) {
    $fullPath = Join-Path $scriptRoot $dir
    if (Test-Path $fullPath) {
        Write-ColorOutput "✅ $dir" "Success"
    } else {
        Write-ColorOutput "❌ $dir faltante" "Error"
        $allValid = $false
    }
}

# 5. Archivos críticos
Write-ColorOutput "[5/8] Verificando archivos críticos..." "Info"
$files = @(
    "db/docker-compose.yml"
    "db/init/001_schema.sql"
    "web-client/package.json"
)
foreach ($file in $files) {
    $fullPath = Join-Path $scriptRoot $file
    if (Test-Path $fullPath) {
        Write-ColorOutput "✅ $file" "Success"
    } else {
        Write-ColorOutput "⚠️  $file faltante" "Warning"
    }
}

# 6. Crear .env.example si no existe
Write-ColorOutput "[6/8] Verificando archivo de configuración..." "Info"
$envExample = Join-Path $scriptRoot ".env.example"
if (!(Test-Path $envExample)) {
    @"
# ============================================================================
# Configuración del Sistema - EJEMPLO
# ============================================================================
# Copia este archivo a .env.local y personaliza según tu entorno

# Base de Datos
DB_HOST=localhost
DB_PORT=5433
DB_NAME=imageproc
DB_USER=imageproc
DB_PASSWORD=imageproc123

# Backend
BACKEND_HOST=localhost
BACKEND_PORT=8080

# Frontend
FRONTEND_PORT=5173

# RMI (para nodos distribuidos)
RMI_HOST=localhost
RMI_PORT=1099

# Perfil
PROFILE=dev

# Logging
LOG_LEVEL=INFO
"@ | Set-Content $envExample

    Write-ColorOutput "✅ .env.example creado" "Success"
} else {
    Write-ColorOutput "✅ .env.example existe" "Success"
}

# 7. Crear .env.local si no existe
Write-ColorOutput "[7/8] Verificando configuración local..." "Info"
$envLocal = Join-Path $scriptRoot ".env.local"
if (!(Test-Path $envLocal) -or $Force) {
    Copy-Item $envExample $envLocal -Force
    Write-ColorOutput "✅ .env.local creado/actualizado" "Success"
} else {
    Write-ColorOutput "✅ .env.local existe (usa --Force para regenerar)" "Success"
}

# 8. Resumen
Write-ColorOutput "[8/8] Generando resumen..." "Info"
Write-Host ""

if ($allValid) {
    Write-ColorOutput "╔════════════════════════════════════════════════════════════════╗" "Success"
    Write-ColorOutput "║  ✅ ENTORNO VALIDADO - LISTO PARA INICIAR                     ║" "Success"
    Write-ColorOutput "╚════════════════════════════════════════════════════════════════╝" "Success"
    Write-Host ""
    
    Write-ColorOutput "Próximos pasos:" "Info"
    Write-Host "  1. Abre una terminal PowerShell en la raíz del proyecto"
    Write-Host "  2. Ejecuta: .\start.ps1"
    Write-Host ""
    Write-Host "Opciones disponibles:"
    Write-Host "  .\start.ps1 -Mode start -Service all      # Inicia todo (BD, Backend, Frontend)"
    Write-Host "  .\start.ps1 -Mode start -Service backend  # Solo Backend"
    Write-Host "  .\start.ps1 -Mode stop -Service all       # Detiene todo"
    Write-Host "  .\start.ps1 -Mode status                  # Ver estado"
    Write-Host "  .\start.ps1 -Mode check                   # Validar sistema"
    Write-Host ""
    
    Write-ColorOutput "URLs de acceso:" "Info"
    Write-Host "  • Frontend:  http://localhost:5173"
    Write-Host "  • Backend:   http://localhost:8080/api/health"
    Write-Host "  • BD:        localhost:5433 (user: imageproc / password: imageproc123)"
    Write-Host ""
    
} else {
    Write-ColorOutput "╔════════════════════════════════════════════════════════════════╗" "Error"
    Write-ColorOutput "║  ❌ ENTORNO INCOMPLETO - INSTALA DEPENDENCIAS                ║" "Error"
    Write-ColorOutput "╚════════════════════════════════════════════════════════════════╝" "Error"
    Write-Host ""
    
    Write-ColorOutput "Requisitos faltantes:" "Error"
    Write-Host "  • Java JDK 17+ desde https://www.oracle.com/java/technologies/downloads/"
    Write-Host "  • Node.js 18+ desde https://nodejs.org/"
    Write-Host "  • Docker Desktop desde https://www.docker.com/products/docker-desktop"
    Write-Host ""
    
    exit 1
}

Write-Host ""
Write-ColorOutput "Para más información, ver:" "Info"
Write-Host "  • README.md - Documentación principal"
Write-Host "  • DEPLOYMENT_VMS.md - Setup en máquinas virtuales"
Write-Host "  • AUDITORIA.md - Análisis del sistema"
Write-Host ""
