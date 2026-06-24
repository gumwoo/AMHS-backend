$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure {
    param([string] $Message)
    $failures.Add($Message) | Out-Null
}

Write-Host "[하네스] 백엔드 아키텍처 제약 검사 시작"

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
            Add-Failure "금지 의존성 '$dependency' 감지: $file"
        }
    }
}

$allowedQuotedStatuses = @(
    "WAITING",
    "ASSIGNED",
    "MOVING",
    "COMPLETED",
    "FAILED",
    "CANCELED",
    "IDLE",
    "RESERVED",
    "ERROR",
    "URGENT",
    "HIGH",
    "NORMAL",
    "LOW"
)

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
        Add-Failure "문서화되지 않은 상태값 '$status' 감지: $($match.Path):$($match.LineNumber)"
    }
}

$trackedHarnessDocs = git ls-files ".md" 2>$null
if ($trackedHarnessDocs) {
    Add-Failure ".md 하네스 문서가 Git에 추적 중입니다. 의도적으로 제외한 문서는 커밋하지 마세요: $($trackedHarnessDocs -join ', ')"
}

if ($failures.Count -gt 0) {
    Write-Host "[하네스] 실패"
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host "[하네스] 통과: 금지 의존성, 미정의 상태값, 문서 커밋 여부 이상 없음"
