@echo off
call "%~dp0run-game.cmd" -Launcher %*
exit /b %errorlevel%
