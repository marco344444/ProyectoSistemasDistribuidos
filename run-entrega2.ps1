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

function Get-RuntimeClasspath {
    $items = @("out")

    if (Test-Path ".\lib") {
        $jars = Get-ChildItem -Path ".\lib" -Filter *.jar -File -ErrorAction SilentlyContinue
        foreach ($jar in $jars) {
            $items += $jar.FullName
        }
    }

    return ($items -join [IO.Path]::PathSeparator)
}

$javaExe = Resolve-JavaTool -ToolName "java"
$javacExe = Resolve-JavaTool -ToolName "javac"

if (!$javaExe -or !$javacExe) {
    Write-Host "No se pudo localizar java.exe y javac.exe." -ForegroundColor Red
    exit 1
}

Write-Host "Usando java:  $javaExe" -ForegroundColor DarkGray
Write-Host "Usando javac: $javacExe" -ForegroundColor DarkGray

if (Test-Path out) {
    Remove-Item -Recurse -Force out
}
New-Item -ItemType Directory -Path out | Out-Null

$srcFiles = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

Write-Host "Compilando proyecto para Entrega 2..." -ForegroundColor Cyan
& $javacExe -encoding UTF-8 -d out $srcFiles

Write-Host "Ejecutando smoke test Entrega 2..." -ForegroundColor Cyan
$runtimeClasspath = Get-RuntimeClasspath
& $javaExe -cp $runtimeClasspath com.sistema.main.Entrega2SmokeTestMain
