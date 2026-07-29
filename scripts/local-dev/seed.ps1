[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$searchBase = "http://127.0.0.1:8086"
$gatewayBase = "http://127.0.0.1:8080"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$fixture = Join-Path $PSScriptRoot "semester-showcase.sql"
$mysqlContainer = "venueflow-base-mysql-1"
$redisContainer = "venueflow-base-redis-1"
$demoUsername = "campus.user"
$demoPassword = "Campus-User-2026!"

if (-not (Test-Path -LiteralPath $fixture)) {
    throw "Semester showcase fixture is missing: $fixture"
}
if (-not (docker ps --format "{{.Names}}" | Where-Object { $_ -eq $mysqlContainer })) {
    throw "Local MySQL container is not running: $mysqlContainer"
}

try {
    Invoke-RestMethod -Method Post -Uri "$gatewayBase/api/v1/auth/register" `
        -ContentType "application/json" `
        -Body (@{ username = $demoUsername; password = $demoPassword } | ConvertTo-Json) | Out-Null
    Write-Host "Created local applicant account: $demoUsername"
} catch {
    $statusCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
    if ($statusCode -ne 409) {
        Write-Warning "Applicant account was not provisioned through Gateway; the semester fixture will still be loaded."
    }
}

docker cp $fixture "${mysqlContainer}:/tmp/venueflow-semester-showcase.sql" | Out-Null
docker exec $mysqlContainer sh -c `
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < /tmp/venueflow-semester-showcase.sql'
if ($LASTEXITCODE -ne 0) { throw "Semester showcase SQL failed" }

if (docker ps --format "{{.Names}}" | Where-Object { $_ -eq $redisContainer }) {
    docker exec $redisContainer sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli FLUSHDB >/dev/null'
    if ($LASTEXITCODE -ne 0) { Write-Warning "Local Redis cache was not cleared after reseeding." }
}

try {
    $null = Invoke-RestMethod -Method Post -Uri "$searchBase/api/v1/admin/search/rebuild"
} catch {
    Write-Warning "Search rebuild was not completed; event projection may still catch up asynchronously."
}

Write-Host "Synthetic semester showcase data is ready."
Write-Host "Applicant: $demoUsername / $demoPassword (local demo only)"
