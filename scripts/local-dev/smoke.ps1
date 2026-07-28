[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$gateway = "http://127.0.0.1:8080"
$username = "acceptance_$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
$password = "Local-Acceptance-2026!"

function Invoke-Json([string]$Method, [string]$Uri, $Body, $Headers = @{}) {
    $parameters = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 6 -Compress
    }
    try {
        return Invoke-RestMethod @parameters
    } catch {
        $status = if ($_.Exception.Response) {
            [int]$_.Exception.Response.StatusCode
        } else {
            "connection-error"
        }
        $details = if ($_.ErrorDetails.Message) {
            $_.ErrorDetails.Message
        } else {
            $_.Exception.Message
        }
        throw "$Method $Uri failed with $status`: $details"
    }
}

$registration = Invoke-Json "Post" "$gateway/api/v1/auth/register" @{
    username = $username
    password = $password
}
$login = Invoke-Json "Post" "$gateway/api/v1/auth/login" @{
    username = $username
    password = $password
}
$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token" }
$adminLogin = Invoke-Json "Post" "$gateway/api/v1/auth/login" @{
    username = "campus.admin"
    password = "Campus-Admin-2026!"
}
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.data.accessToken)" }

$profile = Invoke-Json "Post" "$gateway/api/v1/users" @{
    externalUserId = [string]$registration.data.userId
    displayName = "Local Acceptance"
    campusId = "VF-$($registration.data.userId)"
    identityType = "STUDENT"
    department = "Computer Science"
    phone = "13800000000"
    email = "$username@example.edu.cn"
} $headers

$profile = Invoke-Json "Patch" "$gateway/api/v1/users/me/campus-profile" @{
    displayName = "Local Acceptance"
    campusId = "VF-$($registration.data.userId)"
    identityType = "STUDENT"
    department = "Computer Science"
    phone = "13800000000"
    email = "$username@example.edu.cn"
    expectedVersion = $profile.version
} $headers

$directory = Invoke-Json "Get" `
    "$gateway/api/v1/users/management?keyword=Local%20Acceptance&pageNumber=0&pageSize=100" `
    $null $adminHeaders
if (-not (@($directory.items) | Where-Object { $_.id -eq $profile.id })) {
    throw "Created profile did not appear in the management user directory"
}

$resources = Invoke-RestMethod -Uri "$gateway/api/v1/resources?page=0&size=100" -Headers $headers
$resource = @($resources.items) |
    Where-Object { $_.resourceNo -eq "VF-DEMO-001" } |
    Select-Object -First 1
if (-not $resource) { throw "Demo resource VF-DEMO-001 is missing" }

$from = [Uri]::EscapeDataString((Get-Date).ToUniversalTime().AddHours(-1).ToString("o"))
$to = [Uri]::EscapeDataString((Get-Date).ToUniversalTime().AddDays(30).ToString("o"))
$slots = Invoke-RestMethod `
    -Uri "$gateway/api/v1/resources/$($resource.id)/slots?from=$from&to=$to&page=0&size=100" `
    -Headers $headers
$slot = @($slots.items) | Where-Object { $_.status -eq "OPEN" } | Select-Object -First 1
if (-not $slot) { throw "Demo resource has no open slot" }

$bookingHeaders = @{
    Authorization = "Bearer $token"
    "Idempotency-Key" = [Guid]::NewGuid().ToString()
}
$created = Invoke-Json "Post" "$gateway/api/v1/bookings" @{
    userId = $profile.id
    slotId = $slot.id
    quantity = 1
} $bookingHeaders
$bookingNo = $created.data.bookingNo
if (-not $bookingNo) { throw "Booking creation returned no booking number" }

$managementPage = Invoke-Json "Get" `
    "$gateway/api/v1/bookings/management?status=PENDING_CONFIRMATION&pageNumber=0&pageSize=20" `
    $null $adminHeaders
if (-not (@($managementPage.data.items) | Where-Object { $_.bookingNo -eq $bookingNo })) {
    throw "Booking did not appear in the management approval queue"
}
$confirmed = Invoke-Json "Post" "$gateway/api/v1/bookings/$bookingNo/confirmation" $null $adminHeaders
if ($confirmed.data.status -ne "CONFIRMED") { throw "Booking was not confirmed" }

$search = Invoke-RestMethod `
    -Uri "$gateway/api/v1/search/resources?text=Emerald&page=0&size=20" -Headers $headers
if (@($search.items).Count -lt 1) { throw "Search did not return the demo resource" }

$notificationFound = $false
$deadline = (Get-Date).AddSeconds(20)
do {
    $notifications = Invoke-RestMethod `
        -Uri "$gateway/api/v1/notifications?userId=$($profile.id)&pageNumber=0&pageSize=50" `
        -Headers $headers
    $notificationFound = [bool](
        @($notifications.data.items) | Where-Object { $_.bookingNo -eq $bookingNo }
    )
    if (-not $notificationFound) { Start-Sleep -Seconds 1 }
} while (-not $notificationFound -and (Get-Date) -lt $deadline)
if (-not $notificationFound) { throw "Booking notification did not arrive within 20 seconds" }

$refreshed = Invoke-Json "Post" "$gateway/api/v1/auth/refresh" @{
    refreshToken = $login.data.refreshToken
}
if (-not $refreshed.data.accessToken) { throw "Token refresh returned no access token" }
$null = Invoke-Json "Post" "$gateway/api/v1/auth/logout" @{
    refreshToken = $refreshed.data.refreshToken
}

[pscustomobject]@{
    Gateway = "UP"
    Registration = "PASS"
    Profile = "PASS"
    UserDirectory = "PASS"
    ResourceAndSlot = "PASS"
    Booking = "CONFIRMED"
    Search = "PASS"
    Notification = "PASS"
    RefreshAndLogout = "PASS"
} | Format-List
