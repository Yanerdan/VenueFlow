[CmdletBinding()]
param(
    [string]$EnvFile = ".env.example",
    [string]$VersionsFile = "deploy/versions.env",
    [switch]$AllowPlaceholders
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$composeFile = Join-Path $repoRoot "deploy/compose/compose.yml"

function Resolve-RepositoryPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return Join-Path $repoRoot $Path
}

function Read-EnvironmentFile([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -Encoding UTF8 $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -ne 2 -or -not $parts[0]) {
            throw "Invalid environment assignment in $Path"
        }
        $values[$parts[0]] = $parts[1]
    }
    return $values
}

function Assert-LastExitCode([string]$Operation) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Operation failed with exit code $LASTEXITCODE"
    }
}

$resolvedEnvFile = Resolve-RepositoryPath $EnvFile
$resolvedVersionsFile = Resolve-RepositoryPath $VersionsFile
foreach ($path in @($composeFile, $resolvedEnvFile, $resolvedVersionsFile)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required file not found: $path"
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is required"
}
$composeVersion = (& docker compose version --short).TrimStart("v")
Assert-LastExitCode "docker compose version"
$composeMajor = [int]($composeVersion.Split(".")[0])
if ($composeMajor -lt 2) {
    throw "Docker Compose v2 or newer is required"
}

$values = Read-EnvironmentFile $resolvedVersionsFile
foreach ($entry in (Read-EnvironmentFile $resolvedEnvFile).GetEnumerator()) {
    $values[$entry.Key] = $entry.Value
}

$required = @(
    "MYSQL_IMAGE", "REDIS_IMAGE", "RABBITMQ_IMAGE", "NACOS_IMAGE",
    "INFRA_BIND_ADDRESS", "MYSQL_PORT", "REDIS_PORT", "RABBITMQ_PORT",
    "RABBITMQ_MANAGEMENT_PORT", "NACOS_HTTP_PORT", "NACOS_GRPC_PORT",
    "NACOS_GRPC_TLS_PORT", "MYSQL_ROOT_PASSWORD", "REDIS_PASSWORD",
    "RABBITMQ_USERNAME", "RABBITMQ_PASSWORD", "NACOS_AUTH_ENABLE",
    "NACOS_AUTH_TOKEN", "NACOS_AUTH_IDENTITY_KEY", "NACOS_AUTH_IDENTITY_VALUE"
)
foreach ($name in $required) {
    if (-not $values.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($values[$name])) {
        throw "Required variable is missing: $name"
    }
}

if (-not $AllowPlaceholders) {
    foreach ($name in $required | Where-Object { $_ -notlike "*_IMAGE" -and $_ -notlike "*_PORT" -and $_ -ne "INFRA_BIND_ADDRESS" -and $_ -ne "NACOS_AUTH_ENABLE" }) {
        if ($values[$name] -match "^(replace-with|change-me|placeholder)") {
            throw "Required variable still contains a placeholder: $name"
        }
    }
}

if ($values["INFRA_BIND_ADDRESS"] -in @("0.0.0.0", "::", "[::]", "*")) {
    throw "INFRA_BIND_ADDRESS must not bind every host interface"
}
if ($values["NACOS_AUTH_ENABLE"] -ne "true") {
    throw "NACOS authentication must be enabled"
}
foreach ($name in $required | Where-Object { $_ -like "*_PORT" }) {
    $parsedPort = 0
    if (-not [int]::TryParse($values[$name], [ref]$parsedPort) -or $parsedPort -lt 1 -or $parsedPort -gt 65535) {
        throw "Host port must be an integer from 1 to 65535: $name"
    }
}

$imagePatterns = @{
    MYSQL_IMAGE = '^mysql:8\.4\.\d+(?:-[A-Za-z0-9._-]+)?$'
    REDIS_IMAGE = '^redis:7\.4\.\d+(?:-[A-Za-z0-9._-]+)?$'
    RABBITMQ_IMAGE = '^rabbitmq:4\.1\.\d+-management(?:-[A-Za-z0-9._-]+)?$'
    NACOS_IMAGE = '^nacos/nacos-server:v3\.1\.1(?:-[A-Za-z0-9._-]+)?$'
}
foreach ($name in $imagePatterns.Keys) {
    if ($values[$name] -notmatch $imagePatterns[$name] -or $values[$name] -match ':latest$') {
        throw "Image variable must use an approved exact tag: $name"
    }
}

$composeArgs = @(
    "compose", "--env-file", $resolvedVersionsFile, "--env-file", $resolvedEnvFile,
    "-f", $composeFile, "--profile", "base"
)
$configOutput = & docker @composeArgs config --format json
Assert-LastExitCode "docker compose config"
$config = $configOutput | ConvertFrom-Json

$actualServices = @($config.services.PSObject.Properties.Name | Sort-Object)
$expectedServices = @("mysql", "nacos", "rabbitmq", "redis")
if (Compare-Object $expectedServices $actualServices) {
    throw "The base profile must contain exactly mysql, nacos, rabbitmq and redis"
}

$totalMemory = 0L
foreach ($serviceName in $expectedServices) {
    $service = $config.services.$serviceName
    if (@($service.profiles) -notcontains "base") {
        throw "Service $serviceName is not scoped to the base profile"
    }
    if (-not $service.healthcheck -or -not $service.healthcheck.test -or
        -not $service.healthcheck.interval -or -not $service.healthcheck.timeout -or
        -not $service.healthcheck.retries -or -not $service.healthcheck.start_period) {
        throw "Service $serviceName must define a bounded healthcheck"
    }
    if (-not $service.mem_limit -or -not $service.cpus) {
        throw "Service $serviceName must define CPU and memory limits"
    }
    $totalMemory += [int64]$service.mem_limit
    if (-not $service.volumes) {
        throw "Service $serviceName must use a named volume"
    }
    foreach ($port in @($service.ports)) {
        if ($port.host_ip -ne $values["INFRA_BIND_ADDRESS"]) {
            throw "Service $serviceName has a port without the explicit bind address"
        }
    }
}
if ($totalMemory -gt 5GB) {
    throw "Configured base-profile memory exceeds 5GB"
}

$nacosHealthCommand = @($config.services.nacos.healthcheck.test) -join " "
if ($nacosHealthCommand -notmatch 'http://127\.0\.0\.1:8080/v3/console/health/liveness') {
    throw "Nacos healthcheck must target the container's internal console listener on port 8080"
}

$expectedVolumes = @("mysql-data", "nacos-data", "rabbitmq-data", "redis-data")
$actualVolumes = @($config.volumes.PSObject.Properties.Name | Sort-Object)
if (Compare-Object $expectedVolumes $actualVolumes) {
    throw "Compose must define exactly four component data volumes"
}

$automationFiles = @(
    (Join-Path $repoRoot ".github/workflows/ci.yml"),
    (Join-Path $repoRoot "scripts/smoke-test/base-infrastructure-smoke.ps1"),
    (Join-Path $repoRoot "scripts/smoke-test/base-infrastructure-smoke.sh")
) | Where-Object { Test-Path -LiteralPath $_ }
$destructivePattern = 'down\s+.*(--volumes|-v(?:\s|$))'
if ($automationFiles -and (Select-String -Path $automationFiles -Pattern $destructivePattern)) {
    throw "Automated infrastructure commands must not delete volumes"
}

Write-Output "Base infrastructure static validation passed (4 services, bounded health/resources, safe bind)."
