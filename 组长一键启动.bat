@echo off
setlocal
chcp 65001 >nul 2>&1
cd /d "%~dp0"
title 校园自习室预约系统 - 一键启动

if not exist "pom.xml" (
    echo [错误] 未找到 pom.xml，请在项目根目录双击本文件。
    pause
    exit /b 1
)

set "CSRRM_SCRIPT_ROOT=%~dp0scripts"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\leader-one-click-start.ps1"
set ERR=%ERRORLEVEL%

echo.
if %ERR% NEQ 0 (
    echo [错误] 启动失败，错误码 %ERR%。请查看上方红色提示。
) else (
    echo 启动器已退出。
)
pause
exit /b %ERR%