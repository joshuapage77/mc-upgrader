@echo off
setlocal enabledelayedexpansion

:: File: scripts\find_latest_java.bat

set "quiet=0"
for %%A in (%*) do (
   if "%%A"=="-q" (
      set "quiet=1"
   )
)

:: Assume script is in .minecraft\games\scripts
set "runtime_base=..\..\runtime"

if not exist "%runtime_base%" (
   echo [ERROR] Runtime directory not found: %runtime_base%
   exit /b 1
)

if "%quiet%"=="0" echo [INFO] Scanning runtime directory: %runtime_base%

set "best_version=0"
set "best_path="

for /d %%D in ("%runtime_base%\java-runtime-*") do (
   for /d %%O in ("%%D\*") do (
      for /d %%J in ("%%O\java-runtime-*") do (
         set "java_path=%%~fJ\bin\java.exe"

         if exist "!java_path!" (
            for /f "tokens=3 delims= " %%V in ('"!java_path!" -version 2^>^&1 ^| findstr /i "version"') do (
               set "ver=%%~V"
               set "ver=!ver:"=!"

               for /f "tokens=1 delims=." %%M in ("!ver!") do (
                  set "major=%%M"
                  if "!major!"=="1" (
                     for /f "tokens=2 delims=." %%N in ("!ver!") do set "major=%%N"
                  )

                  set /a test_version=!major!
                  if !test_version! gtr !best_version! (
                     set "best_version=!test_version!"
                     set "best_path=!java_path!"
                  )
               )
            )
         )
      )
   )
)

if not defined best_path (
   echo [ERROR] No valid Java runtimes found.
   exit /b 1
)

if "%quiet%"=="0" echo [INFO] Selected Java version: %best_version% at %best_path%

echo %best_path%
exit /b 0
