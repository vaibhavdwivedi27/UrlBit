@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper startup batch script
@echo off

setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto findMaven

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
echo Please set the JAVA_HOME variable to the location of your JDK. >&2
goto error

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto findMaven

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
goto error

:findMaven
@REM Read distributionUrl from maven-wrapper.properties
set DISTRIBUTION_URL=
for /f "usebackq tokens=1,* delims==" %%a in ("%WRAPPER_PROPERTIES%") do (
    if "%%a"=="distributionUrl" set DISTRIBUTION_URL=%%b
)

if "%DISTRIBUTION_URL%"=="" (
    echo ERROR: Could not read distributionUrl from %WRAPPER_PROPERTIES% >&2
    goto error
)

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9
set MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd

if exist "%MVN_CMD%" goto runMaven

echo Downloading Maven from %DISTRIBUTION_URL%...
set DOWNLOAD_DIR=%USERPROFILE%\.m2\wrapper\dists
if not exist "%DOWNLOAD_DIR%" mkdir "%DOWNLOAD_DIR%"
set DOWNLOAD_FILE=%DOWNLOAD_DIR%\maven.zip

powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DOWNLOAD_FILE%' }"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to download Maven distribution >&2
    goto error
)

powershell -Command "& { Expand-Archive -Path '%DOWNLOAD_FILE%' -DestinationPath '%DOWNLOAD_DIR%' -Force }"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to extract Maven distribution >&2
    goto error
)

del "%DOWNLOAD_FILE%" >NUL 2>&1

if not exist "%MVN_CMD%" (
    echo ERROR: Maven extraction succeeded but mvn.cmd not found at %MVN_CMD% >&2
    goto error
)

:runMaven
"%MVN_CMD%" %* -f "%MAVEN_PROJECTBASEDIR%pom.xml"
set EXIT_CODE=%ERRORLEVEL%
goto end

:error
set EXIT_CODE=1

:end
endlocal
exit /b %EXIT_CODE%
