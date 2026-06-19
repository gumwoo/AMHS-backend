# AMHS/OHT 반송 관제 시스템 Backend

반도체 FAB 내부의 OHT(Overhead Hoist Transport) 반송 흐름을 관제하기 위한 백엔드입니다. 실제 장비와 직접 연결하지 못하는 개발 환경에서도 FAB 지도, OHT 상태, 반송 작업, 실시간 이벤트, 운영 조치, 감사 로그를 하나의 업무 흐름으로 검증할 수 있도록 구현했습니다.

이 프로젝트의 핵심은 단순 CRUD가 아니라 **운영자가 관제 화면에서 상황을 판단하고 조치하며, 그 조치 이력이 추적 가능한 형태로 남는 구조**를 만드는 것입니다.

## 프로젝트 목표

- 실제 OHT 장비가 없어도 관제 흐름을 시연할 수 있는 데모 환경 구성
- FAB 노드/구간, OHT, 반송 작업을 도메인 단위로 분리
- 반송 배정, 이동, 완료, 실패, 취소 상태 전이 구현
- SSE(Server-Sent Events) 기반 실시간 이벤트 발행
- 작업 취소, 구간 차단/해제, OHT 오류/복구 같은 운영 조치 구현
- 운영자 ID와 사유를 포함한 감사 로그 저장
- 운영 지표와 분석 API를 데이터 증가에도 버틸 수 있도록 최적화
- OHT 중복 배정 방지를 위한 동시성 제어 검증

## 핵심 도메인

| 도메인 | 역할 |
| --- | --- |
| FAB | 반송 노드, 구간, 차단 상태 관리 |
| OHT | 장비 상태, 현재 위치, 할당 작업, 오류/복구 관리 |
| Transfer | 반송 요청 생성, 배정, 이동 시작, 취소, 실패/완료 상태 관리 |
| Routing | 출발지와 목적지 기준 최단 경로 탐색 |
| Dispatch | 반송 작업에 적합한 OHT 선정 |
| Monitoring | SSE 기반 실시간 이벤트 발행 |
| Simulation | 실제 장비 없이 데모 이벤트 생성 |
| Operations | 운영 현황, 문제 작업, 이상 장비, 차단 구간 조회 |
| Analytics | 완료율, 실패율, 평균/P95 반송 시간, 병목 구간 분석 |
| Audit Log | 운영 조치 이력 저장 및 필터 조회 |

## 주요 구현 내용

### 1. 실시간 관제 이벤트

백엔드는 `/api/monitoring/stream`에서 SSE 스트림을 제공합니다. 데모 시뮬레이션이나 운영 조치에서 발생한 이벤트는 프론트 화면으로 실시간 전달됩니다.

주요 이벤트는 다음과 같습니다.

- `TRANSFER_CREATED`
- `OHT_ASSIGNED`
- `TRANSFER_STARTED`
- `OHT_MOVED`
- `TRANSFER_COMPLETED`
- `TRANSFER_CANCELED`
- `OHT_ERROR_OCCURRED`
- `OHT_RECOVERED`
- `EDGE_BLOCKED`
- `EDGE_UNBLOCKED`
- `ROUTE_NOT_FOUND`

SSE를 사용한 이유는 관제 시스템의 이벤트 흐름이 서버에서 클라이언트로 지속적으로 전달되는 구조이기 때문입니다. 양방향 통신보다 서버 발행 중심의 실시간 업데이트가 중요하므로 WebSocket보다 단순한 SSE가 이 프로젝트에 적합했습니다.

### 2. 운영 조치와 감사 로그

운영자는 관제 화면에서 다음 조치를 수행할 수 있습니다.

| 조치 | API | 기록되는 감사 로그 |
| --- | --- | --- |
| 반송 작업 취소 | `POST /api/transfer-requests/{requestId}/cancel` | `TRANSFER_CANCELED` |
| FAB 구간 차단 | `POST /api/fab-edges/{edgeId}/block` | `EDGE_BLOCKED` |
| FAB 구간 차단 해제 | `POST /api/fab-edges/{edgeId}/unblock` | `EDGE_UNBLOCKED` |
| OHT 오류 처리 | `POST /api/ohts/{ohtId}/error` | `OHT_MARKED_ERROR` |
| OHT 복구 | `POST /api/ohts/{ohtId}/recover` | `OHT_RECOVERED` |

조치 API는 `X-Operator-Id` 헤더를 받아 누가 어떤 조치를 했는지 저장합니다. 운영자 값이 없으면 기본값 `operator01`로 기록합니다.

감사 로그에는 조치 유형, 대상 유형, 대상 ID, 운영자 ID, 사유, 생성 시각이 저장됩니다. 이력 조회는 `/api/operations/action-logs`에서 운영자, 조치 유형, 대상 ID 기준으로 필터링할 수 있습니다.

### 3. 트랜잭션 원자성

운영 조치는 상태 변경과 감사 로그가 함께 남아야 운영 기록으로 의미가 있습니다. 그래서 작업 취소, 구간 차단/해제, OHT 오류/복구는 각 도메인 서비스의 `@Transactional` 메서드 안에서 상태 변경과 감사 로그 저장을 함께 처리합니다.

이를 통해 다음 흐름을 보장합니다.

- 반송 작업 취소와 감사 로그 저장이 같은 트랜잭션에서 처리
- 구간 차단/해제와 감사 로그 저장이 같은 트랜잭션에서 처리
- OHT 오류/복구와 감사 로그 저장이 같은 트랜잭션에서 처리
- 컨트롤러는 요청 전달만 담당하고, 업무 원자성은 서비스 계층에서 보장

`OperationActionTransactionTest`로 구간 차단, OHT 오류 처리, 반송 작업 취소 시 도메인 상태와 감사 로그가 함께 저장되는지 검증했습니다.

### 4. 운영 지표

운영 현황 API는 관제 화면에서 계속 조회되는 핵심 API입니다. 현재 구현은 전체 반송 작업을 애플리케이션 메모리로 가져와 계산하지 않고, DB 집계 쿼리와 repository count를 사용합니다.

운영 현황 API에서 제공하는 값은 다음과 같습니다.

| 항목 | 설명 |
| --- | --- |
| waitingTransfers | 대기 중인 반송 작업 수 |
| assignedTransfers | OHT가 배정된 반송 작업 수 |
| movingTransfers | 이동 중인 반송 작업 수 |
| completedTransfers | 완료된 반송 작업 수 |
| failedTransfers | 실패한 반송 작업 수 |
| canceledTransfers | 취소된 반송 작업 수 |
| idleOhts | 대기 중인 OHT 수 |
| reservedOhts | 예약된 OHT 수 |
| movingOhts | 이동 중인 OHT 수 |
| errorOhts | 오류 상태 OHT 수 |
| blockedEdges | 차단된 FAB 구간 수 |

프론트는 `/api/operations/overview` 응답을 받아 운영 지표 영역에 반영합니다. SSE 이벤트로 화면에 들어온 최신 OHT/구간/작업 상태가 있으면 그 값을 우선 사용하고, 없을 때는 백엔드 운영 현황 count를 fallback으로 사용합니다.

### 5. 성능 개선

초기 구현에서는 운영 현황과 분석 요약 계산 시 전체 반송 요청을 조회한 뒤 Java stream으로 count와 통계를 계산했습니다. 이후 DB 집계 쿼리와 조회 패턴 기반 인덱스를 적용해 성능을 개선했습니다.

추가로 OHT 처리량과 병목 구간 분석에 남아 있던 전체 `findAll()` 조회를 제거했습니다. OHT 처리량은 배정 OHT가 있고 완료/실패 상태인 반송 요청만 기간 조건으로 조회하며, 병목 구간은 OHT 이동 이벤트를 기간 조건으로 먼저 줄인 뒤 평균/P95 계산을 수행합니다. 이 항목은 별도 성능 수치를 새로 측정한 항목은 아니며, 대용량 데이터에서 전체 테이블을 애플리케이션 메모리로 올리는 위험을 줄인 후속 최적화입니다.

측정 조건은 로컬 PostgreSQL, 반송 요청 10,000건, 총 300요청, 동시성 20 기준입니다.

| API | 개선 전 p95 | 개선 후 p95 | 개선 전 RPS | 개선 후 RPS |
| --- | ---: | ---: | ---: | ---: |
| `/api/operations/overview` | 1.401초 | 0.051초 | 18.5 req/s | 529.0 req/s |
| `/api/analytics/summary` | 1.084초 | 0.028초 | 21.1 req/s | 1035.7 req/s |

### 6. 동시성 제어

AMHS/OHT 관제에서는 외부 대규모 트래픽보다 같은 OHT가 동시에 여러 작업에 배정되지 않는 정합성이 중요합니다.

`TransferRequestConcurrencyTest`에서 OHT 1대에 100개의 반송 배정 요청을 동시에 실행했고, 조건부 update 기반의 `reserveIfIdle` 로직을 통해 하나의 요청만 성공하고 나머지 99건은 중복 배정으로 차단되는 것을 검증했습니다.

## 주요 API

| 구분 | Method | Endpoint |
| --- | --- | --- |
| Health | GET | `/api/health` |
| FAB 지도 | GET | `/api/fab-map` |
| OHT 목록 | GET | `/api/ohts` |
| 반송 요청 조회 | GET | `/api/transfer-requests` |
| 반송 요청 생성 | POST | `/api/transfer-requests` |
| 반송 배정 | POST | `/api/transfer-requests/{requestId}/assign` |
| 반송 시작 | POST | `/api/transfer-requests/{requestId}/start` |
| 반송 취소 | POST | `/api/transfer-requests/{requestId}/cancel` |
| 최단 경로 | GET | `/api/routes/shortest` |
| 운영 현황 | GET | `/api/operations/overview` |
| 감사 로그 | GET | `/api/operations/action-logs` |
| 분석 요약 | GET | `/api/analytics/summary` |
| 병목 구간 | GET | `/api/analytics/bottlenecks` |
| SSE 스트림 | GET | `/api/monitoring/stream` |
| 데모 시작 | POST | `/api/demo-monitoring/start` |
| 데모 중지 | POST | `/api/demo-monitoring/stop` |

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring Data JPA
- PostgreSQL
- SSE
- Gradle
- JUnit 5, H2, Testcontainers

## 실행 방법

```bash
docker compose up -d
./gradlew bootRun
```

Windows PowerShell:

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다.

## 테스트

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

주요 검증 항목은 도메인 상태 전이, 라우팅, 반송 배정, SSE 이벤트, 운영 지표, 분석 지표, 감사 로그, 동시성 제어, 트랜잭션 원자성입니다.
