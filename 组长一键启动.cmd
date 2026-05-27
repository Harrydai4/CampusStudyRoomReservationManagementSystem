@echo off
REM 若 .vbs 被系统拦截，请双击本文件（与 .bat 相同效果）
cd /d "%~dp0"
call "%~dp0组长一键启动.bat"
exit /b %ERRORLEVEL%
