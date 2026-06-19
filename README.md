# AMHS/OHT Backend

AMHS/OHT 반송 관제 시스템의 백엔드입니다. 실제 OHT 장비가 없는 환경에서도 FAB 지도, OHT 상태, 반송 작업, SSE 실시간 이벤트, 운영 조치, 감사 로그를 검증할 수 있도록 구성했습니다.

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring Data JPA
- PostgreSQL
- SSE(Server-Sent Events)
- Gradle
- JUnit 5, H2, Testcontainers

## 주요 기능

- FAB 노드/구간 조회 및 구간 차단/해제
- OHT 목록/상세 조회 및 오류/복구 처리
- 반송 요청 생성, 조회, 배정, 이동 시작, 취소
- 최단 경로 탐색
- 운영 현황 및 분석 지표 조회
- SSE 기반 실시간 모니터링 이벤트 발행
- 데모 시뮬레이션 이벤트 생성
- 운영 조치 감사 로그 저장 및 필터 조회
- OHT 중복 배정 방지 동시성 제어
- 운영 조치와 감사 로그 트랜잭션 원자성 보장

## 실행 방법

### 1. PostgreSQL 실행

Docker Compose를 사용하면 개발용 PostgreSQL을 바로 실행할 수 있습니다.

```bash
docker compose up -d
```

`compose.yaml`의 기본 DB 설정은 다음과 같습니다.

| 항목 | 값 |
| --- | --- |
| database | `mydatabase` |
| username | `myuser` |
| password | `secret` |

### 2. 백엔드 실행

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다.

### 3. 상태 확인

```bash
curl http://localhost:8080/api/health
```

## 주요 API

| 구분 | Method | Endpoint |
| --- | --- | --- |
| Health | GET | `/api/health` |
| FAB 지도 | GET | `/api/fab-map` |
| 구간 차단 | POST | `/api/fab-edges/{edgeId}/block` |
| 구간 차단 해제 | POST | `/api/fab-edges/{edgeId}/unblock` |
| OHT 목록 | GET | `/api/ohts` |
| OHT 상세 | GET | `/api/ohts/{ohtId}` |
| OHT 오류 처리 | POST | `/api/ohts/{ohtId}/error` |
| OHT 복구 | POST | `/api/ohts/{ohtId}/recover` |
| 반송 요청 생성 | POST | `/api/transfer-requests` |
| 반송 요청 조회 | GET | `/api/transfer-requests` |
| 반송 요청 상세 | GET | `/api/transfer-requests/{requestId}` |
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
| 데모 단일 이벤트 | POST | `/api/demo-monitoring/tick` |

운영 조치 API는 `X-Operator-Id` 헤더를 받을 수 있습니다. 값이 없으면 기본 운영자 `operator01`로 감사 로그가 저장됩니다.

## 테스트

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

현재 검증한 항목은 다음과 같습니다.

- 도메인 상태 전이
- 반송 요청 생성/배정/취소
- 라우팅 및 배정 후보 선정
- SSE 이벤트 발행
- 운영 현황/분석 지표 계산
- 감사 로그 저장 및 조회
- OHT 중복 배정 동시성 테스트
- 운영 조치와 감사 로그 트랜잭션 원자성 테스트

## 성능 및 정합성 검증 결과

로컬 PostgreSQL에 반송 요청 10,000건을 생성한 뒤 운영 현황/분석 API를 측정하고 개선했습니다.

| API | 개선 전 p95 | 개선 후 p95 | 개선 전 RPS | 개선 후 RPS |
| --- | ---: | ---: | ---: | ---: |
| `/api/operations/overview` | 1.401초 | 0.051초 | 18.5 req/s | 529.0 req/s |
| `/api/analytics/summary` | 1.084초 | 0.028초 | 21.1 req/s | 1035.7 req/s |

동시성 검증에서는 OHT 1대에 동시에 100건의 배정 요청을 보내 하나의 작업만 성공하고 나머지 99건은 중복 배정으로 차단되는 것을 확인했습니다.

## 운영 조치 원자성

작업 취소, 구간 차단/해제, OHT 오류/복구 조치는 도메인 상태 변경과 감사 로그 저장이 같은 서비스 트랜잭션 안에서 처리됩니다. 따라서 운영 조치 결과와 감사 로그가 분리되어 저장되는 상황을 줄이고, 관제 이력의 신뢰성을 높였습니다.

## 기본 데이터

애플리케이션 최초 실행 시 FAB 노드, FAB 구간, OHT 기본 데이터가 자동으로 생성됩니다.

- FAB 노드: `STOCKER-A`, `STOCKER-B`, `JUNCTION-01`, `JUNCTION-02`, `EQP-01`, `EQP-02`, `CHARGER-01`
- FAB 구간: `EDGE-001` ~ `EDGE-006`
- OHT: `OHT-01`, `OHT-02`, `OHT-03`

