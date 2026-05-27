@echo off
setlocal EnableDelayedExpansion
REM 组长/验收：双击本文件即可（无需手动打开 PowerShell）
REM 自动：检查环境 -> 可选导入数据库 -> 启动后端 -> 打开浏览器
chcp 65001 >nul 2>&1
cd /d "%~dp0"
title 校园自习室预约系统 - 一键启动

echo.
echo ========================================
echo   校园自习室预约系统 - 一键启动
echo ========================================
echo.

if not exist "pom.xml" (
    echo [错误] 未找到 pom.xml，请在本项目根目录双击本文件。
    echo        若从 ZIP 解压，请进入含 pom.xml 的文件夹。
    pause
    exit /b 1
)

REM ---------- 1. 环境检查 ----------
echo [1/5] 检查 Java ...
where java >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java。请安装 JDK 21 或更高版本。
    echo        下载: https://adoptium.net/
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%v
echo       Java OK: %JAVA_VER%

echo [2/5] 检查 MySQL 客户端 ...
where mysql >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 mysql 命令。
    echo        请将 MySQL 8 的 bin 目录加入系统 PATH，例如:
    echo        C:\Program Files\MySQL\MySQL Server 8.0\bin
    pause
    exit /b 1
)
echo       mysql OK

if not exist "src\main\resources\static\index.html" (
    echo [警告] 前端静态页缺失，首次启动可能较慢或需联系提交者补全 static。
)

REM ---------- 2. MySQL 服务 ----------
echo.
echo [3/5] 检查 MySQL 服务 ...
set MYSQL_SVC=MySQL80
sc query %MYSQL_SVC% >nul 2>&1
if errorlevel 1 set MYSQL_SVC=MySQL
sc query %MYSQL_SVC% 2>nul | findstr /i "RUNNING" >nul
if errorlevel 1 (
    echo       服务未运行，尝试启动 %MYSQL_SVC% ...
    net start %MYSQL_SVC% >nul 2>&1
    if errorlevel 1 (
        echo [提示] 无法自动启动 MySQL（可能需要管理员权限）。
        echo        请手动打开「服务」启动 MySQL80，然后按任意键继续...
        pause >nul
    ) else (
        echo       已启动 %MYSQL_SVC%
    )
) else (
    echo       %MYSQL_SVC% 正在运行
)

REM ---------- 3. 密码（可由 VBS 预先写入临时文件；含特殊字符也安全）----------
set "PWD_FILE=%TEMP%\csrrm_mysql_pwd.txt"
if not exist "%PWD_FILE%" (
    echo.
    set /p "MYSQL_PW=请输入本机 MySQL 的 root 密码（仅保存在本机，不会上传）: "
    if not defined MYSQL_PW (
        echo [错误] 未输入密码。
        pause
        exit /b 1
    )
    powershell -NoProfile -Command "[IO.File]::WriteAllText($env:TEMP + '\csrrm_mysql_pwd.txt', $env:MYSQL_PW)"
)

REM ---------- 4. 是否导入数据库 ----------
set "DO_IMPORT=Y"
if exist "%TEMP%\csrrm_do_import.txt" (
    set /p DO_IMPORT=<"%TEMP%\csrrm_do_import.txt"
    del "%TEMP%\csrrm_do_import.txt" >nul 2>&1
) else if /i not "%~1"=="auto" (
    echo.
    echo 是否导入仓库中的数据库快照 database-full.sql ？
    echo   首次使用请选 Y；日常启动选 N 可跳过导入加快速度。
    set /p DO_IMPORT=输入 Y 导入 / N 跳过 [Y/N，默认 Y]: 
)
if "%DO_IMPORT%"=="" set DO_IMPORT=Y

echo.
echo [4/5] 配置本机并%DO_IMPORT%导入数据库...

if /i "%DO_IMPORT%"=="Y" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\leader-run-setup.ps1" -PwdFile "%PWD_FILE%"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\leader-run-setup.ps1" -PwdFile "%PWD_FILE%" -SkipImport
)
if errorlevel 1 (
    echo.
    echo [错误] 配置或导入失败，请查看上方红色提示。
    echo        也可阅读 组长复现指南.md 中的「分步操作」。
    pause
    exit /b 1
)

REM ---------- 5. 启动后端 ----------
echo.
echo [5/5] 正在新窗口启动后端（首次会下载 Maven 依赖，请耐心等待）...
echo       约 25 秒后自动打开浏览器: http://localhost:8080
echo       登录示例: 管理员 admin / admin123  学生 202301010101 / 123456
echo.
start "CSRRM-Backend" cmd /k "cd /d ""%~dp0"" && call mvnw.cmd spring-boot:run"
start /min cmd /c "ping -n 26 127.0.0.1 >nul && start http://localhost:8080"

echo.
echo 本窗口可关闭。请勿关闭标题为「CSRRM-后端服务」的窗口（关闭即停止系统）。
echo 详细说明见: 组长复现指南.md
echo.
pause
