# 供 组长一键启动.bat 调用：从临时文件读取密码，避免特殊字符在 cmd 传参出错
param(
    [Parameter(Mandatory = $true)][string]$PwdFile,
    [switch]$SkipImport
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$pwd = (Get-Content -LiteralPath $PwdFile -Raw -Encoding UTF8).Trim()
if ([string]::IsNullOrWhiteSpace($pwd)) {
    Write-Host "ERR: MySQL password file is empty. Re-run bat and enter root password." -ForegroundColor Red
    exit 1
}
Remove-Item -LiteralPath $PwdFile -Force -ErrorAction SilentlyContinue

& (Join-Path $PSScriptRoot "test-mysql-login.ps1") -Password $pwd
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$args = @{
    MySqlPassword = $pwd
    SkipStart     = $true
    NoBrowser     = $true
}
if ($SkipImport) { $args.SkipImport = $true }

& (Join-Path $PSScriptRoot "setup-after-clone.ps1") @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
exit 0
