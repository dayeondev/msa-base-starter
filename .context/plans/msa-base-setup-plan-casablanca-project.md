# MSA Base Setup Plan - Casablanca Project

## Context
이 프로젝트는 마이크로서비스 아키텍처(MSA)의 개발 기반을 구축하는 것이 목표입니다. 사용자는 로그인하여 기업을 검색하고 관심종목을 등록할 수 있으며, 등록된 기업의 최신 공시정보를 확인할 수 있습니다. 백엔드 비즈니스 로직은 최소화하고 **통합 실행, 서비스 연동, 모니터링 구축**이 1차 목표입니다.

## 기술 스택 (확정)
- **Frontend**: React + Vite
- **User Service**: Spring Boot + MySQL
- **Disclosure Service**: FastAPI + PostgreSQL
- **API Gateway**: Spring Cloud Gateway
- **Authentication**: Spring Security + JWT
- **Communication**: REST API
- **Data**: Mock 데이터 (초기 구현)

## Directory Structure
```
casablanca/
├── frontend/                 # React + Vite
├── user-service/             # Spring Boot + MySQL
├── disclosure-service/       # FastAPI + PostgreSQL
├── api-gateway/              # Spring Cloud Gateway
├── monitoring/               # Prometheus + Grafana + Loki
├── docker-compose.yml        # 전체 실행
└── README.md                 # 실행 가이드
```

## Implementation Plan

### Phase 1: Backend Services Setup

#### 1.1 User Service (Spring Boot + MySQL)
**Directory**: `user-service/`

**구현 내용**:
- Spring Boot 3.x + Spring Security + JWT
- MySQL 8.0
- 도메인: User, InterestCompany
- 간단한 CRUD API

**API Endpoints**:
```
POST   /api/users/register    # 회원가입
POST   /api/users/login       # 로그인 (JWT 발급)
GET    /api/users/me          # 내 정보 조회
POST   /api/users/interests   # 관심종목 추가
GET    /api/users/interests   # 관심종목 목록
DELETE /api/users/interests/{id}  # 관심종목 삭제
```

**구현 포인트**:
- JWT는 user-service에서 발급하고 gateway에서 검증
- 비밀번호는 BCrypt로 암호화
- Mock 데이터: 5개 정도의 샘플 회사 코드 반환

#### 1.2 Disclosure Service (FastAPI + PostgreSQL)
**Directory**: `disclosure-service/`

**구현 내용**:
- FastAPI + SQLAlchemy
- PostgreSQL 15
- 도메인: Company, Disclosure

**API Endpoints**:
```
GET    /api/companies/search?q={name}  # 기업 검색
GET    /api/companies/{id}              # 기업 상세
GET    /api/disclosures/company/{id}    # 기업 공시 목록
GET    /api/disclosures/latest          # 최신 공시
```

**구현 포인트**:
- Mock 데이터: 삼성전자, LG에너지솔루션 등 10개 회사
- 공시데이터는 더미 데이터 반환

#### 1.3 API Gateway (Spring Cloud Gateway)
**Directory**: `api-gateway/`

**구현 내용**:
- Spring Cloud Gateway
- JWT 필터를 통한 인증 검증
- 라우팅 설정

**Route Configuration**:
```yaml
spring.cloud.gateway.routes:
  - id: user-service
    uri: lb://user-service
    predicates:
      - Path=/api/users/**
  - id: disclosure-service
    uri: lb://disclosure-service
    predicates:
      - Path=/api/companies/**, /api/disclosures/**
```

### Phase 2: Frontend Setup

#### 2.1 React Frontend (Vite)
**Directory**: `frontend/`

**주요 페이지**:
1. `/login` - 로그인 페이지
2. `/signup` - 회원가입 페이지
3. `/dashboard` - 홈 (관심종목 공시정보)
4. `/search` - 기업 검색 페이지

**구현 포인트**:
- Axios로 Gateway 통신
- JWT는 localStorage 저장
- 간단한 UI: Material-UI 또는 Tailwind CSS

### Phase 3: Docker Compose Setup

#### 3.1 docker-compose.yml
**전체 구성**:
```yaml
services:
  user-service:
    build: ./user-service
    ports: ["8081:8081"]
    depends_on: [mysql]

  disclosure-service:
    build: ./disclosure-service
    ports: ["8082:8000"]
    depends_on: [postgres]

  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
    depends_on: [user-service, disclosure-service]

  mysql:
    image: mysql:8.0

  postgres:
    image: postgres:15

  frontend:
    build: ./frontend
    ports: ["3000:3000"]
```

### Phase 4: Monitoring (Level 1 - 기본)

#### 4.1 Monitoring Stack
**Directory**: `monitoring/`

**구성 요소**:
```
monitoring/
├── prometheus/
│   └── prometheus.yml      # JVM/Python metrics 수집
├── grafana/
│   └── datasources/        # Prometheus, Loki datasource
├── loki/
│   └── loki-config.yml     # Log 수집 설정
└── docker-compose.monitoring.yml
```

**Metrics 수집 대상**:
- User Service: Spring Boot Actuator (`/actuator/prometheus`)
- Disclosure Service: prometheus-fastapi-instrumentator
- Gateway: Spring Boot Actuator

**Log 수집**:
- 각 서비스의 stdout을 Loki로 전송
- Grafana에서 Loki datasource 연결

### Phase 5: Integration & Verification

#### 5.1 통합 흐름
```
Frontend (3000) → Gateway (8080) → User Service (8081)
                                   → Disclosure Service (8082)
```

#### 5.2 실행 순서
1. `docker-compose up -d` - 전체 서비스 실행
2. Frontend 접속: http://localhost:3000
3. Grafana 접속: http://localhost:3001
4. Prometheus 접속: http://localhost:9090

#### 5.3 테스트 시나리오
1. 회원가입 → 로그인 → JWT 토큰 발급 확인
2. 기업 검색 → 관심종목 등록
3. 홈화면에서 관심종목 공시정보 확인
4. Grafana에서 metrics/logs 확인

## Critical Files Reference

### User Service
- `user-service/src/main/java/com/casablanca/config/SecurityConfig.java` - JWT 설정
- `user-service/src/main/java/com/casablanca/controller/UserController.java` - API 엔드포인트
- `user-service/src/main/resources/application.yml` - DB 연결 설정

### Disclosure Service
- `disclosure-service/app/main.py` - FastAPI 메인
- `disclosure-service/app/models.py` - SQLAlchemy 모델
- `disclosure-service/app/routers/companies.py` - API 라우터

### API Gateway
- `api-gateway/src/main/java/com/casablanca/gateway/config/GatewayConfig.java` - 라우팅 설정
- `api-gateway/src/main/java/com/casablanca/gateway/filter/JwtFilter.java` - JWT 검증 필터

### Frontend
- `frontend/src/api/auth.js` - 인증 API 호출
- `frontend/src/api/companies.js` - 기업 API 호출
- `frontend/src/pages/Dashboard.jsx` - 홈 화면

### Monitoring
- `monitoring/prometheus/prometheus.yml` - 타겟 서비스 설정
- `monitoring/grafana/provisioning/datasources/` - datasource 자동 설정

## Mock Data Examples

### User Service - Mock Companies
```json
[
  {"id": 1, "code": "005930", "name": "삼성전자"},
  {"id": 2, "code": "373220", "name": "LG에너지솔루션"},
  {"id": 3, "code": "000660", "name": "SK하이닉스"}
]
```

### Disclosure Service - Mock Disclosures
```json
[
  {
    "id": 1,
    "companyId": 1,
    "title": "반기보고서",
    "date": "2025-02-20",
    "url": "https://dart.fss.or.kr/..."
  }
]
```

## Verification

### 1. 서비스 실행 확인
```bash
# 모든 서비스 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f user-service
```

### 2. API 테스트
```bash
# Health check
curl http://localhost:8080/actuator/health

# 회원가입
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123","email":"test@test.com"}'

# 로그인
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# JWT로 기업 검색
curl http://localhost:8080/api/companies/search?q=삼성 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### 3. 모니터링 확인
- **Grafana Dashboard**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Loki**: http://localhost:3100

### 4. End-to-End 테스트
1. 브라우저 접속 → http://localhost:3000
2. 회원가입 후 로그인
3. 기업 검색 후 관심종목 등록
4. 홈화면에서 공시정보 확인
5. Grafana에서 로그/메트릭 확인

## Next Steps (Future Enhancement)
1. Level 2 모니터링: Custom metrics + Tracing
2. 실제 OPEN DART API 연동
3. OAuth2 소셜 로그인 추가
4. Message Queue를 이용한 비동기 처리
5. CI/CD 파이프라인 구성

---

# Request Tracer Dashboard Improvement Plan

## Context
현재 Request Tracer 대시보드는 기본적인 Request ID 검색 기능만 제공합니다. 사용자는 **로그 발생량 시각화**와 **서비스별 로그 확인**을 원합니다.

## User's Dashboard Structure Proposal

```
┌─────────────────────────────────────────────┐
│  Request ID: [________________]             │  ← Template Variable
├─────────────────────────────────────────────┤
│  Row 1: 로그 발생량 그래프                   │
│  ┌────────────────────────────────────┐    │
│  │  전체 로그 & 에러 로그 (Timeseries)  │    │  ← 드래그으로 시간 선택 가능
│  └────────────────────────────────────┘    │
├─────────────────────────────────────────────┤
│  Row 2: 일반 로그 확인용                     │
│  ┌─────────────────────┐ ┌───────────────┐ │
│  │ User Service Logs   │ │ Disclosure    │ │
│  │ (Logs Panel)        │ │ Service Logs  │ │
│  └─────────────────────┘ └───────────────┘ │
├─────────────────────────────────────────────┤
│  Row 3: Request ID 기반 확인                │
│  ┌─────────────────────┐ ┌───────────────┐ │
│  │ User Service Logs   │ │ Disclosure    │ │
│  │ (Filtered by ID)    │ │ Service Logs  │ │
│  └─────────────────────┘ └───────────────┘ │
└─────────────────────────────────────────────┘
```

## Evaluation

### ✅ 장점
1. **명확한 목적 분리**: 일반 로그(Row 2)와 Request ID 기반 로그(Row 3)를 물리적으로 분리
2. **직관적인 시각화**: Row 1의 그래프로 전체 로그 트렌드 파악
3. **서비스별 분리**: user-service와 disclosure-service를 별도 패널로 표시
4. **시간 범위 상호작용**: 드래그으로 시간 선택 후 하단 패널에서 상세 로그 확인

### 🔄 개선 제안

#### 1. Row 1: 로그 발생량 그래프 강화
- **추가 패널**: 로그 레벨별 분포 (INFO, WARN, ERROR, DEBUG)
- **서비스별 분리**: user-service와 disclosure-service를 각각 다른 색상으로 표시
- **에러율 계산**: 에러 로그 / 전체 로그 비율을 Gauge로 표시

#### 2. Row 2: 일반 로그 확인용
- **패널 타입**: Logs panel (Table보다 실시간 로그 스트리밍에 적합)
- **시간 동기화**: Row 1의 시간 범위 선택과 자동 동기화
- **로그 레벨 필터**: 드롭다운으로 ERROR, WARN, INFO 필터링

#### 3. Row 3: Request ID 기반 확인
- **자동 필터링**: Request ID 입력 시 Row 3 패널만 필터링
- **하이라이트**: Request ID를 포함한 로그 라인 강조
- **연관 로그**: 동일 Request ID의 모든 로그를 시간 순서대로 표시

## Implementation Plan

### Phase 1: 기본 구조 구현

**파일**: `monitoring/grafana/provisioning/dashboards/files/request-tracer.json`

#### Template Variables
```json
{
  "name": "requestId",
  "type": "textbox",
  "label": "Request ID",
  "current": {"text": "", "value": ""}
}
```

#### Row 1: 로그 발생량 그래프
**Panel Type**: Timeseries
**Queries**:
```logql
# 전체 로그
sum(count_over_time({container_name=~"casablanca-.*"}[5m]))

# 에러 로그
sum(count_over_time({container_name=~"casablanca-.*"} |= "ERROR"[5m]))

# 서비스별 로그
sum(count_over_time({container_name=~"casablanca-.*"}[5m])) by (container_name)
```

#### Row 2: 일반 로그
**Panel Type**: Logs
**Left Panel** (User Service):
```logql
{container_name="casablanca-user-service"}
```

**Right Panel** (Disclosure Service):
```logql
{container_name="casablanca-disclosure-service"}
```

#### Row 3: Request ID 기반 로그
**Panel Type**: Logs
**Left Panel** (User Service):
```logql
{container_name="casablanca-user-service"} |~ "${requestId:.*}"
```

**Right Panel** (Disclosure Service):
```logql
{container_name="casablanca-disclosure-service"} |~ "${requestId:.*}"
```

### Phase 2: 기능 강화 (선택)

1. **로그 레벨 필터 Template Variable**
   ```json
   {
     "name": "logLevel",
     "type": "custom",
     "query": "ERROR,WARN,INFO,DEBUG,ALL"
   }
   ```

2. **시간 동기화 설정**
   - 각 패널에 `"timeOptions": {"sync": true}` 추가

3. **에러 알림**
   - 에러 로그가 특정 임계값을 초과하면 알림

## Critical Files to Modify

### Primary File
- `monitoring/grafana/provisioning/dashboards/files/request-tracer.json`

### Reference Files (Reusable Patterns)
- `monitoring/grafana/provisioning/dashboards/files/enhanced-logs-explorer.json` (로그 패턴 참고)
- `monitoring/grafana/provisioning/dashboards/files/service-health-overview.json` (그래프 설정 참고)

## Panel Configuration Examples

### Timeseries Panel (Row 1)
```json
{
  "type": "timeseries",
  "title": "로그 발생량",
  "gridPos": {"h": 8, "w": 24, "x": 0, "y": 1},
  "fieldConfig": {
    "defaults": {
      "custom": {
        "lineWidth": 2,
        "fillOpacity": 10,
        "axisCenteredZero": false
      }
    }
  },
  "targets": [
    {
      "expr": "sum(count_over_time({container_name=~\"casablanca-.*\"}[5m]))",
      "refId": "A",
      "legendFormat": "전체 로그"
    },
    {
      "expr": "sum(count_over_time({container_name=~\"casablanca-.*\"} |= \"ERROR\"[5m]))",
      "refId": "B",
      "legendFormat": "에러 로그"
    }
  ]
}
```

### Logs Panel (Row 2 & 3)
```json
{
  "type": "logs",
  "title": "User Service Logs",
  "gridPos": {"h": 10, "w": 12, "x": 0, "y": 9},
  "targets": [
    {
      "expr": "{container_name=\"casablanca-user-service\"}",
      "refId": "A"
    }
  ],
  "options": {
    "showTime": true,
    "showLabels": true,
    "wrapLogMessage": false
  }
}
```

## Verification

1. **Grafana 재시작**: `docker-compose -f docker-compose.monitoring.yml restart grafana`
2. **대시보드 접속**: http://localhost:3001 → Dashboards → Request Tracer
3. **테스트 시나리오**:
   - Request ID 비어있을 때: Row 2만 데이터 표시, Row 3는 모든 로그 표시
   - Request ID 입력 후: Row 3에서 해당 ID 로그만 필터링
   - Row 1 그래프 드래그: 시간 범위 선택 후 Row 2, 3 동기화 확인

---

# Grafana Dashboard Improvement Plan

## Context
현재 대시보드는 MSA 모니터링에 부족합니다. 사용자는 **서비스 건강 상태 대시보드**와 **개선된 로그 모니터링**을 요청했습니다.

## Current Dashboard Issues
- **System Overview**: 너무 일반적, 서비스별 메트릭 부재
- **Spring Boot Metrics**: user-service만 커버, 다른 서비스 누락
- **Logs Explorer**: 기본적, 로그와 메트릭 연동 부재

## Implementation Plan

### Dashboard 1: Service Health Overview (서비스 건강 상태 대시보드)

**목표**: 모든 서비스의 핵심 건강 지표를 한눈에 확인

**Panels 구성**:

1. **서비스 상태 매트릭스** (Row 1)
   - 각 서비스의 up/down 상태 (Green/Red)
   - API Gateway, User Service, Disclosure Service, MySQL, PostgreSQL

2. **요청 응답시간** (Row 2)
   - p50, p90, p99 레이턴시 (바 차트)
   - 서비스별 색상 구분

3. **에러율** (Row 3)
   - 4xx, 5xx 에러율 (Gauge)
   - 서비스별 에러 추이 (시계열)

4. **요청률** (Row 4)
   - RPS (Requests Per Second)
   - 서비스별 요청량 (시계열)

5. **DB 커넥션 풀** (Row 5)
   - 활성/유휴 커넥션 수
   - HikariCP 메트릭

**PromQL 쿼리**:
```promql
# Service Health
up{job=~"user-service|disclosure-service|api-gateway"}

# Response Time Percentiles
histogram_quantile(0.90, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))

# Error Rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
/ sum(rate(http_server_requests_seconds_count[5m])) by (service) * 100

# Request Rate
sum(rate(http_server_requests_seconds_count[5m])) by (service)
```

### Dashboard 2: Enhanced Logs Explorer (개선된 로그 모니터링)

**목표**: 로그와 메트릭 연동, 에러 추적 용이성

**Panels 구성**:

1. **로그 볼륨 & 에러 추이** (Row 1)
   - 전체 로그 vs 에러 로그 (시계열)
   - 서비스별 로그량

2. **에러 패턴 분석** (Row 2)
   - 상위 에러 메시지 (Bar chart)
   - 에러 발생 빈도

3. **예외별 집계** (Row 3)
   - Exception 타입별 Pie chart
   - Stack trace 카운트

4. **최근 에러 로그** (Row 4)
   - Table로 최근 에러 표시
   - 클릭 시 전체 로그 확인 가능

5. **로그 검색** (Row 5)
   - Loki Query 입력 필드
   - 빈도 높은 로그 패턴 자동 추천

**LogQL 쿼리**:
```logql
# Error logs with count
count_over_time({level="error"}[5m])

# Top error messages
topk(10, count by (error_message) ({level="error"} | line_format "{{error_message}}"))

# Logs by service
sum(count_over_time({container_name=~".+"}[5m])) by (container_name)
```

## Files to Create/Modify

### Create
```
monitoring/grafana/provisioning/dashboards/files/
├── service-health-overview.json      # NEW
└── enhanced-logs-explorer.json        # NEW (replace existing)
```

### Modify (Optional)
```
monitoring/grafana/provisioning/dashboards/dashboard.yml  # Already configured
```

## Verification

1. **Grafana 접속**: http://localhost:3001 (admin/admin)
2. **Dashboard 확인**:
   - Dashboards → Browse → Service Health Overview
   - Dashboards → Browse → Enhanced Logs Explorer
3. **테스트 시나리오**:
   - 의도적으로 에러 발생 (잘못된 API 호출)
   - 대시보드에서 에러 반영 확인
   - 로그에서 해당 에러 추적 가능한지 확인
