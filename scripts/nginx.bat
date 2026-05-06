@echo off
REM Aurora LowCode — Nginx Local Dev Helper
REM Usage: scripts\nginx.bat [start|stop|reload|test]
REM   start:  copies frontend build to nginx dir, starts nginx
REM   stop:   stops nginx
REM   reload: copies config and reloads nginx

set NGINX_PATH=E:\nginx-1.29.8
set CONF_SRC=E:\CogniBase\AURORA-LOWCODE\deploy\nginx-local.conf
set CONF_DST=%NGINX_PATH%\conf\nginx.conf
set FRONTEND_BUILD=E:\CogniBase\AURORA-LOWCODE\frontend\dist
set NGINX_HTML=%NGINX_PATH%\html\dist

if "%1"=="" set CMD=start
if /i "%1"=="start" set CMD=start
if /i "%1"=="stop" set CMD=stop
if /i "%1"=="reload" set CMD=reload
if /i "%1"=="test" set CMD=test

if "%CMD%"=="start" (
    copy /Y "%CONF_SRC%" "%CONF_DST%" >nul
    echo [Aurora] Copying frontend build...
    if exist "%FRONTEND_BUILD%" (
        xcopy /E /Y /Q "%FRONTEND_BUILD%\*.*" "%NGINX_HTML%\" >nul
    ) else (
        echo [Aurora] WARNING: frontend build not found at %FRONTEND_BUILD%
        echo [Aurora] Run: cd frontend ^&^& pnpm build
    )
    echo [Aurora] Starting nginx...
    cd /d "%NGINX_PATH%"
    start /B nginx.exe -p "%NGINX_PATH%"
    echo [Aurora] Nginx running at http://localhost:8088
    echo [Aurora]   Frontend : http://localhost:8088
    echo [Aurora]   Backend  : http://localhost:8088/api/
    echo [Aurora]   Swagger  : http://localhost:8088/swagger-ui/
    goto :eof
)

if "%CMD%"=="stop" (
    echo [Aurora] Stopping nginx...
    cd /d "%NGINX_PATH%"
    nginx.exe -s quit -p "%NGINX_PATH%"
    echo [Aurora] Nginx stopped.
    goto :eof
)

if "%CMD%"=="reload" (
    copy /Y "%CONF_SRC%" "%CONF_DST%" >nul
    cd /d "%NGINX_PATH%"
    nginx.exe -s reload -p "%NGINX_PATH%"
    echo [Aurora] Nginx config reloaded.
    goto :eof
)

if "%CMD%"=="test" (
    cd /d "%NGINX_PATH%"
    nginx.exe -t -p "%NGINX_PATH%"
    goto :eof
)

echo Usage: %0 [start^|stop^|reload^|test]
