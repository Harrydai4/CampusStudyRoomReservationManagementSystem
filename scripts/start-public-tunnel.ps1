# Public HTTPS URL for teammates on different WiFi (Cloudflare Quick Tunnel)
# ONLY forwards http://127.0.0.1:8080 — does not expose MySQL or whole PC.
#
# Prerequisite:
#   Window A: .\scripts\start-shared-server.ps1  (backend on 8080)
#   cloudflared installed: .\scripts\install-tunnel-tools.ps1
#
# Run from project root: .\scripts\start-public-tunnel.ps1

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "=== CSRRM public tunnel (Cloudflare) ===" -ForegroundColor Cyan
Write-Host ""

& (Join-Path $root "scripts\check-public-exposure-safety.ps1")
if ($LASTEXITCODE -ne 0) { exit 1 }

$cloudflared = (Get-Command cloudflared -ErrorAction SilentlyContinue).Source
if (-not $cloudflared) {
    $candidates = @(
        "$env:ProgramFiles\cloudflared\cloudflared.exe",
        "$env:LOCALAPPDATA\Microsoft\WinGet\Links\cloudflared.exe"
    )
    $cloudflared = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $cloudflared) {
    Write-Host "ERR: cloudflared not found. Run: .\scripts\install-tunnel-tools.ps1" -ForegroundColor Red
    Write-Host "     Then reopen PowerShell." -ForegroundColor Red
    exit 1
}

$listening = netstat -ano | Select-String "LISTENING" | Select-String ":8080\s"
if (-not $listening) {
    Write-Host "ERR: nothing listening on port 8080." -ForegroundColor Red
    Write-Host "     Open another PowerShell window and run:" -ForegroundColor Yellow
    Write-Host "     .\scripts\start-shared-server.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host "OK: backend detected on port 8080" -ForegroundColor Green
Write-Host ""
Write-Host "Starting HTTPS tunnel to http://127.0.0.1:8080 ..."
Write-Host "Copy the https://....trycloudflare.com URL for your team."
Write-Host "Press Ctrl+C to stop tunnel (backend keeps running in other window)."
Write-Host ""

& $cloudflared tunnel --url http://127.0.0.1:8080
