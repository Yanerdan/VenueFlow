[CmdletBinding()]
param(
    [string]$EnvironmentFile,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $EnvironmentFile) {
    $EnvironmentFile = Join-Path $repoRoot "secrets\local-dev\local-dev.env"
}
$configDir = Join-Path $repoRoot "deploy\nacos"
$requiredDataIds = @(
    "venueflow-common.yml",
    "venueflow-auth-service.yml",
    "venueflow-user-service.yml",
    "venueflow-resource-service.yml",
    "venueflow-booking-service.yml",
    "venueflow-notification-service.yml",
    "venueflow-search-service.yml",
    "venueflow-gateway.yml"
)

function Import-Environment {
    if (-not (Test-Path $EnvironmentFile)) { throw "Missing environment file: $EnvironmentFile" }
    Get-Content $EnvironmentFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
}

function Assert-BootstrapContract {
    $missing = @($requiredDataIds | Where-Object { -not (Test-Path (Join-Path $configDir $_)) })
    if ($missing.Count -gt 0) { throw "Missing Nacos Data IDs: $($missing -join ', ')" }
    foreach ($secretName in @(
        "MYSQL_ROOT_PASSWORD", "REDIS_PASSWORD", "RABBITMQ_PASSWORD",
        "NACOS_PASSWORD", "NACOS_AUTH_TOKEN", "JWT_PRIVATE_KEY", "JWT_PUBLIC_KEY"
    )) {
        $secret = [Environment]::GetEnvironmentVariable($secretName)
        if ($secret -and $secret.Length -ge 8) {
            foreach ($file in Get-ChildItem $configDir -File) {
                if ([IO.File]::ReadAllText($file.FullName).Contains($secret)) {
                    throw "$($file.Name) contains secret value from $secretName"
                }
            }
        }
    }
}

function Invoke-NacosForm([string]$Method, [string]$Path, [hashtable]$Body, [string]$Token = "") {
    $headers = @{}
    if ($Token) { $headers.accessToken = $Token }
    return Invoke-RestMethod -Method $Method -Uri "$script:nacosBase$Path" `
        -Headers $headers -Body $Body -ContentType "application/x-www-form-urlencoded" `
        -TimeoutSec 15
}

function Get-NacosToken {
    $login = $null
    try {
        $login = Invoke-NacosForm "Post" "/v3/auth/user/login" @{
            username = $env:NACOS_USERNAME
            password = $env:NACOS_PASSWORD
        }
    } catch {}
    $accessToken = if ($login -and $login.PSObject.Properties["accessToken"]) {
        $login.accessToken
    } elseif ($login -and $login.PSObject.Properties["data"] -and
        $login.data.PSObject.Properties["accessToken"]) {
        $login.data.accessToken
    } else { $null }
    if (-not $accessToken) {
        Invoke-NacosForm "Post" "/v3/auth/user/admin" @{
            password = $env:NACOS_PASSWORD
        } | Out-Null
        $login = Invoke-NacosForm "Post" "/v3/auth/user/login" @{
            username = $env:NACOS_USERNAME
            password = $env:NACOS_PASSWORD
        }
        $accessToken = if ($login.PSObject.Properties["accessToken"]) {
            $login.accessToken
        } elseif ($login.PSObject.Properties["data"]) {
            $login.data.accessToken
        } else { $null }
    }
    if (-not $accessToken) { throw "Nacos login did not return an access token" }
    return [string]$accessToken
}

Import-Environment
Assert-BootstrapContract
if ($ValidateOnly) {
    Write-Host "Nacos bootstrap contract is valid."
    exit 0
}

foreach ($required in @(
    "NACOS_HTTP_PORT", "NACOS_USERNAME", "NACOS_PASSWORD", "NACOS_NAMESPACE",
    "VENUEFLOW_NACOS_GROUP"
)) {
    if (-not [Environment]::GetEnvironmentVariable($required)) {
        throw "Missing required environment value: $required"
    }
}

$script:nacosBase = "http://127.0.0.1:$($env:NACOS_HTTP_PORT)/nacos"
$token = Get-NacosToken
$namespaceCheck = Invoke-NacosForm "Get" (
    "/v3/admin/core/namespace/check?namespaceId=$([Uri]::EscapeDataString($env:NACOS_NAMESPACE))"
) @{} $token
if (-not $namespaceCheck.data) {
    $created = Invoke-NacosForm "Post" "/v3/admin/core/namespace" @{
        namespaceId = $env:NACOS_NAMESPACE
        namespaceName = $env:NACOS_NAMESPACE
        namespaceDesc = "VenueFlow local governance"
    } $token
    if (-not $created.data) { throw "Failed to create Nacos namespace" }
}

foreach ($dataId in $requiredDataIds) {
    $content = [IO.File]::ReadAllText((Join-Path $configDir $dataId))
    $published = Invoke-NacosForm "Post" "/v3/admin/cs/config" @{
        namespaceId = $env:NACOS_NAMESPACE
        groupName = $env:VENUEFLOW_NACOS_GROUP
        dataId = $dataId
        content = $content
        type = "yaml"
    } $token
    if (-not $published.data) { throw "Failed to publish Nacos Data ID: $dataId" }
}

Write-Host "Nacos namespace and $($requiredDataIds.Count) Data IDs are ready."
