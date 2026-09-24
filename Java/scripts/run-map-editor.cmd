@echo off
call "%~dp0run-game.cmd" -Editor %*
exit /b %errorlevel%
