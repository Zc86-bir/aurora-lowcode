@echo off
REM Aurora LowCode — Caddy Local Dev Helper
REM Usage: scripts\caddy.bat [start|stop|reload|test]
REM   start:  starts caddy with local dev config
REM   stop:   stops caddy (kills process since admin API is disabled)
REM   reload: stops and restarts with new config (admin off requires restart)
REM   test:   validates caddy config

set CADDY_PATH=E:\caddy
set CONF_SRC=E:\CogniBase\AURORA-LOWCODE\deploy\caddy-local.Caddyfile
set FRONTEND_BUILD=E:\CogniBase\AURORA-LOWCODE\frontend\dist
set CADDY_HTML=%CADDY_PATH%\html\dist

if "%1"=="" set CMD=start
if /i "%1"=="start" set CMD=start
if /i "%1"=="stop" set CMD=stop
if /i "%1"=="reload" set CMD=reload
if /i "%1"=="test" set CMD=test

if "%CMD%"=="start" (
    echo [Aurora] Copying frontend build...
    if exist "%FRONTEND_BUILD%" (
        xcopy /E /Y /Q "%FRONTEND_BUILD%\*.*" "%CADDY_HTML%\" >nul
    ) else (
        echo [Aurora] WARNING: frontend build not found at %FRONTEND_BUILD%
        echo [Aurora] Run: cd frontend ^&^& pnpm build
    )
    echo [Aurora] Starting caddy...
    cd /d "%CADDY_PATH%"
    start /B caddy.exe run --config "%CONF_SRC%"
    echo [Aurora] Caddy running at http://localhost:8088
    echo [Aurora]   Frontend : http://localhost:8088
    echo [Aurora]   Backend  : http://localhost:8088/api/
    echo [Aurora]   Swagger  : http://localhost:8088/swagger-ui/
    goto :eof
)

if "%CMD%"=="stop" (
    echo [Aurora] Stopping caddy...
    taskkill /IM caddy.exe /F >nul 2>&1
    echo [Aurora] Caddy stopped.
    goto :eof
)

if "%CMD%"=="reload" (
    echo [Aurora] Restarting caddy with new config...
    taskkill /IM caddy.exe /F >nul 2>&1
    timeout /t 2 /nobreak >nul
    cd /d "%CADDY_PATH%"
    start /B caddy.exe run --config "%CONF_SRC%"
    echo [Aurora] Caddy restarted.
    goto :eof
)

if "%CMD%"=="test" (
    cd /d "%CADDY_PATH%"
    caddy.exe validate --config "%CONF_SRC%"
    goto :eof
)

echo Usage: %0 [start^|stop^|reload^|test]
