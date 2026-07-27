param(
  [Parameter(Mandatory = $true)]
  [ValidateSet(
    "notification-consumer-outage",
    "elasticsearch-outage",
    "resource-instance-outage",
    "downstream-latency",
    "duplicate-event",
    "outbox-publisher-outage",
    "redis-failure"
  )]
  [string]$Scenario,
  [switch]$Execute,
  [string]$EnvFile = ".env",
  [string]$EvidenceDirectory = "artifacts/fault-evidence"
)

$ErrorActionPreference = "Stop"
$repository = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$catalog = Get-Content -Raw (Join-Path $PSScriptRoot "scenarios.json") | ConvertFrom-Json
$selected = $catalog.scenarios | Where-Object id -eq $Scenario
if ($null -eq $selected) {
  throw "Unknown scenario: $Scenario"
}

Write-Output "Scenario: $($selected.id)"
Write-Output "Target: $($selected.target)"
Write-Output "Recovery: $($selected.recovery)"

$startedAt = [DateTime]::UtcNow
$status = "PLANNED"
$exitCode = 0

if ($Execute) {
  if ($selected.mode -ne "compose-stop") {
    throw "Scenario '$Scenario' is plan-only because its isolated process fixture is not automated."
  }
  if (-not (Test-Path -LiteralPath (Join-Path $repository $EnvFile))) {
    throw "Execution requires an existing local environment file: $EnvFile"
  }
  if ($selected.target -notin @("elasticsearch", "redis")) {
    throw "Target is not allowlisted for automated mutation: $($selected.target)"
  }
  if ($selected.holdSeconds -gt 30) {
    throw "Fault hold exceeds the 30 second safety bound."
  }

  $compose = @(
    "compose",
    "--env-file", "deploy/versions.env",
    "--env-file", $EnvFile,
    "-f", "deploy/compose/compose.yml",
    "--profile", $selected.profile
  )
  Push-Location $repository
  try {
    & docker @compose stop $selected.target
    if ($LASTEXITCODE -ne 0) {
      throw "Fault mutation failed."
    }
    Start-Sleep -Seconds $selected.holdSeconds
    & docker @compose start $selected.target
    if ($LASTEXITCODE -ne 0) {
      throw "Recovery failed."
    }
    & docker @compose ps --status running $selected.target | Out-Null
    if ($LASTEXITCODE -ne 0) {
      throw "Post-recovery health command failed."
    }
    $status = "EXECUTED"
  } catch {
    $status = "FAILED"
    $exitCode = 1
    Write-Error "$($_.Exception.Message) Recovery: $($selected.recovery)"
  } finally {
    Pop-Location
  }
}

$evidenceRoot =
  if ([System.IO.Path]::IsPathRooted($EvidenceDirectory)) {
    $EvidenceDirectory
  } else {
    Join-Path $repository $EvidenceDirectory
  }
New-Item -ItemType Directory -Path $evidenceRoot -Force | Out-Null
$finishedAt = [DateTime]::UtcNow
$manifest = [ordered]@{
  schemaVersion = 1
  scenario = $selected.id
  target = $selected.target
  status = $status
  startedAtUtc = $startedAt.ToString("o")
  finishedAtUtc = $finishedAt.ToString("o")
  exitCode = $exitCode
  note = if ($status -eq "PLANNED") { "No mutation executed." } else { "Scoped action recorded." }
}
$path = Join-Path $evidenceRoot "$($selected.id)-$($startedAt.ToString('yyyyMMddTHHmmssZ')).json"
$manifest | ConvertTo-Json | Set-Content -Encoding utf8 $path
Write-Output "Evidence: $path"
if ($exitCode -ne 0) {
  exit $exitCode
}
