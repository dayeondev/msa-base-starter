# Casablanca - MSA Base Project

마이크로서비스 아키텍처(MSA) 개발 기반 구축 프로젝트입니다. 사용자는 로그인하여 기업을 검색하고 관심종목을 등록할 수 있으며, 등록된 기업의 최신 공시정보를 확인할 수 있습니다.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│                          Port: 3000                             │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway (8080)                         │
│                  - JWT Authentication                           │
│                  - Request Routing                             │
└─────────┬───────────────────────────────┬───────────────────────┘
          │                               │
          ▼                               ▼
┌──────────────────────┐      ┌──────────────────────────────────┐
│  User Service (8081) │      │  Disclosure Service (8000)       │
│  - Spring Boot       │      │  - FastAPI                       │
│  - MySQL             │      │  - PostgreSQL                    │
│  - Auth, Interests   │      │  - Companies, Disclosures        │
└──────────────────────┘      └──────────────────────────────────┘
```

## Tech Stack

| Service | Technology | Port |
|---------|-----------|------|
| Frontend | React + Vite | 3000 |
| API Gateway | Spring Cloud Gateway | 8080 |
| User Service | Spring Boot + MySQL | 8081 |
| Disclosure Service | FastAPI + PostgreSQL | 8000 (host: 8082) |
| Prometheus | Monitoring | 9090 |
| Grafana | Visualization | 3001 |
| Loki | Log Aggregation | 3100 |

## Quick Start

### Prerequisites

- Docker
- Docker Compose

### 1. Start All Services

```bash
# Start main services
docker-compose up -d

# Start monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2. Verify Services

```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f user-service
```

### 3. Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | - |
| API Gateway Health | http://localhost:8080/actuator/health | - |
| Grafana | http://localhost:3001 | admin/admin |
| Prometheus | http://localhost:9090 | - |

## API Documentation

### User Service (via Gateway)

```bash
# Register
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "test1234"
}

# Login
POST http://localhost:8080/api/users/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "test1234"
}

# Get Profile (requires JWT)
GET http://localhost:8080/api/users/me
Authorization: Bearer {token}

# Add Interest
POST http://localhost:8080/api/users/interests
Authorization: Bearer {token}
Content-Type: application/json

{
  "companyCode": "005930",
  "companyName": "삼성전자"
}

# Get Interests
GET http://localhost:8080/api/users/interests
Authorization: Bearer {token}
```

### Disclosure Service (via Gateway)

```bash
# Search Companies
GET http://localhost:8080/api/companies/search?q=삼성전자
Authorization: Bearer {token}

# Get Company Disclosures
GET http://localhost:8080/api/disclosures/company/1
Authorization: Bearer {token}

# Get Latest Disclosures
GET http://localhost:8080/api/disclosures/latest?company_ids=1,2,3
Authorization: Bearer {token}
```

## Testing

### 1. User Flow Test

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@test.com","password":"test1234"}'

# 2. Login and get JWT
TOKEN=$(curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test1234"}' \
  | jq -r '.token')

# 3. Search companies
curl -X GET "http://localhost:8080/api/companies/search?q=삼성" \
  -H "Authorization: Bearer $TOKEN"

# 4. Add to interests
curl -X POST http://localhost:8080/api/users/interests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"companyCode":"005930","companyName":"삼성전자"}'

# 5. View interests
curl -X GET http://localhost:8080/api/users/interests \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Health Checks

```bash
# Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Disclosure Service
curl http://localhost:8082/health
```

## Monitoring

### Grafana Dashboards

1. Access Grafana: http://localhost:3001 (admin/admin)
2. Go to Dashboards → Browse
3. View pre-configured dashboards for:
   - JVM metrics (User Service, Gateway)
   - Python metrics (Disclosure Service)
   - System metrics

### Loki Logs

1. In Grafana, go to Explore
2. Select Loki datasource
3. Query logs by service:
   - `{service="user-service"}`
   - `{service="disclosure-service"}`
   - `{service="api-gateway"}`

### Prometheus Metrics

Access http://localhost:9090 and query:
- `jvm_memory_used_bytes` - JVM memory usage
- `http_server_requests_seconds` - Request latency
- `process_cpu_usage` - CPU usage

## Development

### Local Development (without Docker)

```bash
# Start databases only
docker-compose up mysql postgres

# User Service
cd user-service
mvn spring-boot:run

# Disclosure Service
cd disclosure-service
pip install -r requirements.txt
uvicorn app.main:app --reload

# API Gateway
cd api-gateway
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

### Stopping Services

```bash
# Stop main services
docker-compose down

# Stop monitoring
docker-compose -f docker-compose.monitoring.yml down

# Stop all (including volumes)
docker-compose down -v
```

## Troubleshooting

### Service Not Starting

```bash
# Check logs
docker-compose logs user-service
docker-compose logs api-gateway

# Restart specific service
docker-compose restart user-service
```

### Database Connection Issues

```bash
# Check if databases are running
docker-compose ps mysql postgres

# Restart databases
docker-compose restart mysql postgres
```

### Frontend API Connection

1. Check `VITE_API_URL` in frontend Dockerfile
2. Verify API Gateway is accessible: `curl http://localhost:8080/actuator/health`
3. Check browser console for CORS errors

## Project Structure

```
casablanca/
├── frontend/                 # React + Vite
│   ├── src/
│   │   ├── api/             # API clients
│   │   ├── pages/           # Page components
│   │   └── components/      # Reusable components
│   └── Dockerfile
├── user-service/            # Spring Boot
│   ├── src/main/java/com/casablanca/
│   │   ├── config/         # Security, JWT
│   │   ├── controller/     # REST controllers
│   │   ├── entity/         # JPA entities
│   │   ├── service/        # Business logic
│   │   └── repository/     # Data access
│   └── Dockerfile
├── disclosure-service/      # FastAPI
│   ├── app/
│   │   ├── models/         # SQLAlchemy models
│   │   ├── routers/        # API routes
│   │   └── main.py
│   └── Dockerfile
├── api-gateway/            # Spring Cloud Gateway
│   ├── src/main/java/com/casablanca/gateway/
│   │   ├── config/         # CORS, Gateway config
│   │   ├── filter/         # JWT filter
│   │   └── util/           # JWT utilities
│   └── Dockerfile
├── monitoring/             # Prometheus, Grafana, Loki
│   ├── prometheus/
│   ├── grafana/
│   ├── loki/
│   └── promtail/
├── docker-compose.yml
├── docker-compose.monitoring.yml
└── README.md
```

## Next Steps

1. **Level 2 Monitoring**: Add custom metrics and distributed tracing
2. **Real Data Integration**: Connect to OPEN DART API
3. **OAuth2**: Add social login (Google, GitHub)
4. **Message Queue**: Add RabbitMQ/Kafka for async processing
5. **CI/CD**: Set up GitHub Actions or Jenkins pipeline

## License

MIT
