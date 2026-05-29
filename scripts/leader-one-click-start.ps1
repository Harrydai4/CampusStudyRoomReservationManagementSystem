# One-click launcher for course demo.
# Steps: detect MySQL service -> prompt password if needed -> import full SCAU test database -> start Spring Boot -> print URLs.

$ErrorActionPreference = "Stop"
$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } elseif ($env:CSRRM_SCRIPT_ROOT) { $env:CSRRM_SCRIPT_ROOT } else { Join-Path (Get-Location) "scripts" }
$root = Split-Path -Parent $scriptRoot
Set-Location $root

function Write-Title {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host " CSRRM one-click launcher" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Write-Step([string]$message) {
    Write-Host ""
    Write-Host $message -ForegroundColor Yellow
}

function Require-Command([string]$name, [string]$hint) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Command not found: $name" -ForegroundColor Red
        Write-Host "        $hint" -ForegroundColor Red
        exit 1
    }
}

function ConvertFrom-SecureStringPlain([securestring]$secure) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringAuto($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Get-LocalIPv4Addresses {
    try {
        @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction Stop |
            Where-Object {
                $_.IPAddress -notlike "127.*" -and
                $_.IPAddress -notlike "169.254.*" -and
                $_.IPAddress -notlike "198.18.*" -and
                $_.InterfaceAlias -notmatch "VMware|VirtualBox|Loopback|vEthernet"
            } |
            Select-Object -ExpandProperty IPAddress -Unique)
    } catch {
        @(ipconfig |
            Select-String "IPv4" |
            ForEach-Object { ($_ -split ":\s*", 2)[1].Trim() } |
            Where-Object {
                $_ -and
                $_ -notlike "127.*" -and
                $_ -notlike "169.254.*" -and
                $_ -notlike "198.18.*"
            } |
            Select-Object -Unique)
    }
}

function Test-MysqlLogin([string]$password) {
    $oldPwd = $env:MYSQL_PWD
    try {
        if ($password) {
            $env:MYSQL_PWD = $password
        } else {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
        & mysql -uroot -h 127.0.0.1 -P 3306 -e "SELECT 1;" > $null 2> $null
        return ($LASTEXITCODE -eq 0)
    } finally {
        if ($null -ne $oldPwd) {
            $env:MYSQL_PWD = $oldPwd
        } else {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
    }
}

function Get-ConfiguredMysqlPassword {
    $localProps = Join-Path $root "src\main\resources\application-local.properties"
    if (-not (Test-Path $localProps)) {
        return ""
    }
    $line = Get-Content -LiteralPath $localProps -Encoding UTF8 |
        Where-Object { $_ -match '^\s*spring\.datasource\.password\s*=' } |
        Select-Object -First 1
    if ($line -match '=\s*(.*)$') {
        return $Matches[1].Trim()
    }
    ""
}

Write-Title

if (-not (Test-Path (Join-Path $root "pom.xml"))) {
    Write-Host "[ERROR] pom.xml not found. Run this script from the project root." -ForegroundColor Red
    exit 1
}

Write-Step "[1/5] Check Java and MySQL client"
Require-Command "java" "Install JDK 21 or newer."
Require-Command "mysql" "Install MySQL 8 and add the MySQL bin directory to PATH."

$javaVersionLine = (cmd /c "java -version 2>&1" | Select-Object -First 1)
Write-Host "Java: $javaVersionLine" -ForegroundColor Green
Write-Host "mysql: $((Get-Command mysql).Source)" -ForegroundColor Green

$javaMajor = 0
if ($javaVersionLine -match '"(\d+)') {
    $javaMajor = [int]$Matches[1]
}
$mvnJavaArg = ""
if ($javaMajor -eq 20) {
    $mvnJavaArg = "-Djava.version=20"
    Write-Host "Detected Java 20; using Maven argument: $mvnJavaArg" -ForegroundColor Yellow
} elseif ($javaMajor -gt 0 -and $javaMajor -lt 21) {
    Write-Host "[WARN] JDK 21+ is recommended. Current Java major version: $javaMajor" -ForegroundColor Yellow
}

Write-Step "[2/5] Detect local MySQL service"
$mysqlServices = @(Get-Service -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match 'mysql|mariadb' -or $_.DisplayName -match 'mysql|mariadb' } |
    Sort-Object Status, Name -Descending)

if ($mysqlServices.Count -eq 0) {
    Write-Host "[ERROR] No Windows MySQL/MariaDB service found. Confirm MySQL is installed." -ForegroundColor Red
    exit 1
}

$mysqlService = $mysqlServices | Where-Object { $_.Status -eq "Running" } | Select-Object -First 1
if (-not $mysqlService) {
    $mysqlService = $mysqlServices | Select-Object -First 1
    Write-Host "Found service: $($mysqlService.Name), status: $($mysqlService.Status)" -ForegroundColor Yellow
    try {
        Start-Service -Name $mysqlService.Name -ErrorAction Stop
        Start-Sleep -Seconds 3
        $mysqlService = Get-Service -Name $mysqlService.Name
    } catch {
        Write-Host "[WARN] Could not start MySQL automatically; administrator permission may be required." -ForegroundColor Yellow
        Write-Host "       Start service manually, then press Enter here: $($mysqlService.Name)" -ForegroundColor Yellow
        Read-Host | Out-Null
        $mysqlService = Get-Service -Name $mysqlService.Name
    }
}

if ($mysqlService.Status -ne "Running") {
    Write-Host "[ERROR] MySQL service is still not running: $($mysqlService.Name)" -ForegroundColor Red
    exit 1
}
Write-Host "MySQL service is running: $($mysqlService.Name)" -ForegroundColor Green

Write-Step "[3/5] Clean import full database (drop + database-full.sql)"
$password = if ($env:CSRRM_MYSQL_PASSWORD) { $env:CSRRM_MYSQL_PASSWORD } else { Get-ConfiguredMysqlPassword }
if ($password -and (Test-MysqlLogin $password)) {
    Write-Host "Connected with the configured database password." -ForegroundColor Green
} elseif (Test-MysqlLogin "") {
    $password = ""
    Write-Host "Connected with empty MySQL root password." -ForegroundColor Green
} else {
    do {
        $secure = Read-Host "Enter local MySQL root password (stored locally only)" -AsSecureString
        $password = ConvertFrom-SecureStringPlain $secure
        if (-not (Test-MysqlLogin $password)) {
            Write-Host "Password rejected by MySQL. Please try again." -ForegroundColor Red
            $password = $null
        }
    } while ($null -eq $password)
}

$fullSql = Join-Path $root "docs\06-部署配置\database-full.sql"
if (-not (Test-Path $fullSql)) {
    $cfgDir = Get-ChildItem (Join-Path $root "docs") -Directory |
        Where-Object { $_.Name -match '^06-' } |
        Select-Object -First 1
    if ($cfgDir) {
        $fullSql = Join-Path $cfgDir.FullName "database-full.sql"
    }
}
if (-not (Test-Path $fullSql)) {
    Write-Host "[ERROR] database-full.sql not found." -ForegroundColor Red
    exit 1
}

& (Join-Path $scriptRoot "setup-after-clone.ps1") -MySqlPassword $password -SkipStart -NoBrowser
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Step "[4/5] Start backend"
$listen = netstat -ano | Select-String ":8080\s+.*LISTENING"
if ($listen) {
    Write-Host "Port 8080 is already in use; not starting another backend." -ForegroundColor Yellow
} else {
    $mvnw = Join-Path $root "mvnw.cmd"
    $mvnArgs = @("spring-boot:run")
    if ($mvnJavaArg) {
        $mvnArgs = @($mvnJavaArg) + $mvnArgs
    }
    $process = Start-Process -FilePath $mvnw -ArgumentList $mvnArgs -WorkingDirectory $root -WindowStyle Hidden -PassThru
    Write-Host "Backend PID: $($process.Id)" -ForegroundColor Green

    $started = $false
    for ($i = 1; $i -le 45; $i++) {
        Start-Sleep -Seconds 1
        try {
            $resp = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:8080" -TimeoutSec 2
            if ($resp.StatusCode -eq 200) {
                $started = $true
                break
            }
        } catch {
        }
        Write-Host "." -NoNewline
    }
    Write-Host ""
    if (-not $started) {
        Write-Host "[WARN] http://localhost:8080 did not return 200 yet. Check Spring Boot logs if needed." -ForegroundColor Yellow
    }
}

Write-Step "[5/5] Done"
$localUrl = "http://localhost:8080"
Write-Host "Local URL:" -ForegroundColor Green
Write-Host "  $localUrl" -ForegroundColor Green

$ips = Get-LocalIPv4Addresses
if ($ips.Count -gt 0) {
    Write-Host "LAN URLs:" -ForegroundColor Green
    foreach ($ip in $ips) {
        Write-Host "  http://${ip}:8080" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Login accounts:" -ForegroundColor Cyan
Write-Host "  Admin:   admin / admin123"
Write-Host "  Student: 202225220101 / 123456"
Write-Host ""
Write-Host "Tip: if other devices cannot open the LAN URL, allow Windows Firewall port 8080." -ForegroundColor Yellow

if (-not $env:CSRRM_NONINTERACTIVE) {
    try {
        Start-Process $localUrl
    } catch {
    }
    Read-Host "Press Enter to close this launcher window"
}
