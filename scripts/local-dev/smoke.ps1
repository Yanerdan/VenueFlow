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

function Get-JwtSubject([string]$Token) {
    $payload = $Token.Split(".")[1].Replace("-", "+").Replace("_", "/")
    while ($payload.Length % 4) { $payload += "=" }
    return ([Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) |
        ConvertFrom-Json).sub
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
$adminExternalUserId = Get-JwtSubject $adminLogin.data.accessToken

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

$accounts = Invoke-Json "Get" "$gateway/api/v1/auth/management/accounts" $null $adminHeaders
$candidate = @($accounts.data) |
    Where-Object { [string]$_.userId -eq [string]$registration.data.userId } |
    Select-Object -First 1
if (-not $candidate) { throw "Registered account did not appear in role management" }
$promoted = Invoke-Json "Patch" `
    "$gateway/api/v1/auth/management/accounts/$($candidate.userId)/role" @{
        role = "APPROVER"
        expectedVersion = $candidate.version
    } $adminHeaders
if ($promoted.data.role -ne "APPROVER") { throw "Account was not promoted to approver" }
$approvers = Invoke-Json "Get" `
    "$gateway/api/v1/auth/management/accounts/approvers" $null $adminHeaders
if (-not (@($approvers.data) | Where-Object { [string]$_.userId -eq [string]$candidate.userId })) {
    throw "Promoted account did not appear in the eligible approver directory"
}
$approverLogin = Invoke-Json "Post" "$gateway/api/v1/auth/login" @{
    username = $username
    password = $password
}
$token = $approverLogin.data.accessToken
$headers = @{ Authorization = "Bearer $token" }

$resources = Invoke-RestMethod -Uri "$gateway/api/v1/resources?page=0&size=100" -Headers $headers
$resource = @($resources.items) |
    Where-Object { $_.resourceNo -eq "VF-CAMPUS-001" } |
    Select-Object -First 1
if (-not $resource) { throw "Showcase resource VF-CAMPUS-001 is missing" }
$originalResource = $resource

$resource = Invoke-Json "Patch" "$gateway/api/v1/resources/$($resource.id)/ownership" @{
    ownerDepartment = "Campus Operations"
    approverExternalUserId = [string]$candidate.userId
    approvalMode = "TWO_STAGE"
    finalApproverExternalUserId = $adminExternalUserId
    expectedVersion = $resource.version
} $adminHeaders

$resource = Invoke-Json "Patch" "$gateway/api/v1/resources/$($resource.id)/booking-rules" @{
    bookingNotice = "Bring a campus card and follow the room instructions"
    minAdvanceHours = 0
    maxAdvanceDays = 90
    maxDurationMinutes = 480
    expectedVersion = $resource.version
} $adminHeaders
if ($resource.bookingNotice -notlike "Bring a campus card*") {
    throw "Resource booking notice was not persisted"
}

$from = [Uri]::EscapeDataString((Get-Date).ToUniversalTime().AddHours(-1).ToString("o"))
$to = [Uri]::EscapeDataString((Get-Date).ToUniversalTime().AddDays(30).ToString("o"))
$slots = Invoke-RestMethod `
    -Uri "$gateway/api/v1/resources/$($resource.id)/slots?from=$from&to=$to&page=0&size=100" `
    -Headers $headers
$slot = @($slots.items) | Where-Object { $_.status -eq "OPEN" } | Select-Object -First 1
if (-not $slot) { throw "Demo resource has no open slot" }
$slotDetail = Invoke-Json "Get" "$gateway/api/v1/resource-slots/$($slot.id)" $null $headers
if ($slotDetail.maxAdvanceDays -ne 90 -or $slotDetail.maxDurationMinutes -ne 480) {
    throw "Slot collaboration response did not carry resource booking rules"
}

$bookingHeaders = @{
    Authorization = "Bearer $token"
    "Idempotency-Key" = [Guid]::NewGuid().ToString()
}
$created = Invoke-Json "Post" "$gateway/api/v1/bookings" @{
    userId = $profile.id
    slotId = $slot.id
    quantity = 1
    activityTitle = "VenueFlow Acceptance Workshop"
    purpose = "Verify the complete campus application and review workflow"
    contactName = "Local Acceptance"
    contactPhone = "13800000000"
    note = "Automated local acceptance"
} $bookingHeaders
$bookingNo = $created.data.bookingNo
if (-not $bookingNo) { throw "Booking creation returned no booking number" }
if ($created.data.ownerDepartment -ne "Campus Operations") {
    throw "Booking did not snapshot resource ownership"
}

$managementPage = Invoke-Json "Get" `
    "$gateway/api/v1/bookings/management?status=PENDING_CONFIRMATION&pageNumber=0&pageSize=20" `
    $null $headers
if (-not (@($managementPage.data.items) | Where-Object { $_.bookingNo -eq $bookingNo })) {
    throw "Booking did not appear in the management approval queue"
}
$advanced = Invoke-Json "Post" "$gateway/api/v1/bookings/$bookingNo/confirmation" @{
    reviewNote = "Initial responsibility review passed"
} $headers
if ($advanced.data.status -ne "PENDING_CONFIRMATION" -or
    $advanced.data.currentApprovalStep -ne 2) {
    throw "Initial approval did not advance to the final stage"
}
$confirmed = Invoke-Json "Post" "$gateway/api/v1/bookings/$bookingNo/confirmation" @{
    reviewNote = "Final administration review passed"
} $adminHeaders
if ($confirmed.data.status -ne "CONFIRMED") { throw "Booking was not confirmed" }
if ($confirmed.data.reviewNote -ne "Final administration review passed") {
    throw "Booking review note was not persisted"
}
$approvalActions = Invoke-Json "Get" `
    "$gateway/api/v1/bookings/$bookingNo/approval-actions" $null $headers
if (@($approvalActions.data).Count -ne 2) {
    throw "Two-stage approval history was not persisted"
}
$report = Invoke-Json "Get" "$gateway/api/v1/bookings/management/report" $null $adminHeaders
if ($report.data.summary.totalBookings -lt 1 -or $report.data.summary.totalAttendees -lt 1) {
    throw "Operational report did not include persisted bookings"
}
if (-not (@($report.data.recentReviews) | Where-Object { $_.bookingNo -eq $bookingNo })) {
    throw "Operational report did not include the latest approval audit"
}

$search = Invoke-RestMethod `
    -Uri "$gateway/api/v1/search/resources?text=%E5%9B%BE%E4%B9%A6%E9%A6%86&page=0&size=20" -Headers $headers
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
    refreshToken = $approverLogin.data.refreshToken
}
if (-not $refreshed.data.accessToken) { throw "Token refresh returned no access token" }
$null = Invoke-Json "Post" "$gateway/api/v1/auth/logout" @{
    refreshToken = $refreshed.data.refreshToken
}

$restored = Invoke-Json "Patch" `
    "$gateway/api/v1/auth/management/accounts/$($candidate.userId)/role" @{
        role = "APPLICANT"
        expectedVersion = $promoted.data.version
    } $adminHeaders
if ($restored.data.role -ne "APPLICANT") { throw "Acceptance account role was not restored" }

$resource = Invoke-Json "Patch" "$gateway/api/v1/resources/$($resource.id)/ownership" @{
    ownerDepartment = $originalResource.ownerDepartment
    approverExternalUserId = $originalResource.approverExternalUserId
    approvalMode = $originalResource.approvalMode
    finalApproverExternalUserId = $originalResource.finalApproverExternalUserId
    expectedVersion = $resource.version
} $adminHeaders
$null = Invoke-Json "Patch" "$gateway/api/v1/resources/$($resource.id)/booking-rules" @{
    bookingNotice = $originalResource.bookingNotice
    minAdvanceHours = $originalResource.minAdvanceHours
    maxAdvanceDays = $originalResource.maxAdvanceDays
    maxDurationMinutes = $originalResource.maxDurationMinutes
    expectedVersion = $resource.version
} $adminHeaders

[pscustomobject]@{
    Gateway = "UP"
    Registration = "PASS"
    Profile = "PASS"
    UserDirectory = "PASS"
    ResourceAndSlot = "PASS"
    BookingRules = "PASS"
    Booking = "CONFIRMED"
    OperationalReport = "PASS"
    RoleManagement = "PASS"
    TwoStageApproval = "PASS"
    Search = "PASS"
    Notification = "PASS"
    RefreshAndLogout = "PASS"
} | Format-List
