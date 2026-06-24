$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string] $Message)
    $failures.Add($Message) | Out-Null
}

function Assert-SetEquals {
    param(
        [string] $Name,
        [string[]] $Expected,
        [string[]] $Actual
    )

    $missing = $Expected | Where-Object { $_ -notin $Actual }
    $extra = $Actual | Where-Object { $_ -notin $Expected }

    foreach ($item in $missing) {
        Add-Failure "$Name is missing '$item'"
    }
    foreach ($item in $extra) {
        Add-Failure "$Name has undocumented value '$item'"
    }
}

function Read-JavaEnumValues {
    param(
        [string] $Path,
        [string] $EnumName
    )

    $raw = Get-Content $Path -Raw
    $match = [regex]::Match($raw, "enum\s+$EnumName\s*\{(?<body>.*?)\}", "Singleline")
    if (-not $match.Success) {
        Add-Failure "Enum '$EnumName' was not found in $Path"
        return @()
    }

    $body = $match.Groups["body"].Value
    $body = [regex]::Replace($body, "//.*", "")
    $body = ($body -split ";")[0]

    return $body -split "," |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ } |
        ForEach-Object { ($_ -replace "\(.*$", "" -replace "\s.*$", "").Trim() } |
        Where-Object { $_ }
}

function Read-ControllerEndpoints {
    $endpoints = New-Object System.Collections.Generic.List[string]
    $controllerFiles = Get-ChildItem -Path "src/main/java/org/example/amhs" -Recurse -File -Filter "*Controller.java"

    foreach ($file in $controllerFiles) {
        $raw = Get-Content $file.FullName -Raw
        $base = ""
        $baseMatch = [regex]::Match($raw, "@RequestMapping\s*\(\s*`"(?<path>[^`"]*)`"")
        if ($baseMatch.Success) {
            $base = $baseMatch.Groups["path"].Value
        }

        $mappingMatches = [regex]::Matches(
            $raw,
            "@(?<method>Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?`"(?<path>[^`"]*)`"",
            "Singleline"
        )

        foreach ($mapping in $mappingMatches) {
            $method = $mapping.Groups["method"].Value.ToUpperInvariant()
            $path = $mapping.Groups["path"].Value
            if ($path.StartsWith("/api/")) {
                $fullPath = $path
            } else {
                $fullPath = "$base$path"
            }
            $endpoints.Add("$method $fullPath") | Out-Null
        }
    }

    return $endpoints | Sort-Object -Unique
}

Write-Host "[harness] Backend harness check started"

$forbiddenDependencies = @(
    "spring-kafka",
    "kafka-clients",
    "amqp",
    "rabbitmq",
    "spring-boot-starter-websocket",
    "spring-boot-starter-webflux",
    "querydsl",
    "jooq"
)

$dependencyFiles = @("build.gradle", "settings.gradle") | Where-Object { Test-Path $_ }
foreach ($dependency in $forbiddenDependencies) {
    foreach ($file in $dependencyFiles) {
        $matches = Select-String -Path $file -Pattern $dependency -SimpleMatch -CaseSensitive:$false
        if ($matches) {
            Add-Failure "Forbidden dependency detected: $dependency in $file"
        }
    }
}

$forbiddenStatuses = @(
    "BUSY",
    "RUNNING",
    "OFFLINE",
    "MAINTENANCE",
    "PAUSED",
    "DONE",
    "IN_PROGRESS"
)

$sourceFiles = Get-ChildItem -Path "src" -Recurse -File -Include *.java,*.properties,*.sql |
    Where-Object { $_.FullName -notmatch "\\build\\" }

foreach ($status in $forbiddenStatuses) {
    $pattern = "['""]$status['""]"
    $matches = $sourceFiles | Select-String -Pattern $pattern -CaseSensitive
    foreach ($match in $matches) {
        Add-Failure "Undocumented status literal '$status' detected: $($match.Path):$($match.LineNumber)"
    }
}

Assert-SetEquals "TransferRequestStatus enum" `
    @("WAITING", "ASSIGNED", "MOVING", "COMPLETED", "FAILED", "CANCELED") `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/transfer/domain/TransferRequestStatus.java" "TransferRequestStatus")

Assert-SetEquals "TransferPriority enum" `
    @("LOW", "NORMAL", "HIGH", "URGENT") `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/transfer/domain/TransferPriority.java" "TransferPriority")

Assert-SetEquals "OhtStatus enum" `
    @("IDLE", "RESERVED", "MOVING", "ERROR") `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/oht/domain/OhtStatus.java" "OhtStatus")

Assert-SetEquals "NodeType enum" `
    @("STOCKER", "EQP", "PORT", "JUNCTION", "CHARGER", "BUFFER") `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/fab/domain/NodeType.java" "NodeType")

Assert-SetEquals "DomainEventType enum" `
    @(
        "TRANSFER_CREATED",
        "OHT_ASSIGNED",
        "TRANSFER_STARTED",
        "OHT_MOVED",
        "TRANSFER_COMPLETED",
        "TRANSFER_DELAYED",
        "TRANSFER_FAILED",
        "TRANSFER_CANCELED",
        "OHT_ERROR_OCCURRED",
        "OHT_RECOVERED",
        "EDGE_BLOCKED",
        "EDGE_UNBLOCKED",
        "ROUTE_NOT_FOUND"
    ) `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/monitoring/event/DomainEventType.java" "DomainEventType")

Assert-SetEquals "OperationActionType enum" `
    @("TRANSFER_CANCELED", "EDGE_BLOCKED", "EDGE_UNBLOCKED", "OHT_MARKED_ERROR", "OHT_RECOVERED") `
    (Read-JavaEnumValues "src/main/java/org/example/amhs/operations/domain/OperationActionType.java" "OperationActionType")

$expectedEndpoints = @(
    "GET /api/analytics/bottlenecks",
    "GET /api/analytics/oht-throughput",
    "GET /api/analytics/summary",
    "GET /api/demo-monitoring/status",
    "GET /api/dispatch/auto/status",
    "GET /api/fab-map",
    "GET /api/health",
    "GET /api/monitoring/stream",
    "GET /api/ohts",
    "GET /api/ohts/{ohtId}",
    "GET /api/operations/action-logs",
    "GET /api/operations/overview",
    "GET /api/routes/shortest",
    "GET /api/simulation/status",
    "GET /api/transfer-requests",
    "GET /api/transfer-requests/{requestId}",
    "POST /api/demo-monitoring/start",
    "POST /api/demo-monitoring/stop",
    "POST /api/demo-monitoring/tick",
    "POST /api/dispatch/auto/start",
    "POST /api/dispatch/auto/stop",
    "POST /api/dispatch/auto/tick",
    "POST /api/fab-edges/{edgeId}/block",
    "POST /api/fab-edges/{edgeId}/unblock",
    "POST /api/ohts/{ohtId}/error",
    "POST /api/ohts/{ohtId}/recover",
    "POST /api/simulation/start",
    "POST /api/simulation/stop",
    "POST /api/transfer-requests",
    "POST /api/transfer-requests/{requestId}/assign",
    "POST /api/transfer-requests/{requestId}/cancel",
    "POST /api/transfer-requests/{requestId}/start"
)
Assert-SetEquals "Controller endpoint contract" $expectedEndpoints (Read-ControllerEndpoints)

$apiFiles = Get-ChildItem -Path "src/main/java/org/example/amhs" -Recurse -File -Filter "*.java" |
    Where-Object { $_.FullName -match "\\api\\" }
foreach ($file in $apiFiles) {
    $repositoryImport = Select-String -Path $file.FullName -Pattern "import org\.example\.amhs\..*\.repository\." -CaseSensitive
    if ($repositoryImport) {
        Add-Failure "API layer must not import repositories: $($file.FullName)"
    }
}

$applicationFiles = Get-ChildItem -Path "src/main/java/org/example/amhs" -Recurse -File -Filter "*.java" |
    Where-Object { $_.FullName -match "\\application\\" }
foreach ($file in $applicationFiles) {
    $apiImport = Select-String -Path $file.FullName -Pattern "import org\.example\.amhs\..*\.api\." -CaseSensitive
    if ($apiImport) {
        Add-Failure "Application layer must not import API layer: $($file.FullName)"
    }
}

$trackedHarnessDocs = git ls-files ".md" 2>$null
if ($trackedHarnessDocs) {
    Add-Failure ".md harness documents are tracked by Git: $($trackedHarnessDocs -join ', ')"
}

if ($failures.Count -gt 0) {
    Write-Host "[harness] FAILED"
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host "[harness] PASSED: backend constraints, contracts, and layer boundaries are valid"
