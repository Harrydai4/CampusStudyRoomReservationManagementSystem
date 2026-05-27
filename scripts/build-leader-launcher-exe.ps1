# 可选：将 组长一键启动.bat 打包为单个 .exe（依赖 Windows 自带的 iexpress）
# 用法（在项目根目录）: .\scripts\build-leader-launcher-exe.ps1
# 生成: dist\CSRRM-组长一键启动.exe

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$bat = Join-Path $root "组长一键启动.bat"
$vbs = Join-Path $root "组长一键启动.vbs"
$dist = Join-Path $root "dist"
$outExe = Join-Path $dist "CSRRM-组长一键启动.exe"

if (-not (Test-Path $bat)) { throw "missing 组长一键启动.bat" }

New-Item -ItemType Directory -Path $dist -Force | Out-Null

$sedPath = Join-Path $env:TEMP "csrrm_iexpress.sed"
$batEsc = $bat -replace '\\', '\\'
$vbsEsc = $vbs -replace '\\', '\\'
$outEsc = $outExe -replace '\\', '\\'
$rootEsc = $root -replace '\\', '\\'

@"
[Version]
Class=IEXPRESS
SEDVersion=3
[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=1
HideExtractAnimation=0
HideExtractProgress=0
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=解压完成。请双击 CSRRM-组长一键启动.exe 同目录下的 组长一键启动.vbs 或 .bat 运行。
TargetName=%TargetName%
FriendlyName=CSRRM Leader Launcher
AppLaunched=cmd /c echo 请运行 组长一键启动.vbs
PostInstallCmd=<None>
AdminQuietInst=
UserQuietInst=
SourceFiles=SourceFiles
[Strings]
InstallPrompt=将解压校园自习室一键启动文件到当前目录，是否继续？
DisplayLicense=
TargetName=$outEsc
FriendlyName=CSRRM
[SourceFiles]
SourceFiles0=$rootEsc
[SourceFiles0]
%FILE0%=组长一键启动.bat
%FILE1%=组长一键启动.vbs
"@ | Set-Content -Path $sedPath -Encoding ASCII

Write-Host "Building $outExe ..."
& iexpress /N $sedPath /Q
if ($LASTEXITCODE -ne 0) {
    Write-Host "iexpress failed. Leader can use 组长一键启动.vbs directly (double-click)." -ForegroundColor Yellow
    exit 1
}
Write-Host "OK: $outExe" -ForegroundColor Green
Write-Host "Note: iexpress packages bat+vbs; MySQL/JDK must still be installed on leader PC."
