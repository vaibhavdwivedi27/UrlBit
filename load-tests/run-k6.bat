@echo off
REM ==============================================================================
REM Urlbit k6 Docker Runner (Windows CMD / PowerShell)
REM Executes k6 load testing scenarios via official Docker container (no install needed).
REM Usage:
REM   run-k6.bat [scenario] [target_host]
REM Examples:
REM   run-k6.bat smoke.js
REM   run-k6.bat ramp.js http://host.docker.internal:8080
REM ==============================================================================

set SCENARIO=%1
if "%SCENARIO%"=="" set SCENARIO=smoke.js

set TARGET_HOST=%2
if "%TARGET_HOST%"=="" set TARGET_HOST=http://host.docker.internal:8080

set SCRIPT_DIR=%~dp0
if not exist "%SCRIPT_DIR%scenarios\%SCENARIO%" (
  echo ERROR: Scenario '%SCENARIO%' not found in load-tests\scenarios\
  echo Available scenarios:
  dir /b "%SCRIPT_DIR%scenarios\*.js"
  exit /b 1
)

echo ======================================================================
echo  Running k6 Scenario: %SCENARIO%
echo  Target Host:         %TARGET_HOST%
echo ======================================================================

docker run --rm -i ^
  --add-host=host.docker.internal:host-gateway ^
  -v "%SCRIPT_DIR%..:/repo:ro" ^
  grafana/k6:latest run ^
  -e TARGET_HOST=%TARGET_HOST% ^
  /repo/load-tests/scenarios/%SCENARIO% %3 %4 %5 %6
