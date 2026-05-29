# 第三版数据字典验收脚本：检查 MySQL 快照是否符合组长要求
# 用法：.\scripts\verify-v3-dictionary.ps1
#       .\scripts\verify-v3-dictionary.ps1 -Password 123456

param(
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [string]$Database = "study_room_reservation"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    Write-Host "[FAIL] mysql not in PATH" -ForegroundColor Red
    exit 1
}

if (-not $Password) {
    $localProps = Join-Path $root "src\main\resources\application-local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^\s*spring\.datasource\.password\s*=' } | Select-Object -First 1
        if ($line -match '=\s*(.+)$') { $Password = $Matches[1].Trim() }
    }
}
if (-not $Password) {
    $sec = Read-Host "MySQL password for $User" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

$env:MYSQL_PWD = $Password
$mysql = (Get-Command mysql).Source
$fail = 0
$pass = 0

function Test-Check {
    param([string]$Name, [bool]$Ok, [string]$Detail = "")
    if ($Ok) {
        $script:pass++
        Write-Host "[PASS] $Name" -ForegroundColor Green
        if ($Detail) { Write-Host "       $Detail" -ForegroundColor DarkGray }
    } else {
        $script:fail++
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        if ($Detail) { Write-Host "       $Detail" -ForegroundColor Yellow }
    }
}

function Invoke-Query {
    param([string]$Sql)
    $out = & $mysql -h $DbHost -P $Port --protocol=TCP -u $User -N -B $Database -e $Sql 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ($out | Out-String).Trim()
    }
    return $out
}

Write-Host ""
Write-Host "=== CSRRM v3 dictionary verification ===" -ForegroundColor Cyan
Write-Host "Database: $Database @ ${DbHost}:$Port" -ForegroundColor Cyan
Write-Host ""

try {
    $dbExists = & $mysql -h $DbHost -P $Port --protocol=TCP -u $User -N -B -e `
        "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$Database';" 2>&1
    Test-Check "Database exists" ($dbExists -eq "1") "schema=$Database"

    $tableCount = Invoke-Query @"
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA='$Database' AND TABLE_TYPE='BASE TABLE';
"@
    $tableNames = Invoke-Query @"
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA='$Database' AND TABLE_TYPE='BASE TABLE'
ORDER BY TABLE_NAME;
"@
    Test-Check "16 business tables" ($tableCount -eq "16") "actual=$tableCount tables: $($tableNames -join ', ')"
    $hasTempLeave = Invoke-Query @"
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='temp_leave';
"@
    Test-Check "No temp_leave table (v3 removed)" ($hasTempLeave -eq "0")

    $hasFacility = (Invoke-Query @"
SELECT COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME IN ('facility','study_room_facility');
"@) -eq "2"
    Test-Check "facility + study_room_facility exist" $hasFacility

    $fkCount = Invoke-Query @"
SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA='$Database';
"@
    Test-Check "24 foreign keys" ($fkCount -eq "24") "actual=$fkCount"

    $viewCount = Invoke-Query @"
SELECT COUNT(*) FROM information_schema.VIEWS
WHERE TABLE_SCHEMA='$Database' AND TABLE_NAME='v_room_daily_usage';
"@
    Test-Check "View v_room_daily_usage exists" ($viewCount -eq "1")

    $demoUser = Invoke-Query "SELECT COUNT(*) FROM user_account WHERE username='202225220101';"
    Test-Check "Demo student 202225220101 exists" ($demoUser -eq "1") "rows=$demoUser"

    $badStudentNo = Invoke-Query @"
SELECT COUNT(*) FROM student_profile
WHERE student_no IS NULL OR CHAR_LENGTH(student_no) <> 12 OR student_no REGEXP '[^0-9]';
"@
    Test-Check "All student_no are 12-digit numbers" ($badStudentNo -eq "0") "violations=$badStudentNo"

    $badGrade = Invoke-Query @"
SELECT COUNT(*) FROM student_profile
WHERE grade IS NULL OR CHAR_LENGTH(grade) <> 4 OR grade REGEXP '[^0-9]';
"@
    Test-Check "All grade are 4-digit numbers" ($badGrade -eq "0") "violations=$badGrade"

    $badResNo = Invoke-Query @"
SELECT COUNT(*) FROM reservation
WHERE reservation_no IS NULL OR CHAR_LENGTH(reservation_no) <> 16 OR reservation_no REGEXP '[^0-9]';
"@
    Test-Check "All reservation_no are 16-digit numbers" ($badResNo -eq "0") "violations=$badResNo"

    $badCredit = Invoke-Query @"
SELECT COUNT(*) FROM student_profile WHERE credit_score < 0 OR credit_score > 500;
"@
    Test-Check "Credit score in 0-500" ($badCredit -eq "0") "violations=$badCredit"

    $engResStatus = Invoke-Query @"
SELECT COUNT(*) FROM reservation
WHERE status IN ('PENDING','USING','COMPLETED','CANCELLED','VIOLATED');
"@
    Test-Check "Reservation status in Chinese" ($engResStatus -eq "0") "english_rows=$engResStatus"

    $roomCodeLong = Invoke-Query "SELECT COUNT(*) FROM study_room WHERE CHAR_LENGTH(room_code) > 10;"
    Test-Check "room_code length <= 10" ($roomCodeLong -eq "0") "violations=$roomCodeLong"

    $seatNoLong = Invoke-Query "SELECT COUNT(*) FROM seat WHERE CHAR_LENGTH(seat_no) > 10;"
    Test-Check "seat_no length <= 10" ($seatNoLong -eq "0") "violations=$seatNoLong"

    $facilityLinks = Invoke-Query "SELECT COUNT(*) FROM study_room_facility;"
    Test-Check "study_room_facility has data" ([int]$facilityLinks -gt 0) "rows=$facilityLinks"

    $uploadsDir = Join-Path $root "uploads"
    $uploadCount = 0
    if (Test-Path $uploadsDir) {
        Get-ChildItem $uploadsDir -Recurse -File | ForEach-Object {
            if ($_.Name -notin @("README.md", ".gitkeep")) { $uploadCount++ }
        }
    }
    Test-Check "uploads/ has material and layout files" ($uploadCount -ge 50) "files=$uploadCount"

    $staticJs = Get-ChildItem (Join-Path $root "src\main\resources\static\assets\*.js") -ErrorAction SilentlyContinue |
        Select-Object -First 1
    Test-Check "Frontend static bundle exists" ($null -ne $staticJs) $(if ($staticJs) { $staticJs.Name } else { "missing" })

} catch {
    Write-Host "[FAIL] Query error: $_" -ForegroundColor Red
    $fail++
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== Summary: PASS=$pass FAIL=$fail ===" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
if ($fail -eq 0) {
    Write-Host "Database snapshot matches v3 dictionary requirements." -ForegroundColor Green
    exit 0
}
Write-Host "Fix failures above, then re-run: .\组长一键启动.bat" -ForegroundColor Yellow
exit 1
