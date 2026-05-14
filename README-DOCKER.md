# SmartCourier – Docker Setup

## Prerequisites
- Docker Desktop (or Docker Engine + Docker Compose)
- At least 4 GB RAM allocated to Docker

## Services & Ports

| Service           | Port |
|-------------------|------|
| API Gateway        | 9090 |
| Auth Service       | 8081 |
| Delivery Service   | 8082 |
| Tracking Service   | 8083 |
| Admin Service      | 8084 |
| Service Registry (Eureka) | 8761 |
| MySQL              | 3306 |
| Zipkin (Tracing)   | 9411 |

## How to Run

```bash
# From the project root (where docker-compose.yml is located):
docker compose up --build
```

First run will take ~5–10 minutes (Maven downloads dependencies and builds all JARs).

### Run in background
```bash
docker compose up --build -d
```

### Stop all services
```bash
docker compose down
```

### Stop and remove all data (including DB volumes)
```bash
docker compose down -v
```

## Useful URLs

- **Eureka Dashboard**: http://localhost:8761
- **Zipkin Dashboard**: http://localhost:9411
- **API Gateway**: http://localhost:9090
- **Auth Swagger UI**: http://localhost:8081/swagger-ui.html
- **Delivery Swagger UI**: http://localhost:8082/swagger-ui.html
- **Tracking Swagger UI**: http://localhost:8083/swagger-ui.html
- **Admin Swagger UI**: http://localhost:8084/swagger-ui.html

## API Gateway Routes

| Endpoint Prefix | Routes To |
|----------------|-----------|
| `/gateway/auth/**` | auth-service |
| `/gateway/deliveries/**` | delivery-service |
| `/gateway/services/**` | delivery-service |
| `/gateway/tracking/**` | tracking-service |
| `/gateway/admin/**` | admin-service |

## Default DB Credentials

| Property | Value |
|----------|-------|
| Host (inside Docker) | `mysql` |
| Host (from host machine) | `localhost:3306` |
| Username | `root` |
| Password | `system` |
| Databases | `auth_db`, `delivery_db`, `tracking_db`, `admin_db` |
