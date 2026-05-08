# Aurora Quick Start
$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " Aurora Low-Code Platform - Quick Start" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# --- Auto-detect and set JAVA_HOME ---
function Find-Java {
    $javaPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaPath) { return ($javaPath.Source | Split-Path | Split-Path) }
    $searchPaths = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft",
        "C:\Program Files\Zulu",
        "$env:LOCALAPPDATA\Programs\Java",
        "C:\Users\$env:USERNAME\.jdks"
    )
    foreach ($dir in $searchPaths) {
        if (Test-Path $dir) {
            $found = Get-ChildItem $dir -Directory -Recurse -Filter "bin" -ErrorAction SilentlyContinue |
                Where-Object { Test-Path "$($_.FullName)\java.exe" } |
                Select-Object -First 1
            if ($found) { return $found.Parent.FullName }
        }
    }
    return $null
}

$JAVA_HOME = Find-Java
if (-not $JAVA_HOME) {
    Write-Host "[ERROR] Java not found. Install JDK 25+." -ForegroundColor Red
    Read-Host "Press Enter to exit"; exit 1
}
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$env:PATH"
Write-Host "[OK] Java: $JAVA_HOME" -ForegroundColor Gray

try { docker --version 2>&1 | Out-Null; Write-Host "[OK] Docker" -ForegroundColor Gray }
catch { Write-Host "[ERROR] Docker not found." -ForegroundColor Red; Read-Host "Press Enter to exit"; exit 1 }

try { node --version 2>&1 | Out-Null; Write-Host "[OK] Node.js" -ForegroundColor Gray }
catch { Write-Host "[ERROR] Node.js not found." -ForegroundColor Red; Read-Host "Press Enter to exit"; exit 1 }

try { pnpm --version 2>&1 | Out-Null; Write-Host "[OK] pnpm" -ForegroundColor Gray }
catch { Write-Host "[INFO] Installing pnpm..." -ForegroundColor Yellow; npm install -g pnpm }

# --- Detect already running ---
$backendRunning = Get-Process -Name "java" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like "*spring-boot:run*" -or $_.CommandLine -like "*aurora*" }
$frontendRunning = Get-Process -Name "node" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like "*vite*" -or $_.CommandLine -like "*pnpm dev*" }

$rootDir = $PSScriptRoot
$frontendDir = "$rootDir\frontend"

# --- Infrastructure ---
Write-Host ""
Write-Host "[1/4] Starting PostgreSQL + Redis..." -ForegroundColor Cyan
docker compose -f docker-compose.dev.yml up -d

# --- Backend ---
Write-Host ""
if ($backendRunning) {
    Write-Host "[2/4] Backend already running, skipping" -ForegroundColor Yellow
} else {
    Write-Host "[2/4] Starting backend (port 8080)..." -ForegroundColor Cyan
    Start-Process cmd -ArgumentList "/k", "set JAVA_HOME=$JAVA_HOME && cd /d `"$rootDir`" && mvn spring-boot:run -Dspring.profiles.active=dev" -WindowStyle Normal
    Write-Host "  Backend starting..." -ForegroundColor Gray
}

# --- Frontend ---
Write-Host ""
if ($frontendRunning) {
    Write-Host "[3/4] Frontend already running, skipping" -ForegroundColor Yellow
} else {
    Write-Host "[3/4] Starting frontend (port 3000)..." -ForegroundColor Cyan
    if (-not (Test-Path "$frontendDir\node_modules")) {
        Write-Host "  Installing dependencies..." -ForegroundColor Yellow
        Push-Location $frontendDir; pnpm install; Pop-Location
    }
    Start-Process cmd -ArgumentList "/k", "cd /d `"$frontendDir`" && pnpm dev" -WindowStyle Normal
    Write-Host "  Frontend starting..." -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host " Backend:   http://localhost:8080" -ForegroundColor White
Write-Host " Swagger:   http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host " Frontend:  http://localhost:3000" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to exit"
