[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$searchBase = "http://127.0.0.1:8086"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$fixture = Join-Path $PSScriptRoot "semester-showcase.sql"
$mysqlContainer = "venueflow-base-mysql-1"

if (-not (Test-Path -LiteralPath $fixture)) {
    throw "Semester showcase fixture is missing: $fixture"
}
if (-not (docker ps --format "{{.Names}}" | Where-Object { $_ -eq $mysqlContainer })) {
    throw "Local MySQL container is not running: $mysqlContainer"
}

docker cp $fixture "${mysqlContainer}:/tmp/venueflow-semester-showcase.sql" | Out-Null
docker exec $mysqlContainer sh -c `
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < /tmp/venueflow-semester-showcase.sql'
if ($LASTEXITCODE -ne 0) { throw "Semester showcase SQL failed" }

try {
    $null = Invoke-RestMethod -Method Post -Uri "$searchBase/api/v1/admin/search/rebuild"
} catch {
    Write-Warning "Search rebuild was not completed; event projection may still catch up asynchronously."
}

Write-Host "Synthetic semester showcase data is ready."
