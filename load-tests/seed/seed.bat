@echo off
REM ==============================================================================
REM Urlbit Benchmark Seeding Script (Windows CMD / PowerShell)
REM Seeds the PostgreSQL container with 100 hot links and 10,000 cold links.
REM Usage: seed.bat [container_name]
REM ==============================================================================

set CONTAINER_NAME=%1

if "%CONTAINER_NAME%"=="" (
  docker ps --format "{{.Names}}" | findstr /C:"urlbit-postgres" >nul
  if not errorlevel 1 (
    set CONTAINER_NAME=urlbit-postgres
  ) else (
    docker ps --format "{{.Names}}" | findstr /C:"urlbit-prod-postgres" >nul
    if not errorlevel 1 (
      set CONTAINER_NAME=urlbit-prod-postgres
    ) else (
      set CONTAINER_NAME=urlbit-postgres
    )
  )
)

echo Checking if PostgreSQL container '%CONTAINER_NAME%' is running...
docker ps --format "{{.Names}}" | findstr /C:"%CONTAINER_NAME%" >nul
if errorlevel 1 (
  echo ERROR: Neither 'urlbit-postgres' nor 'urlbit-prod-postgres' is running.
  echo Start infrastructure first with:
  echo   docker compose -f infra\compose.local.yml up -d postgres
  echo   OR
  echo   docker compose -f infra\compose.prod.yml up -d postgres
  exit /b 1
)

echo Seeding database with benchmark data...
docker exec -i %CONTAINER_NAME% psql -U urlbit -d urlbit < "%~dp0seed-data.sql"
if errorlevel 1 (
  echo ERROR: Failed to seed database.
  exit /b 1
)

echo Database successfully seeded!
echo - User: benchmarks@urlbit.in (Password: BenchmarkPassword123!)
echo - 100 Hot Links: hot-0001 through hot-0100
echo - 10,000 Cold Links: cold-00001 through cold-10000
