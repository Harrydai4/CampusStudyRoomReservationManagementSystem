# 供 组长一键启动.bat 调用：从临时文件读取密码，避免特殊字符在 cmd 传参出错
param(
    [Parameter(Mandatory = $true)][string]$PwdFile,
    [switch]$SkipImport
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pwd = (Get-Content -LiteralPath $PwdFile -Raw).Trim()
Remove-Item -LiteralPath $PwdFile -Force -ErrorAction SilentlyContinue

$args = @{
    MySqlPassword = $pwd
    SkipStart     = $true
    NoBrowser     = $true
}
if ($SkipImport) { $args.SkipImport = $true }

& (Join-Path $PSScriptRoot "setup-after-clone.ps1") @args
