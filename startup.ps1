# Lay duong dan goc cua script (Fallback ve thu muc hien tai neu chay interactive)
$scriptPath = $PSScriptRoot
if (-not $scriptPath) {
    $scriptPath = $pwd.Path
}

# Kiểm tra Docker trước khi chạy compose
# if (!(docker info > $null 2>&1)) {
#     Write-Host "[!] Docker chưa khởi động! Vui lòng mở Docker Desktop trước." -ForegroundColor Red
#     return
# }

# 1. Khoi dong ha tang Docker
Write-Host ">>> Khoi dong Docker Infrastructure..." -ForegroundColor Green
# Chi chay cac service ben ngoai (infrastructure services)
docker compose -p uniwise up -d mysql redis redisinsight nginx rabbitmq minio ffmpeg elasticsearch kibana
# Chay toan bo cac service (all services)
# docker compose -p uniwise up -d

# 2. Build cac thu vien dung chung (local libs)
Write-Host ">>> Build local libraries..." -ForegroundColor Green
# mvn -f (Join-Path $scriptPath "platforms/java/libs/common/pom.xml") clean install -DskipTests
# mvn -f (Join-Path $scriptPath "platforms/java/libs/grpc-contracts/pom.xml") clean install -DskipTests
# mvn -f (Join-Path $scriptPath "platforms/java/libs/grpc-spring-boot-starter/pom.xml") clean install -DskipTests
# mvn -f (Join-Path $scriptPath "platforms/java/libs/jwt-security-starter/pom.xml") clean install -DskipTests
# mvn -f (Join-Path $scriptPath "platforms/java/libs/platform-event-contract/pom.xml") clean install -DskipTests
# mvn -f (Join-Path $scriptPath "platforms/java/libs/platform-event-starter/pom.xml") clean install -DskipTests

# 3. Khoi chay cac service Spring Boot (Su dung Windows Terminal tab neu co)
Write-Host ">>> Khoi chay cac service..." -ForegroundColor Green

$services = @(
    @{ Path = "gateway"; Title = "Gateway (8080)" },
    @{ Path = "platforms/java/services/identity-service"; Title = "Identity Service (8000)" },
    @{ Path = "platforms/java/services/user-service"; Title = "User Service (8081)" },
    @{ Path = "platforms/java/services/course-service"; Title = "Course Service (8082)" },
    @{ Path = "platforms/java/services/media-service"; Title = "Media Service (8083)" },
    @{ Path = "platforms/java/services/payment-service"; Title = "Payment Service (8085)" },
    @{ Path = "platforms/java/services/search-service"; Title = "Search Service (8086)" },
    @{ Path = "platforms/java/workers/ffmpeg-worker"; Title = "FFmpeg Worker (9900)" }
)


$ps7guid = "574e775e-4f2a-5b96-ac1e-a2962a402336"
$hasWt = Get-Command wt -ErrorAction SilentlyContinue

if ($hasWt) {
    $first = $true
    foreach ($svc in $services) {
        $svcFullPath = Join-Path $scriptPath $svc.Path
        $title = "[Uniwise] " + $svc.Title
        if ($first) {
            wt -w 0 -d "$svcFullPath" -p "$ps7guid" --title "$title" pwsh -NoExit -Command "mvn spring-boot:run"
            $first = $false
        } else {
            wt -w 0 new-tab -d "$svcFullPath" -p "$ps7guid" --title "$title" pwsh -NoExit -Command "mvn spring-boot:run"
        }
        Start-Sleep -Seconds 1
    }
    Write-Host "[+] Da kich hoat toan bo cac tab dich vu trong Windows Terminal!" -ForegroundColor Green
} else {
    foreach ($svc in $services) {
        $svcFullPath = Join-Path $scriptPath $svc.Path
        $title = "[Uniwise] " + $svc.Title
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle='$title'; cd `"$svcFullPath`"; mvn spring-boot:run"
        Start-Sleep -Seconds 2
    }
    Write-Host "[+] Da kich hoat toan bo cac cua so dich vu powershell!" -ForegroundColor Green
}
