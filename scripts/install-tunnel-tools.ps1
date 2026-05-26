# Install optional tools for cross-network access (winget)
# Run from project root: .\scripts\install-tunnel-tools.ps1

$ErrorActionPreference = "Continue"
Write-Host "=== Install tunnel tools ===" -ForegroundColor Cyan

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Write-Host "ERR: winget not found. Install App Installer from Microsoft Store." -ForegroundColor Red
    exit 1
}

Write-Host "Installing cloudflared (HTTPS public tunnel, recommended for demo)..."
winget install --id Cloudflare.cloudflared -e --accept-source-agreements --accept-package-agreements

Write-Host ""
Write-Host "Optional: Tailscale (private team VPN, most secure, not public internet)"
Write-Host "  winget install --id Tailscale.Tailscale -e --accept-source-agreements --accept-package-agreements"
Write-Host ""
Write-Host "After install, close and reopen PowerShell, then:"
Write-Host "  .\scripts\start-public-tunnel.ps1"
Write-Host "  or see docs\01-使用指南\08-跨WiFi共用与安全访问.md"
