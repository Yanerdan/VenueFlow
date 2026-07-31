[CmdletBinding()]
param([switch]$Infrastructure)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$runDir = Join-Path $repoRoot ".local\venueflow"

if (Test-Path $runDir) {
    Get-ChildItem $runDir -Filter "*.pid" | ForEach-Object {
        $processId = [int][IO.File]::ReadAllText($_.FullName)
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" `
            -ErrorAction SilentlyContinue
        if ($process -and $process.Name -eq "java.exe" -and
            $process.CommandLine -match "com\.yanerdan\.venueflow|venueflow-.*\.jar") {
            Stop-Process -Id $processId -Force
        }
    }
}

if ($Infrastructure) {
    $envFile = Join-Path $repoRoot "secrets\local-dev\local-dev.env"
    if (Test-Path $envFile) {
        & docker compose --env-file (Join-Path $repoRoot "deploy\versions.env") `
            --env-file $envFile -f (Join-Path $repoRoot "deploy\compose\compose.yml") `
            stop mysql redis rabbitmq nacos elasticsearch
        if ($LASTEXITCODE -ne 0) { throw "Failed to stop local infrastructure" }
    }
}

Write-Host "VenueFlow local services stopped."
