@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" -ConservativeJit %*
set "launchExitCode=%errorlevel%"
if not "%launchExitCode%"=="0" (
    echo.
    echo Alderfall could not start or exited with an error. See the details above.
    pause
)
exit /b %launchExitCode%
