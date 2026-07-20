[CmdletBinding()]
param(
    [string]$EnvFile = ".env",
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$resolvedEnvFile = if ([System.IO.Path]::IsPathRooted($EnvFile)) { $EnvFile } else { Join-Path $repoRoot $EnvFile }
$versionsFile = Join-Path $repoRoot "deploy/versions.env"
$composeFile = Join-Path $repoRoot "deploy/compose/compose.yml"
$validator = Join-Path $repoRoot "scripts/bootstrap/validate-base-infrastructure.ps1"
$services = @("mysql", "redis", "rabbitmq", "nacos")
$composeArgs = @(
    "compose", "--env-file", $versionsFile, "--env-file", $resolvedEnvFile,
    "-f", $composeFile, "--profile", "base"
)

function Invoke-Compose([string[]]$Arguments) {
    & docker @composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose command failed: $($Arguments[0])"
    }
}

function Show-Diagnostics {
    & docker @composeArgs ps
    foreach ($service in $services) {
        Write-Output "Diagnostics: $service"
        & docker @composeArgs logs --tail 80 $service
    }
}

& $validator -EnvFile $resolvedEnvFile
if ($LASTEXITCODE -ne 0) {
    throw "Base infrastructure preflight failed"
}

$startedAt = Get-Date
try {
    Invoke-Compose @("up", "-d")
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $allHealthy = $true
        foreach ($service in $services) {
            $containerId = (& docker @composeArgs ps -q $service).Trim()
            if (-not $containerId) {
                $allHealthy = $false
                continue
            }
            $status = (& docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $containerId).Trim()
            if ($status -ne "healthy") {
                $allHealthy = $false
            }
        }
        if ($allHealthy) {
            break
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    if (-not $allHealthy) {
        Show-Diagnostics
        throw "Base infrastructure did not become healthy within $TimeoutSeconds seconds"
    }

    Invoke-Compose @("exec", "-T", "mysql", "sh", "-c", 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql --protocol=TCP --host=127.0.0.1 --user=root --batch --skip-column-names --execute=SELECT/**/1 | grep -qx 1')
    Invoke-Compose @("exec", "-T", "redis", "sh", "-c", 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli ping | grep -qx PONG')
    Invoke-Compose @("exec", "-T", "rabbitmq", "rabbitmq-diagnostics", "-q", "check_running")
    Invoke-Compose @("exec", "-T", "nacos", "sh", "-c", 'curl --fail --silent http://127.0.0.1:8080/v3/console/health/liveness >/dev/null')

    $duration = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 1)
    Write-Output "Base infrastructure smoke passed (4/4 healthy, read-only protocols passed, ${duration}s)."
} catch {
    Show-Diagnostics
    throw
}
