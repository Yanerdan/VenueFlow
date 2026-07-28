[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$SkipSeed
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$composeFile = Join-Path $repoRoot "deploy\compose\compose.yml"
$versionsFile = Join-Path $repoRoot "deploy\versions.env"
$secretDir = Join-Path $repoRoot "secrets\local-dev"
$envFile = Join-Path $secretDir "local-dev.env"
$privateKeyFile = Join-Path $secretDir "jwt-private.pem"
$publicKeyFile = Join-Path $secretDir "jwt-public.pem"
$runDir = Join-Path $repoRoot ".local\venueflow"

function New-RandomSecret([int]$Bytes = 24) {
    $value = New-Object byte[] $Bytes
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($value)
        return [Convert]::ToBase64String($value).TrimEnd("=").Replace("+", "-").Replace("/", "_")
    } finally {
        $generator.Dispose()
    }
}

function Initialize-LocalSecrets {
    New-Item -ItemType Directory -Force -Path $secretDir, $runDir | Out-Null
    if (-not (Test-Path $envFile)) {
        $rootPassword = New-RandomSecret
        $redisPassword = New-RandomSecret
        $rabbitPassword = New-RandomSecret
        $nacosToken = New-RandomSecret 32
        $servicePasswords = @{
            Auth = New-RandomSecret
            User = New-RandomSecret
            Resource = New-RandomSecret
            Booking = New-RandomSecret
            Notification = New-RandomSecret
        }
        $content = @"
INFRA_BIND_ADDRESS=127.0.0.1
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=$rootPassword
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=$redisPassword
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672
RABBITMQ_USERNAME=venueflow
RABBITMQ_PASSWORD=$rabbitPassword
RABBITMQ_VHOST=/
VENUEFLOW_RABBITMQ_HOST=127.0.0.1
VENUEFLOW_RABBITMQ_PORT=5672
VENUEFLOW_RABBITMQ_USERNAME=venueflow
VENUEFLOW_RABBITMQ_PASSWORD=$rabbitPassword
VENUEFLOW_RABBITMQ_VHOST=/
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=$nacosToken
NACOS_AUTH_IDENTITY_KEY=venueflow-local-key
NACOS_AUTH_IDENTITY_VALUE=$(New-RandomSecret 16)
NACOS_HTTP_PORT=8848
NACOS_GRPC_PORT=9848
NACOS_GRPC_TLS_PORT=9849
ELASTICSEARCH_URI=http://127.0.0.1:9200
ELASTICSEARCH_PORT=9200
RESOURCE_SERVICE_BASE_URI=http://127.0.0.1:8083
VENUEFLOW_ENV=local
VENUEFLOW_AUTH_DB_URL=jdbc:mysql://127.0.0.1:3306/venueflow_auth
VENUEFLOW_AUTH_DB_USERNAME=vf_auth
VENUEFLOW_AUTH_DB_PASSWORD=$($servicePasswords.Auth)
VENUEFLOW_USER_DB_URL=jdbc:mysql://127.0.0.1:3306/venueflow_user
VENUEFLOW_USER_DB_USERNAME=vf_user
VENUEFLOW_USER_DB_PASSWORD=$($servicePasswords.User)
VENUEFLOW_RESOURCE_DB_URL=jdbc:mysql://127.0.0.1:3306/venueflow_resource
VENUEFLOW_RESOURCE_DB_USERNAME=vf_resource
VENUEFLOW_RESOURCE_DB_PASSWORD=$($servicePasswords.Resource)
VENUEFLOW_BOOKING_DB_URL=jdbc:mysql://127.0.0.1:3306/venueflow_booking
VENUEFLOW_BOOKING_DB_USERNAME=vf_booking
VENUEFLOW_BOOKING_DB_PASSWORD=$($servicePasswords.Booking)
VENUEFLOW_NOTIFICATION_DB_URL=jdbc:mysql://127.0.0.1:3306/venueflow_notification
VENUEFLOW_NOTIFICATION_DB_USERNAME=vf_notification
VENUEFLOW_NOTIFICATION_DB_PASSWORD=$($servicePasswords.Notification)
VENUEFLOW_USER_SERVICE_BASE_URL=http://127.0.0.1:8082
VENUEFLOW_RESOURCE_SERVICE_BASE_URL=http://127.0.0.1:8083
VENUEFLOW_GATEWAY_AUTH_URI=http://127.0.0.1:8081
VENUEFLOW_GATEWAY_USER_URI=http://127.0.0.1:8082
VENUEFLOW_GATEWAY_RESOURCE_URI=http://127.0.0.1:8083
VENUEFLOW_GATEWAY_BOOKING_URI=http://127.0.0.1:8084
VENUEFLOW_GATEWAY_NOTIFICATION_URI=http://127.0.0.1:8085
VENUEFLOW_GATEWAY_SEARCH_URI=http://127.0.0.1:8086
VENUEFLOW_GATEWAY_ALLOWED_ORIGINS=http://127.0.0.1:3000
VENUEFLOW_AUTH_ISSUER=venueflow-auth-service
VENUEFLOW_BOOTSTRAP_ADMIN_USERNAME=campus.admin
VENUEFLOW_BOOTSTRAP_ADMIN_PASSWORD=Campus-Admin-2026!
VENUEFLOW_OUTBOX_ENABLED=true
"@
        [IO.File]::WriteAllText($envFile, $content, [Text.UTF8Encoding]::new($false))
    }

    if (-not (Test-Path $privateKeyFile) -or -not (Test-Path $publicKeyFile)) {
        $openssl = (Get-Command openssl -ErrorAction SilentlyContinue).Source
        if (-not $openssl) { throw "OpenSSL is required to generate local JWT keys" }
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $privateKeyFile 2>$null
        $privateExitCode = $LASTEXITCODE
        & $openssl pkey -in $privateKeyFile -pubout -out $publicKeyFile 2>$null
        $publicExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorPreference
        if ($privateExitCode -ne 0) { throw "Failed to generate local JWT private key" }
        if ($publicExitCode -ne 0) { throw "Failed to generate local JWT public key" }
    }
}

function Import-EnvironmentFile {
    Get-Content $envFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
    [Environment]::SetEnvironmentVariable(
        "JWT_PRIVATE_KEY", [IO.File]::ReadAllText($privateKeyFile), "Process"
    )
    [Environment]::SetEnvironmentVariable(
        "JWT_PUBLIC_KEY", [IO.File]::ReadAllText($publicKeyFile), "Process"
    )
}

function Set-LocalEnvironmentValue([string]$Name, [string]$Value) {
    $lines = [Collections.Generic.List[string]]::new()
    Get-Content $envFile -Encoding utf8 | ForEach-Object { $lines.Add($_) }
    $updated = $false
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index].StartsWith("$Name=")) {
            $lines[$index] = "$Name=$Value"
            $updated = $true
            break
        }
    }
    if (-not $updated) { $lines.Add("$Name=$Value") }
    [IO.File]::WriteAllLines($envFile, $lines, [Text.UTF8Encoding]::new($false))
    [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
}

function Resolve-MySqlPort {
    $configuredPort = [int]$env:MYSQL_PORT
    $composeMySqlRunning = (& docker ps --filter "name=^venueflow-base-mysql-1$" --format "{{.Names}}") `
        -eq "venueflow-base-mysql-1"
    $occupied = [bool](Get-NetTCPConnection -LocalPort $configuredPort -State Listen `
        -ErrorAction SilentlyContinue)
    if ($occupied -and -not $composeMySqlRunning) {
        $availablePort = 3307..3315 | Where-Object {
            -not (Get-NetTCPConnection -LocalPort $_ -State Listen -ErrorAction SilentlyContinue)
        } | Select-Object -First 1
        if (-not $availablePort) { throw "No free local MySQL port was found in 3307..3315" }
        Set-LocalEnvironmentValue "MYSQL_PORT" ([string]$availablePort)
        foreach ($service in "AUTH", "USER", "RESOURCE", "BOOKING", "NOTIFICATION") {
            $schema = "venueflow_$($service.ToLowerInvariant())"
            Set-LocalEnvironmentValue "VENUEFLOW_${service}_DB_URL" `
                "jdbc:mysql://127.0.0.1:$availablePort/$schema"
        }
        Write-Host "Host port $configuredPort is occupied; VenueFlow MySQL will use $availablePort."
    }
}

function Invoke-Compose([string[]]$Arguments) {
    & docker compose --env-file $versionsFile --env-file $envFile -f $composeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed: $($Arguments -join ' ')"
    }
}

function Wait-ContainerHealthy([string]$Name, [int]$TimeoutSeconds = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = (& docker inspect --format "{{.State.Health.Status}}" $Name 2>$null)
        if ($status -eq "healthy") { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "$Name did not become healthy within $TimeoutSeconds seconds"
}

function Initialize-Databases {
    $rootPassword = [Environment]::GetEnvironmentVariable("MYSQL_ROOT_PASSWORD")
    $databases = @(
        @{ Schema = "venueflow_auth"; User = "vf_auth"; Password = $env:VENUEFLOW_AUTH_DB_PASSWORD },
        @{ Schema = "venueflow_user"; User = "vf_user"; Password = $env:VENUEFLOW_USER_DB_PASSWORD },
        @{ Schema = "venueflow_resource"; User = "vf_resource"; Password = $env:VENUEFLOW_RESOURCE_DB_PASSWORD },
        @{ Schema = "venueflow_booking"; User = "vf_booking"; Password = $env:VENUEFLOW_BOOKING_DB_PASSWORD },
        @{ Schema = "venueflow_notification"; User = "vf_notification"; Password = $env:VENUEFLOW_NOTIFICATION_DB_PASSWORD }
    )
    $statements = foreach ($database in $databases) {
        "CREATE DATABASE IF NOT EXISTS ``$($database.Schema)`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
        "CREATE USER IF NOT EXISTS '$($database.User)'@'%' IDENTIFIED BY '$($database.Password)';"
        "ALTER USER '$($database.User)'@'%' IDENTIFIED BY '$($database.Password)';"
        "GRANT ALL PRIVILEGES ON ``$($database.Schema)``.* TO '$($database.User)'@'%';"
    }
    $statements += "ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';"
    $sql = ($statements + "FLUSH PRIVILEGES;") -join [Environment]::NewLine
    $candidatePasswords = [Collections.Generic.List[string]]::new()
    $candidatePasswords.Add($rootPassword)
    $legacyEnvFile = Join-Path $repoRoot ".env"
    if (Test-Path $legacyEnvFile) {
        $legacyLine = Get-Content $legacyEnvFile -Encoding utf8 |
            Where-Object { $_ -match "^MYSQL_ROOT_PASSWORD=" } | Select-Object -First 1
        if ($legacyLine) {
            $legacyPassword = $legacyLine.Split("=", 2)[1]
            if ($legacyPassword -and -not $candidatePasswords.Contains($legacyPassword)) {
                $candidatePasswords.Add($legacyPassword)
            }
        }
    }
    $previousErrorPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    foreach ($candidate in $candidatePasswords) {
        $sql | & docker exec -i -e "MYSQL_PWD=$candidate" venueflow-base-mysql-1 mysql -uroot 2>$null
        if ($LASTEXITCODE -eq 0) {
            $ErrorActionPreference = $previousErrorPreference
            return
        }
    }
    $ErrorActionPreference = $previousErrorPreference
    throw "Local database initialization failed: the existing MySQL volume uses an unknown root password"
}

function Stop-StaleVenueFlowProcesses {
    foreach ($port in 8080..8086) {
        $listeners = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
        foreach ($listener in $listeners) {
            $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)" `
                -ErrorAction SilentlyContinue
            if ($process -and $process.Name -eq "java.exe" -and
                $process.CommandLine -match "com\.yanerdan\.venueflow|venueflow-.*\.jar") {
                Stop-Process -Id $listener.OwningProcess -Force
            } else {
                throw "Port $port is already occupied by a non-VenueFlow process"
            }
        }
    }
}

function Start-ServiceProcess($Service) {
    $jar = Join-Path $repoRoot $Service.Jar
    if (-not (Test-Path $jar)) { throw "Missing executable jar: $jar" }
    $stdout = Join-Path $runDir "$($Service.Name).out.log"
    $stderr = Join-Path $runDir "$($Service.Name).err.log"
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-Dspring.profiles.active=$($Service.Profiles)", "-jar", $jar) `
        -WorkingDirectory $repoRoot -WindowStyle Hidden `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    [IO.File]::WriteAllText(
        (Join-Path $runDir "$($Service.Name).pid"),
        [string]$process.Id,
        [Text.UTF8Encoding]::new($false)
    )
}

function Wait-ServiceHealthy($Service, [int]$TimeoutSeconds = 90) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $uri = "http://127.0.0.1:$($Service.Port)/actuator/health/liveness"
    do {
        try {
            $response = Invoke-RestMethod -Uri $uri -TimeoutSec 2
            if ($response.status -eq "UP") { return }
        } catch {
            Start-Sleep -Seconds 2
        }
        $pidFile = Join-Path $runDir "$($Service.Name).pid"
        if (Test-Path $pidFile) {
            $processId = [int][IO.File]::ReadAllText($pidFile)
            if (-not (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
                $errorLog = Join-Path $runDir "$($Service.Name).err.log"
                $tail = if (Test-Path $errorLog) {
                    (Get-Content $errorLog -Tail 25 -ErrorAction SilentlyContinue) -join [Environment]::NewLine
                } else { "No error log was written." }
                throw "$($Service.Name) exited during startup.`n$tail"
            }
        }
    } while ((Get-Date) -lt $deadline)
    throw "$($Service.Name) did not become healthy at $uri"
}

Set-Location $repoRoot
Initialize-LocalSecrets
Import-EnvironmentFile
Set-LocalEnvironmentValue "VENUEFLOW_BOOTSTRAP_ADMIN_USERNAME" "campus.admin"
Set-LocalEnvironmentValue "VENUEFLOW_BOOTSTRAP_ADMIN_PASSWORD" "Campus-Admin-2026!"
Resolve-MySqlPort
Stop-StaleVenueFlowProcesses

Write-Host "Starting local infrastructure..."
Invoke-Compose @("up", "-d", "mysql", "redis", "rabbitmq", "elasticsearch")
Wait-ContainerHealthy "venueflow-base-mysql-1"
Wait-ContainerHealthy "venueflow-base-redis-1"
Wait-ContainerHealthy "venueflow-base-rabbitmq-1"
Wait-ContainerHealthy "venueflow-base-elasticsearch-1"
Initialize-Databases

if (-not $SkipBuild) {
    Write-Host "Building executable services..."
    & (Join-Path $repoRoot "mvnw.cmd") -DskipTests package --no-transfer-progress
    if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
}

$services = @(
    [pscustomobject]@{ Name = "auth"; Port = 8081; Profiles = "persistence"; Jar = "venueflow-auth-service\target\venueflow-auth-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "user"; Port = 8082; Profiles = "persistence"; Jar = "venueflow-user-service\target\venueflow-user-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "resource"; Port = 8083; Profiles = "persistence,cache,resource-events"; Jar = "venueflow-resource-service\target\venueflow-resource-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "booking"; Port = 8084; Profiles = "persistence,messaging"; Jar = "venueflow-booking-service\target\venueflow-booking-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "notification"; Port = 8085; Profiles = "persistence,messaging"; Jar = "venueflow-notification-service\target\venueflow-notification-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "search"; Port = 8086; Profiles = "search"; Jar = "venueflow-search-service\target\venueflow-search-service-0.1.0-SNAPSHOT.jar" },
    [pscustomobject]@{ Name = "gateway"; Port = 8080; Profiles = "gateway"; Jar = "venueflow-gateway\target\venueflow-gateway-0.1.0-SNAPSHOT.jar" }
)

Write-Host "Starting VenueFlow services..."
foreach ($service in $services) { Start-ServiceProcess $service }
foreach ($service in $services) {
    Wait-ServiceHealthy $service
    Write-Host ("  {0,-13} UP on {1}" -f $service.Name, $service.Port)
}

if (-not $SkipSeed) {
    & (Join-Path $PSScriptRoot "seed.ps1")
    if ($LASTEXITCODE -ne 0) { throw "Demo data seed failed" }
}

Write-Host ""
Write-Host "VenueFlow local stack is ready."
Write-Host "Gateway:  http://127.0.0.1:8080"
Write-Host "Frontend: python -m http.server 3000 --directory venueflow-web"
Write-Host "Admin:    campus.admin / Campus-Admin-2026! (local demo only)"
