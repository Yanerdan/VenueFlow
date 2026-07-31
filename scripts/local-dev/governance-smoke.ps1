[CmdletBinding()]
param([int]$TimeoutSeconds = 75)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$envFile = Join-Path $repoRoot "secrets\local-dev\local-dev.env"
$runDir = Join-Path $repoRoot ".local\venueflow"
$resourcePidFile = Join-Path $runDir "resource-2.pid"
$resourceJar = Join-Path $repoRoot `
    "venueflow-resource-service\target\venueflow-resource-service-0.1.0-SNAPSHOT.jar"

function Import-LocalEnvironment {
    Get-Content $envFile -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
    $publicKey = Join-Path $repoRoot "secrets\local-dev\jwt-public.pem"
    [Environment]::SetEnvironmentVariable(
        "JWT_PUBLIC_KEY", [IO.File]::ReadAllText($publicKey), "Process"
    )
}

function Get-NacosToken {
    $login = Invoke-RestMethod -Method Post `
        -Uri "http://127.0.0.1:$($env:NACOS_HTTP_PORT)/nacos/v3/auth/user/login" `
        -Body @{ username = $env:NACOS_USERNAME; password = $env:NACOS_PASSWORD } `
        -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    if ($login.PSObject.Properties["accessToken"]) { return [string]$login.accessToken }
    return [string]$login.data.accessToken
}

function Get-HealthyResourceInstances([string]$Token) {
    $query = @(
        "serviceName=venueflow-resource-service",
        "groupName=$([Uri]::EscapeDataString($env:VENUEFLOW_NACOS_GROUP))",
        "namespaceId=$([Uri]::EscapeDataString($env:NACOS_NAMESPACE))",
        "healthyOnly=true"
    ) -join "&"
    $response = Invoke-RestMethod `
        -Uri "http://127.0.0.1:$($env:NACOS_HTTP_PORT)/nacos/v3/client/ns/instance/list?$query" `
        -Headers @{ accessToken = $Token } -TimeoutSec 10
    $instances = if ($response.data.PSObject.Properties["hosts"]) {
        $response.data.hosts
    } else {
        $response.data
    }
    return @($instances | Where-Object { $_.healthy -ne $false -and $_.enabled -ne $false })
}

function Wait-ResourceCount([string]$Token, [scriptblock]$Predicate, [string]$Expectation) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $count = @(Get-HealthyResourceInstances $Token).Count
            if (& $Predicate $count) { return }
        } catch {}
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Nacos did not report $Expectation within $TimeoutSeconds seconds"
}

function Assert-GatewayResourceRead {
    $login = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:8080/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body (@{ username = "campus.user"; password = "Campus-User-2026!" } |
            ConvertTo-Json -Compress)
    $traceId = [Guid]::NewGuid().ToString()
    $response = Invoke-WebRequest `
        -Uri "http://127.0.0.1:8080/api/v1/resources?page=0&size=1" `
        -Headers @{
            Authorization = "Bearer $($login.data.accessToken)"
            "X-Trace-Id" = $traceId
        } -TimeoutSec 10
    if ($response.StatusCode -ne 200) { throw "Gateway Resource read did not return HTTP 200" }
    if ($response.Headers["X-Trace-Id"] -ne $traceId) {
        throw "Gateway did not preserve X-Trace-Id"
    }
}

function Start-ResourceTwo {
    $stdout = Join-Path $runDir "resource-2.out.log"
    $stderr = Join-Path $runDir "resource-2.err.log"
    $process = Start-Process -FilePath "java" -ArgumentList @(
        "-Dspring.profiles.active=persistence,cache,resource-events,governance",
        "-Dserver.port=18083",
        "-DVENUEFLOW_INSTANCE_ID=resource-2",
        "-jar",
        $resourceJar
    ) -WorkingDirectory $repoRoot -WindowStyle Hidden `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    [IO.File]::WriteAllText($resourcePidFile, [string]$process.Id, [Text.UTF8Encoding]::new($false))
}

function Wait-ResourceTwoHealthy {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $health = Invoke-RestMethod `
                -Uri "http://127.0.0.1:18083/actuator/health/liveness" -TimeoutSec 2
            if ($health.status -eq "UP") { return }
        } catch {}
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Resource-2 did not recover within $TimeoutSeconds seconds"
}

Import-LocalEnvironment
$token = Get-NacosToken
Wait-ResourceCount $token { param($count) $count -ge 2 } "two healthy Resource instances"
Assert-GatewayResourceRead

$stopped = $false
try {
    if (-not (Test-Path $resourcePidFile)) { throw "Missing managed Resource-2 PID file" }
    $resourcePid = [int][IO.File]::ReadAllText($resourcePidFile)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$resourcePid"
    if (-not $process.CommandLine.Contains("venueflow-resource-service")) {
        throw "Resource-2 PID does not identify a VenueFlow Resource process"
    }
    Stop-Process -Id $resourcePid -Force
    $stopped = $true
    Wait-ResourceCount $token { param($count) $count -eq 1 } "one healthy Resource instance"
    Assert-GatewayResourceRead
    Write-Host "Nacos registration and Resource failover passed."
} finally {
    if ($stopped) {
        Start-ResourceTwo
        Wait-ResourceTwoHealthy
        Wait-ResourceCount $token { param($count) $count -ge 2 } "restored Resource instances"
        Write-Host "Resource-2 was restored."
    }
}
