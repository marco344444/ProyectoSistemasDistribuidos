#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Script Unificado para Iniciar/Detener Sistema Distribuido de Procesamiento de Imágenes
.DESCRIPTION
    Consolida todas las operaciones del sistema (BD, Backend, Frontend) con validaciones,
    health checks y logging centralizado.
.PARAMETER Mode
    Modo de ejecución: 'start' (por defecto), 'stop', 'restart', 'status', 'check'
.PARAMETER Service
    Servicio a iniciar: 'all' (por defecto), 'db', 'backend', 'frontend'
.PARAMETER Profile
    Perfil de ejecución: 'dev' (por defecto), 'prod', 'test'
.EXAMPLE
    .\start.ps1 -Mode start -Service all -Profile dev
    .\start.ps1 -Mode stop -Service all
    .\start.ps1 -Mode check   # Validar sistema
    .\start.ps1 -Mode status  # Estado de servicios
#>

param(
    [ValidateSet("start", "stop", "restart", "status", "check")]
    [string]$Mode = "start",
    
    [ValidateSet("all", "db", "backend", "frontend")]
    [string]$Service = "all",
    
    [ValidateSet("dev", "prod", "test")]
    [string]$Profile = "dev",
    
    [switch]$RemoveVolumes = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommandPath
$logDir = Join-Path $scriptRoot ".runtime\logs"
$runtimeDir = Join-Path $scriptRoot ".runtime"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = Join-Path $logDir "sistema_$timestamp.log"

# Crear directorios si no existen
if (!(Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

# Configuración de servicios
$Config = @{
    db = @{
        name = "PostgreSQL"
        port = 5433
        container = "imageproc-postgres"
        healthUrl = ""
    }
    backend = @{
        name = "Backend Java"
        port = 8080
        healthUrl = "http://localhost:8080/api/health"
        mainClass = "com.sistema.main.VisualDemoServerMain"
    }
    frontend = @{
        name = "Frontend React"
        port = 5173
        devUrl = "http://localhost:5173"
        buildDir = "web-client\dist"
    }
}

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

function Log {
    param([string]$Message, [ValidateSet("INFO", "WARN", "ERROR", "SUCCESS")]$Level = "INFO")
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $colors = @{
        INFO    = "Cyan"
        WARN    = "Yellow"
        ERROR   = "Red"
        SUCCESS = "Green"
    }
    
    $logEntry = "[$timestamp] [$Level] $Message"
    Write-Host $logEntry -ForegroundColor $colors[$Level]
    Add-Content -Path $logFile -Value $logEntry
}

function LogSection {
    param([string]$Title)
    Log "════════════════════════════════════════════════════════════════" "INFO"
    Log $Title "INFO"
    Log "════════════════════════════════════════════════════════════════" "INFO"
}

# ============================================================================
# VALIDACIONES PREVIAS
# ============================================================================

function Validate-Prerequisites {
    LogSection "🔍 Validando Pre-requisitos"
    $valid = $true
    
    # Java
    if (!(Get-Command java -ErrorAction SilentlyContinue)) {
        Log "❌ Java no encontrado en PATH" "ERROR"
        $valid = $false
    } else {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        Log "✅ Java: $javaVersion" "SUCCESS"
    }
    
    # Javac
    if (!(Get-Command javac -ErrorAction SilentlyContinue)) {
        Log "❌ Javac no encontrado en PATH" "ERROR"
        $valid = $false
    } else {
        Log "✅ Javac encontrado" "SUCCESS"
    }
    
    # Node.js
    if (!(Get-Command node -ErrorAction SilentlyContinue)) {
        Log "❌ Node.js no encontrado en PATH" "ERROR"
        $valid = $false
    } else {
        $nodeVersion = node --version
        Log "✅ Node.js: $nodeVersion" "SUCCESS"
    }
    
    # npm
    if (!(Get-Command npm -ErrorAction SilentlyContinue)) {
        Log "❌ npm no encontrado en PATH" "ERROR"
        $valid = $false
    } else {
        $npmVersion = npm --version
        Log "✅ npm: $npmVersion" "SUCCESS"
    }
    
    # Docker
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Log "⚠️  Docker no encontrado (se usará BD en memoria)" "WARN"
    } else {
        Log "✅ Docker encontrado" "SUCCESS"
    }
    
    if (!$valid) {
        Log "❌ Faltan dependencias críticas" "ERROR"
        Log "Instala: Java JDK 17+, Node.js 18+, npm" "ERROR"
        exit 1
    }
    
    Log "✅ Todas las dependencias validadas" "SUCCESS"
}

# ============================================================================
# VALIDACIÓN DE PUERTOS
# ============================================================================

function Test-PortAvailable {
    param([int]$Port)
    
    try {
        $tcpConnection = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue
        return !$tcpConnection.TcpTestSucceeded
    } catch {
        return $true
    }
}

function Validate-Ports {
    LogSection "🔌 Validando Puertos Disponibles"
    
    $ports = @(
        @{port = 8080; service = "Backend"}
        @{port = 5433; service = "PostgreSQL"}
        @{port = 5173; service = "Frontend"}
    )
    
    $portsOk = $true
    foreach ($portCheck in $ports) {
        if (Test-PortAvailable $portCheck.port) {
            Log "✅ Puerto $($portCheck.port) [$($portCheck.service)] disponible" "SUCCESS"
        } else {
            Log "❌ Puerto $($portCheck.port) [$($portCheck.service)] ocupado" "ERROR"
            $portsOk = $false
        }
    }
    
    if (!$portsOk) {
        Log "⚠️  Algunos puertos están ocupados. Intenta cerrar los procesos o cambiar configuración." "WARN"
    }
}

# ============================================================================
# LIMPIEZA DE PROCESOS ANTERIORES
# ============================================================================

function Stop-LegacyProcesses {
    LogSection "🧹 Limpiando Procesos Anteriores"
    
    # Detener Java (Backend)
    $javaProcesses = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -eq "java.exe" -and $_.CommandLine -like "*VisualDemoServerMain*"
    }
    
    foreach ($proc in $javaProcesses) {
        try {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Log "✅ Detenido Backend (PID $($proc.ProcessId))" "SUCCESS"
        } catch {
            Log "⚠️  No se pudo detener Backend (PID $($proc.ProcessId))" "WARN"
        }
    }
    
    # Detener Node (Frontend)
    $nodeProcesses = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -eq "node.exe" -and ($_.CommandLine -like "*vite*" -or $_.CommandLine -like "*dev*")
    }
    
    foreach ($proc in $nodeProcesses) {
        try {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Log "✅ Detenido Frontend (PID $($proc.ProcessId))" "SUCCESS"
        } catch {
            Log "⚠️  No se pudo detener Frontend (PID $($proc.ProcessId))" "WARN"
        }
    }
}

# ============================================================================
# GESTIÓN DE BD
# ============================================================================

function Start-Database {
    LogSection "🗄️  Iniciando PostgreSQL"
    
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Log "⚠️  Docker no disponible - BD en memoria" "WARN"
        return
    }
    
    try {
        Push-Location (Join-Path $scriptRoot "db")
        
        # Detener contenedor anterior si existe
        try {
            docker stop imageproc-postgres -t 5 -ErrorAction SilentlyContinue
            docker rm imageproc-postgres -ErrorAction SilentlyContinue
        } catch {}
        
        # Iniciar nuevo contenedor
        if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
            docker-compose up -d
        } else {
            docker compose up -d
        }
        
        Log "✅ PostgreSQL iniciado" "SUCCESS"
        
        # Esperar a que BD esté lista
        $maxAttempts = 30
        $attempt = 0
        while ($attempt -lt $maxAttempts) {
            try {
                $result = docker exec imageproc-postgres pg_isready -U imageproc -d imageproc 2>&1
                if ($LASTEXITCODE -eq 0) {
                    Log "✅ PostgreSQL está listo" "SUCCESS"
                    break
                }
            } catch {}
            
            $attempt++
            Start-Sleep -Seconds 1
            Write-Host -NoNewline "."
        }
        
        if ($attempt -ge $maxAttempts) {
            Log "⚠️  PostgreSQL no respondió en tiempo" "WARN"
        }
        
        Pop-Location
    } catch {
        Log "❌ Error iniciando BD: $_" "ERROR"
        throw
    }
}

function Stop-Database {
    LogSection "🛑 Deteniendo PostgreSQL"
    
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Log "⚠️  Docker no disponible" "WARN"
        return
    }
    
    try {
        Push-Location (Join-Path $scriptRoot "db")
        
        if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
            docker-compose down $(if ($RemoveVolumes) { "-v" })
        } else {
            docker compose down $(if ($RemoveVolumes) { "-v" })
        }
        
        Log "✅ PostgreSQL detenido" "SUCCESS"
        Pop-Location
    } catch {
        Log "⚠️  Error deteniendo BD: $_" "WARN"
    }
}

# ============================================================================
# GESTIÓN DE BACKEND
# ============================================================================

function Start-Backend {
    LogSection "🖥️  Compilando e Iniciando Backend"
    
    try {
        Push-Location $scriptRoot
        
        # Compilar Java
        Log "Compilando Java..." "INFO"
        $srcFiles = Get-ChildItem -Path "src", "Contratos", "javax" -Include "*.java" -Recurse -ErrorAction SilentlyContinue
        
        if (!$srcFiles) {
            Log "❌ No hay archivos Java para compilar" "ERROR"
            Pop-Location
            return
        }
        
        $buildDir = Join-Path $scriptRoot "build"
        if (!(Test-Path $buildDir)) {
            New-Item -ItemType Directory -Path $buildDir | Out-Null
        }
        
        # Obtener librerías
        $libPath = @()
        $libDir = Join-Path $scriptRoot "lib"
        if (Test-Path $libDir) {
            $libPath = @((Get-ChildItem -Path $libDir -Filter "*.jar" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }) -join ";")
            if ($libPath) { $libPath = ";$libPath" }
        }
        
        # Compilar
        javac -encoding UTF-8 -d $buildDir -cp ".:$libPath" $srcFiles 2>&1 | ForEach-Object {
            if ($_ -like "*error*") {
                Log "❌ Error de compilación: $_" "ERROR"
            }
        }
        
        if ($LASTEXITCODE -ne 0) {
            Log "❌ Compilación fallida" "ERROR"
            Pop-Location
            return
        }
        
        Log "✅ Compilación exitosa" "SUCCESS"
        
        # Ejecutar Backend
        Log "Iniciando Backend en puerto 8080..." "INFO"
        $backendCmd = @(
            "Set-Location '$scriptRoot'",
            "`$buildPath = Join-Path '$scriptRoot' 'build'",
            "`$libPath = @((Get-ChildItem -Path (Join-Path '$scriptRoot' 'lib') -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object { `$_.FullName }) -join ';')",
            "`$cp = if (`$libPath) { `"`$buildPath;`$libPath`" } else { `$buildPath }",
            "java -cp `$cp com.sistema.main.VisualDemoServerMain"
        ) -join "; "
        
        $backendProcess = Start-Process -FilePath "powershell.exe" `
            -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd `
            -PassThru -NoNewWindow
        
        $backendPid = $backendProcess.Id
        Log "✅ Backend iniciado (PID $backendPid)" "SUCCESS"
        
        # Esperar a que Backend esté listo
        $maxAttempts = 30
        $attempt = 0
        while ($attempt -lt $maxAttempts) {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" `
                    -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
                if ($response.StatusCode -eq 200) {
                    Log "✅ Backend está listo" "SUCCESS"
                    break
                }
            } catch {}
            
            $attempt++
            Start-Sleep -Seconds 1
            Write-Host -NoNewline "."
        }
        
        # Guardar PID
        @{ backend = $backendPid } | ConvertTo-Json | Set-Content -Path (Join-Path $runtimeDir "backend.pid")
        
        Pop-Location
    } catch {
        Log "❌ Error iniciando Backend: $_" "ERROR"
        throw
    }
}

function Stop-Backend {
    LogSection "🛑 Deteniendo Backend"
    
    try {
        $javaProcesses = Get-CimInstance Win32_Process | Where-Object {
            $_.Name -eq "java.exe" -and $_.CommandLine -like "*VisualDemoServerMain*"
        }
        
        foreach ($proc in $javaProcesses) {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Log "✅ Backend detenido (PID $($proc.ProcessId))" "SUCCESS"
        }
    } catch {
        Log "⚠️  Error deteniendo Backend: $_" "WARN"
    }
}

# ============================================================================
# GESTIÓN DE FRONTEND
# ============================================================================

function Start-Frontend {
    LogSection "🌐 Iniciando Frontend React"
    
    try {
        $frontendPath = Join-Path $scriptRoot "web-client"
        
        # Instalar dependencias
        Log "Instalando dependencias npm..." "INFO"
        Push-Location $frontendPath
        npm install --silent
        Pop-Location
        
        Log "✅ Dependencias instaladas" "SUCCESS"
        
        # Iniciar Frontend
        Log "Iniciando Frontend en puerto 5173..." "INFO"
        $frontendCmd = @(
            "Set-Location '$frontendPath'",
            "npm run dev"
        ) -join "; "
        
        $frontendProcess = Start-Process -FilePath "powershell.exe" `
            -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $frontendCmd `
            -PassThru -NoNewWindow
        
        $frontendPid = $frontendProcess.Id
        Log "✅ Frontend iniciado (PID $frontendPid)" "SUCCESS"
        
        # Guardar PID
        @{ frontend = $frontendPid } | ConvertTo-Json | Set-Content -Path (Join-Path $runtimeDir "frontend.pid")
        
    } catch {
        Log "❌ Error iniciando Frontend: $_" "ERROR"
        throw
    }
}

function Stop-Frontend {
    LogSection "🛑 Deteniendo Frontend"
    
    try {
        $nodeProcesses = Get-CimInstance Win32_Process | Where-Object {
            $_.Name -eq "node.exe" -and $_.CommandLine -like "*vite*"
        }
        
        foreach ($proc in $nodeProcesses) {
            Stop-Process -Id $proc.ProcessId -Force -ErrorAction Stop
            Log "✅ Frontend detenido (PID $($proc.ProcessId))" "SUCCESS"
        }
    } catch {
        Log "⚠️  Error deteniendo Frontend: $_" "WARN"
    }
}

# ============================================================================
# SALUD DEL SISTEMA
# ============================================================================

function Check-SystemHealth {
    LogSection "💚 Verificando Salud del Sistema"
    
    $healthy = $true
    
    # Verificar Backend
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/health" `
            -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Log "✅ Backend: OK" "SUCCESS"
        } else {
            Log "❌ Backend: Respondiendo con error" "ERROR"
            $healthy = $false
        }
    } catch {
        Log "❌ Backend: No responde" "ERROR"
        $healthy = $false
    }
    
    # Verificar BD
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        try {
            $result = docker exec imageproc-postgres pg_isready -U imageproc 2>&1
            if ($LASTEXITCODE -eq 0) {
                Log "✅ BD: OK" "SUCCESS"
            } else {
                Log "❌ BD: No disponible" "ERROR"
                $healthy = $false
            }
        } catch {
            Log "❌ BD: No accesible" "ERROR"
            $healthy = $false
        }
    }
    
    # Verificar puertos
    if (!(Test-PortAvailable 8080)) {
        Log "✅ Puerto 8080 en uso (Backend)" "SUCCESS"
    } else {
        Log "⚠️  Puerto 8080 no en uso" "WARN"
    }
    
    if ($healthy) {
        Log "✅ Sistema saludable" "SUCCESS"
    } else {
        Log "⚠️  Sistema con problemas" "WARN"
    }
}

function Show-Status {
    LogSection "📊 Estado del Sistema"
    
    Log "Backend (8080):  $(if (!(Test-PortAvailable 8080)) { '🟢 RUNNING' } else { '🔴 STOPPED' })" "INFO"
    Log "PostgreSQL:      $(if (Get-Command docker -ErrorAction SilentlyContinue) { '🟢 RUNNING' } else { '⚠️  N/A' })" "INFO"
    Log "Frontend (5173): $(if (!(Test-PortAvailable 5173)) { '🟢 RUNNING' } else { '🔴 STOPPED' })" "INFO"
    Log "" "INFO"
    Log "URLs de acceso:" "INFO"
    Log "  • Backend:  http://localhost:8080/api/health" "INFO"
    Log "  • Frontend: http://localhost:5173" "INFO"
    Log "  • BD:       localhost:5433 (imageproc/imageproc123)" "INFO"
    Log "" "INFO"
    Log "Logs: $logFile" "INFO"
}

# ============================================================================
# ORQUESTACIÓN
# ============================================================================

function Main {
    LogSection "🚀 Sistema Distribuido de Procesamiento de Imágenes"
    Log "Modo: $Mode | Servicio: $Service | Perfil: $Profile" "INFO"
    
    try {
        switch ($Mode) {
            "start" {
                Validate-Prerequisites
                Validate-Ports
                Stop-LegacyProcesses
                
                if ($Service -in @("all", "db")) {
                    Start-Database
                    Start-Sleep -Seconds 2
                }
                if ($Service -in @("all", "backend")) {
                    Start-Backend
                    Start-Sleep -Seconds 3
                }
                if ($Service -in @("all", "frontend")) {
                    Start-Frontend
                }
                
                Log "" "INFO"
                Show-Status
            }
            
            "stop" {
                if ($Service -in @("all", "frontend")) {
                    Stop-Frontend
                }
                if ($Service -in @("all", "backend")) {
                    Stop-Backend
                }
                if ($Service -in @("all", "db")) {
                    Stop-Database
                }
                
                Log "✅ Servicios detenidos" "SUCCESS"
            }
            
            "restart" {
                & $MyInvocation.MyCommandPath -Mode "stop" -Service $Service
                Start-Sleep -Seconds 2
                & $MyInvocation.MyCommandPath -Mode "start" -Service $Service -Profile $Profile
            }
            
            "status" {
                Show-Status
                Check-SystemHealth
            }
            
            "check" {
                Validate-Prerequisites
                Validate-Ports
                Check-SystemHealth
            }
        }
        
        Log "✅ Operación completada exitosamente" "SUCCESS"
    } catch {
        Log "❌ Error fatal: $_" "ERROR"
        exit 1
    }
}

# ============================================================================
# EJECUTAR
# ============================================================================

Main
