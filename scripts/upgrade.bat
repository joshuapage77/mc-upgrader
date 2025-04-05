@echo off
setlocal enabledelayedexpansion

set "env_file=.env"

if exist "%env_file%" (
   for /f "usebackq delims=" %%A in ("%env_file%") do (
      set "%%A"
   )
) else (
   for /f "delims=" %%J in ('call find_latest_java.bat -q') do (
      set "JAVA_PATH=%%J"
   )
   >"%env_file%" echo JAVA_PATH=%JAVA_PATH%
   echo [INFO] Created .env with JAVA_PATH=%JAVA_PATH%
)

echo [INFO] Using Java: %JAVA_PATH%
"%JAVA_PATH%" -version

set "UPGRADE_CLASS=java\Upgrader.class"

if not exist "%UPGRADE_CLASS%" (
   echo [INFO] Compiling Java sources...
   "%JAVA_PATH%c" -d java java\*.java
   if errorlevel 1 (
      echo [ERROR] Compilation failed
      exit /b 1
   )
)

"%JAVA_PATH%" -cp java Upgrader