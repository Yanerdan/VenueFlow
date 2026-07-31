$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$startScript = [IO.File]::ReadAllText((Join-Path $PSScriptRoot "start.ps1"))
$compose = [IO.File]::ReadAllText((Join-Path $repoRoot "deploy\compose\compose.yml"))
$bootstrap = [IO.File]::ReadAllText((Join-Path $PSScriptRoot "nacos-bootstrap.ps1"))

foreach ($expected in @(
    "[switch]`$Governance",
    "resource-2",
    "VENUEFLOW_INSTANCE_ID",
    "nacos-bootstrap.ps1"
)) {
    if (-not $startScript.Contains($expected)) { throw "start.ps1 is missing contract: $expected" }
}
foreach ($expected in @(
    'NACOS_AUTH_ADMIN_ENABLE: "true"',
    'NACOS_AUTH_CONSOLE_ENABLE: "true"',
    "NACOS_AUTH_SYSTEM_TYPE: nacos"
)) {
    if (-not $compose.Contains($expected)) { throw "Compose is missing contract: $expected" }
}
foreach ($expected in @(
    "/v3/auth/user/admin",
    "/v3/auth/user/login",
    "/v3/admin/core/namespace",
    "/v3/admin/cs/config"
)) {
    if (-not $bootstrap.Contains($expected)) { throw "Bootstrap is missing API: $expected" }
}

& (Join-Path $PSScriptRoot "nacos-bootstrap.ps1") -ValidateOnly
if ($LASTEXITCODE -ne 0) { throw "Nacos bootstrap validation failed" }
Write-Host "Governance script contracts passed."
