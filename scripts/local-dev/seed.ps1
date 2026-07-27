[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$resourceBase = "http://127.0.0.1:8083"
$searchBase = "http://127.0.0.1:8086"

function Invoke-JsonPost([string]$Uri, $Body) {
    return Invoke-RestMethod -Method Post -Uri $Uri -ContentType "application/json; charset=utf-8" `
        -Body ($Body | ConvertTo-Json -Depth 5 -Compress)
}

$categories = @(Invoke-RestMethod -Uri "$resourceBase/api/v1/resource-categories")
$category = $categories | Where-Object { $_.code -eq "CURATED" } | Select-Object -First 1
if (-not $category) {
    $category = Invoke-JsonPost "$resourceBase/api/v1/resource-categories" @{
        code = "CURATED"
        name = "Curated Spaces"
    }
}

$definitions = @(
    @{ No = "VF-DEMO-001"; Name = "Emerald Hall"; Description = "A bright collaboration space for workshops and small launches."; Location = "City House - 2F"; Capacity = 40; DayOffset = 1 },
    @{ No = "VF-DEMO-002"; Name = "Mineral Gallery"; Description = "An architectural gallery for exhibitions, showcases, and brand events."; Location = "City House - 1F"; Capacity = 120; DayOffset = 2 },
    @{ No = "VF-DEMO-003"; Name = "Garden Studio"; Description = "A quiet and flexible studio for creative sessions and team work."; Location = "North Garden - A3"; Capacity = 16; DayOffset = 3 }
)

$page = Invoke-RestMethod -Uri "$resourceBase/api/v1/resources?page=0&size=100"
foreach ($definition in $definitions) {
    $resource = @($page.items) | Where-Object { $_.resourceNo -eq $definition.No } | Select-Object -First 1
    if (-not $resource) {
        $resource = Invoke-JsonPost "$resourceBase/api/v1/resources" @{
            resourceNo = $definition.No
            categoryId = $category.id
            name = $definition.Name
            description = $definition.Description
            location = $definition.Location
            capacity = $definition.Capacity
        }
    }
    if ($resource.status -ne "ACTIVE") {
        $resource = Invoke-RestMethod -Method Patch `
            -Uri "$resourceBase/api/v1/resources/$($resource.id)/status" `
            -ContentType "application/json; charset=utf-8" `
            -Body (@{ targetStatus = "ACTIVE"; expectedVersion = $resource.version } |
                ConvertTo-Json -Compress)
    }

    $from = (Get-Date).ToUniversalTime().AddHours(-1)
    $to = $from.AddDays(30)
    $slotPage = Invoke-RestMethod -Uri (
        "$resourceBase/api/v1/resources/$($resource.id)/slots?from=" +
        [Uri]::EscapeDataString($from.ToString("o")) +
        "&to=" + [Uri]::EscapeDataString($to.ToString("o")) + "&page=0&size=100"
    )
    if (@($slotPage.items).Count -eq 0) {
        $start = (Get-Date).Date.AddDays($definition.DayOffset).AddHours(10).ToUniversalTime()
        $null = Invoke-JsonPost "$resourceBase/api/v1/resources/$($resource.id)/slots" @{
            startAt = $start.ToString("o")
            endAt = $start.AddHours(2).ToString("o")
        }
        $null = Invoke-JsonPost "$resourceBase/api/v1/resources/$($resource.id)/slots" @{
            startAt = $start.AddHours(4).ToString("o")
            endAt = $start.AddHours(7).ToString("o")
        }
    }
}

try {
    $null = Invoke-RestMethod -Method Post -Uri "$searchBase/api/v1/admin/search/rebuild"
} catch {
    Write-Warning "Search rebuild was not completed; event projection may still catch up asynchronously."
}

Write-Host "Demo venues and open slots are ready."
