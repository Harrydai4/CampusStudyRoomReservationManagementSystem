# Team-only access via Tailscale (different WiFi, NOT public internet — safest)
# 1. Install Tailscale on your PC and each teammate's phone/PC
# 2. Same Tailscale account / same tailnet (invite links)
# 3. Run backend: .\scripts\start-shared-server.ps1
# 4. Run this script to print the URL teammates should open

$ErrorActionPreference = "Continue"
Write-Host "=== CSRRM via Tailscale (private team network) ===" -ForegroundColor Cyan
Write-Host ""

$ts = Get-Command tailscale -ErrorAction SilentlyContinue
if (-not $ts) {
    Write-Host "Tailscale not installed. Install:" -ForegroundColor Yellow
    Write-Host "  winget install --id Tailscale.Tailscale -e"
    Write-Host "  Or: https://tailscale.com/download/windows"
    Write-Host ""
    Write-Host "Each teammate installs Tailscale and joins your tailnet (invite in admin console)."
    exit 1
}

$status = & tailscale status 2>&1
Write-Host $status
Write-Host ""

$ip = & tailscale ip -4 2>&1
if ($ip -match "^\d+\.\d+\.\d+\.\d+$") {
    Write-Host "Team access URL (Tailscale connected devices only):" -ForegroundColor Green
    Write-Host "  http://${ip}:8080" -ForegroundColor Green
} else {
    Write-Host "WARN: Tailscale not connected. Log in to Tailscale app first." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Steps:"
Write-Host "  1. You: .\scripts\start-shared-server.ps1"
Write-Host "  2. Teammates: install Tailscale, accept invite, open URL above"
Write-Host "  3. MySQL stays on 127.0.0.1 — never share port 3306 publicly"
