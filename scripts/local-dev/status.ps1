[CmdletBinding()]
param([switch]$Governance)

$ErrorActionPreference = "Stop"

$components = @(
    @{ Name = "Gateway"; Port = 8080 },
    @{ Name = "Auth"; Port = 8081 },
    @{ Name = "User"; Port = 8082 },
    @{ Name = "Resource"; Port = 8083 },
    @{ Name = "Booking"; Port = 8084 },
    @{ Name = "Notification"; Port = 8085 },
    @{ Name = "Search"; Port = 8086 }
)
if ($Governance) {
    $components += @{ Name = "Resource-2"; Port = 18083 }
}

$result = foreach ($component in $components) {
    $uri = "http://127.0.0.1:$($component.Port)/actuator/health/liveness"
    $status = "DOWN"
    try {
        $health = Invoke-RestMethod -Uri $uri -TimeoutSec 2
        if ($health.status -eq "UP") { $status = "UP" }
    } catch {}
    [pscustomobject]@{
        Component = $component.Name
        Port = $component.Port
        Status = $status
    }
}

$result | Format-Table -AutoSize
if ($Governance) {
    $nacosStatus = "DOWN"
    try {
        $nacos = Invoke-RestMethod -Uri "http://127.0.0.1:18080/v3/console/health/liveness" `
            -TimeoutSec 2
        if ($nacos -or $null -eq $nacos) { $nacosStatus = "UP" }
    } catch {}
    Write-Host "Nacos: $nacosStatus"
    if ($nacosStatus -ne "UP") { exit 1 }
}
if (@($result | Where-Object Status -ne "UP").Count -gt 0) { exit 1 }
