# 将 CMD 中的 MYSQL_PW 写入临时文件（UTF-8 无 BOM）
$ErrorActionPreference = "Stop"
$pw = $env:MYSQL_PW
if ([string]::IsNullOrWhiteSpace($pw)) {
    Write-Host "ERR: MySQL password is empty. Type password when bat prompts." -ForegroundColor Red
    exit 1
}
$path = Join-Path $env:TEMP "csrrm_mysql_pwd.txt"
$utf8 = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($path, $pw.Trim(), $utf8)
exit 0
