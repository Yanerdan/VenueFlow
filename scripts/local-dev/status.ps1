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
if (@($result | Where-Object Status -ne "UP").Count -gt 0) { exit 1 }
