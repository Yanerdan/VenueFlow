$ErrorActionPreference = "Stop"
$repository = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("venueflow-fault-test-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $testRoot | Out-Null

try {
  $catalog = Get-Content -Raw (Join-Path $PSScriptRoot "scenarios.json") | ConvertFrom-Json
  if ($catalog.scenarios.Count -ne 7) {
    throw "Expected seven required fault scenarios."
  }
  foreach ($scenario in $catalog.scenarios) {
    if ($scenario.timeoutSeconds -le 0 -or $scenario.timeoutSeconds -gt 60) {
      throw "Scenario timeout is outside the bounded range: $($scenario.id)"
    }
    if ([string]::IsNullOrWhiteSpace($scenario.recovery) -or $scenario.target.Contains("*")) {
      throw "Scenario recovery or target is unsafe: $($scenario.id)"
    }
    & (Join-Path $PSScriptRoot "invoke-fault.ps1") `
      -Scenario $scenario.id `
      -EvidenceDirectory $testRoot
  }
  $manifests = Get-ChildItem $testRoot -Filter "*.json"
  if ($manifests.Count -ne 7) {
    throw "Expected one evidence manifest per scenario."
  }
  foreach ($manifest in $manifests) {
    $evidence = Get-Content -Raw $manifest.FullName | ConvertFrom-Json
    if ($evidence.status -ne "PLANNED" -or $evidence.note -ne "No mutation executed.") {
      throw "Dry-run evidence contains an execution claim."
    }
  }
  Write-Output "fault-driver-tests: PASS"
} finally {
  $resolved = (Resolve-Path $testRoot -ErrorAction SilentlyContinue).Path
  if ($resolved -and $resolved.StartsWith([System.IO.Path]::GetTempPath())) {
    Remove-Item -LiteralPath $resolved -Recurse -Force
  }
}
