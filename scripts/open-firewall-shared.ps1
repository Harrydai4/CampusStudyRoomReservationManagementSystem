# Allow inbound TCP 8080 and 3306 for shared CSRRM
# MUST run PowerShell as Administrator

$ErrorActionPreference = "Continue"

function Add-FirewallRuleIfMissing {
    param([string]$Name, [int]$Port)
    $existing = Get-NetFirewallRule -DisplayName $Name -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "SKIP: rule already exists - $Name" -ForegroundColor DarkGray
        return $true
    }
    try {
        New-NetFirewallRule -DisplayName $Name -Direction Inbound -LocalPort $Port -Protocol TCP -Action Allow -ErrorAction Stop | Out-Null
        Write-Host "OK: allowed TCP $Port ($Name)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "FAIL: cannot add rule for port $Port - $_" -ForegroundColor Red
        return $false
    }
}

Write-Host "=== CSRRM firewall ===" -ForegroundColor Cyan
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "WARN: not running as Administrator. Rules may fail." -ForegroundColor Yellow
    Write-Host "      Right-click PowerShell -> Run as administrator" -ForegroundColor Yellow
    Write-Host ""
}

$ok8080 = Add-FirewallRuleIfMissing -Name "CSRRM HTTP 8080" -Port 8080
$ok3306 = Add-FirewallRuleIfMissing -Name "CSRRM MySQL 3306" -Port 3306

Write-Host ""
if ($ok8080 -and $ok3306) {
    Write-Host "Done. Teammates use http://YOUR_LAN_IP:8080" -ForegroundColor Yellow
} else {
    Write-Host "Some rules failed. Re-run this script as Administrator." -ForegroundColor Red
    exit 1
}
