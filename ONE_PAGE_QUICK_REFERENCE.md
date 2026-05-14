# SmartCourier - ONE PAGE CHEAT SHEET

## System Overview
**SmartCourier** is a microservices-based delivery management system. Customers book courier services → system tracks deliveries in real-time → admins manage operations.

---

## 7 Microservices (Remember These!)

| Service | Port | What It Does | Database |
|---------|------|-------------|----------|
| **Auth** | 8081 | User login, JWT tokens | auth_db |
| **Delivery** | 8082 | Book deliveries, manage status, calculate charges | delivery_db |
| **Tracking** | 8083 | Delivery history, events, proof of delivery | tracking_db |
| **Admin** | 8084 | Dashboard, reports, user management | admin_db |
| **API Gateway** | 9090 | Route requests, validate JWT | (none) |
| **Eureka** | 8761 | Service discovery & health checks | (none) |
| **Config Server** | 8889 | Centralized config management | (none) |

---

## Communication Patterns

### Synchronous (REST + Feign)
```
Admin Service → calls → Delivery Service via Feign client
Blocks waiting for response
Used when: Need immediate data
Downside: Slower, blocks thread
```

### Asynchronous (RabbitMQ)
```
Delivery Service → publishes event → RabbitMQ
Tracking Service → listens → creates history
Non-blocking, fire & forget
Used when: Don't need immediate response
Upside: Faster, decoupled, reliable
Fallback: If RabbitMQ fails → sync REST backup
```

---

## Delivery Lifecycle (Status Transitions)

```
DRAFT (customer wizard starts)
  ↓
BOOKED (wizard finalized, payment pending)
  ↓
PICKED_UP (partner picks up package)
  ↓
IN_TRANSIT (on delivery truck)
  ↓
OUT_FOR_DELIVERY (nearby)
  ↓
DELIVERED ✓

Also possible:
- FAILED → RETURNED (retry failed delivery)
- DELAYED (stuck) → IN_TRANSIT (resuming)
```

**Key Rule:** Customers can ONLY do DRAFT→BOOKED. Admins can do most transitions.

---

## Charge Calculation Formula

```
basePrice = 5.99 (DOMESTIC) | 14.99 (EXPRESS) | 29.99 (INTERNATIONAL)
weightCharge = weight × 0.5
fragileCharge = fragile ? 3.0 : 0

Total = basePrice + weightCharge + fragileCharge

Example: EXPRESS, 10kg, fragile = 14.99 + 5.0 + 3.0 = $22.99
```

---

## Authentication Flow

```
1. User registers → Auth Service stores credentials (hashed)
2. User logs in → Auth Service issues JWT token
3. User stores JWT in browser (localStorage)
4. Every request includes: Authorization: Bearer {JWT}
5. API Gateway validates JWT, extracts username & role
6. Adds headers: X-User-Username, X-User-Role
7. Forwards to service, service re-validates
8. Invalid token → 401 Unauthorized
9. Wrong role → 403 Forbidden
```

**3 Roles:** CUSTOMER, ADMIN, SYSTEM

---

## Database Schema (Key Tables)

### delivery (Delivery Service)
```
id, tracking_number (unique), username, status, charge, paid,
sender_address_id (FK), receiver_address_id (FK), 
parcel_package_id (FK), created_at, updated_at
```

### address (Delivery Service)
```
id, full_name, phone, street, city, state, zip_code, country
```

### parcel_package (Delivery Service)
```
id, weight, length, width, height, description,
service_type (DOMESTIC/EXPRESS/INTERNATIONAL),
declared_value, fragile
```

### tracking_event (Tracking Service)
```
id, delivery_id, status, location, message, timestamp
```

### delivery_proof (Tracking Service)
```
id, delivery_id, recipient_name, signature_url, photo_url, notes
```

---

## API Endpoints (Through API Gateway)

```
Authorization: Bearer {JWT} header required for all

POST /gateway/auth/register          → Create account
POST /gateway/auth/login             → Get JWT token

POST /gateway/deliveries             → Create delivery (direct)
POST /gateway/deliveries/draft       → Start wizard
PUT /gateway/deliveries/{id}/sender  → Add sender address
PUT /gateway/deliveries/{id}/receiver → Add receiver address
PUT /gateway/deliveries/{id}/package → Add package details
PUT /gateway/deliveries/{id}/finalize → Finalize wizard (DRAFT→BOOKED)
PUT /gateway/deliveries/{id}/status  → Update status (ADMIN)
GET /gateway/deliveries/{id}         → Get delivery details
GET /gateway/deliveries/tracking/{number} → Track by number

GET /gateway/tracking/{id}/events    → Get delivery timeline
POST /gateway/tracking/{id}/proof    → Add signature/photo
GET /gateway/tracking/{id}/proof     → Get proof

GET /gateway/admin/dashboard         → Admin KPIs (ADMIN)
GET /gateway/admin/deliveries        → All deliveries (ADMIN)
POST /gateway/admin/reports          → Generate report (ADMIN)
```

---

## Key Design Patterns

1. **Microservices** - Independent, scalable services
2. **Event-Driven** - RabbitMQ for loose coupling
3. **API Gateway** - Single entry point, JWT validation
4. **Service Discovery** - Eureka auto-discovers services
5. **State Machine** - Explicit delivery status transitions
6. **Database-per-Service** - Each service owns schema
7. **Fallback** - RabbitMQ fails → sync REST backup
8. **Distributed Tracing** - Zipkin for debugging

---

## Observability / Dashboards

| Tool | Port | Purpose |
|------|------|---------|
| Eureka | 8761 | Service registry, health status |
| Zipkin | 9411 | Distributed tracing, latency analysis |
| SonarQube | 9000 | Code quality, vulnerabilities |
| Adminer | 8080 | Database GUI |
| RabbitMQ UI | 15672 | Message queue status |

---

## Quick Start

```bash
# Start all services (production-like)
docker compose up --build

# Wait 2-3 minutes for startup

# Access:
# - Frontend: http://localhost:4200
# - API Gateway: http://localhost:9090
# - Eureka: http://localhost:8761
# - Zipkin: http://localhost:9411
```

---

## Common Error Scenarios

| Error | Cause | Fix |
|-------|-------|-----|
| 401 Unauthorized | Invalid JWT or expired | Re-login, get new token |
| 403 Forbidden | Wrong role (e.g., Customer trying admin action) | Use ADMIN account |
| 404 Not Found | Delivery doesn't exist | Check delivery ID |
| 503 Service Unavailable | Service crashed | Check Eureka, restart service |
| Status transition rejected | Invalid state change (e.g., BOOKED→DRAFT) | Check state machine rules |

---

## Resilience Features

✓ **Fallback:** RabbitMQ fails → Sync REST backup  
✓ **Health Checks:** Eureka removes unhealthy services  
✓ **Retry Logic:** RabbitMQ retries failed messages  
✓ **Circuit Breaker:** (Ready to implement) Stop calling failing service  
✓ **Graceful Degradation:** If Tracking fails, booking still works  
✓ **Timeout:** Don't hang forever on slow calls  

---

## Scaling Strategy

**Horizontal:** Run multiple instances of each service
- Eureka auto-discovers all instances
- API Gateway load-balances across them

**Vertical:** Increase container CPU/memory

**Data:** Each service can shard its database independently

---

## Testing Levels

1. **Unit Tests** → Service business logic
2. **Integration Tests** → TestContainers with real DB
3. **Component Tests** → Docker Compose full stack
4. **API Tests** → Postman/curl against running services
5. **Code Coverage** → JaCoCo (measure % tested)
6. **Code Quality** → SonarQube (analyze for bugs)

---

## Interview Talking Points

✓ **Explain why microservices** - Not because it's trendy, but because of independent scaling  
✓ **Show architecture diagram** - 7 services, API Gateway, RabbitMQ queues  
✓ **Walk through user journey** - Register → Login → Create delivery → Track → Delivered  
✓ **Discuss tradeoffs** - Complexity vs scalability, consistency models  
✓ **Mention monitoring** - Zipkin traces, Eureka health, SonarQube code quality  
✓ **Explain resilience** - Fallback patterns, circuit breakers, graceful degradation  
✓ **Discuss improvements** - Redis caching, K8s deployment, Saga pattern  

---

## Critical Files to Know

```
📁 delivery-service/src/main/java
  ├─ DeliveryService.java        ← Business logic, charge calc, status rules
  ├─ DeliveryController.java     ← REST endpoints
  ├─ Delivery.java               ← JPA entity
  └─ DeliveryStatus.java         ← Enum with state transitions

📁 docker-compose.yml           ← All services definition
📁 init-db.sql                  ← Database initialization
📁 config-repo/                 ← Config files for each service
```

---

## ROI / Value Proposition

**Before (Monolith):**
- Entire app crashes if one module bug
- Must scale whole app for one bottleneck
- Deployment = risk to entire system

**After (Microservices):**
- Isolated failures (one service down, others up)
- Scale only the busy services
- Independent deployments, less risk
- Teams work independently

**Trade-off:** More infrastructure, need monitoring tools

---

## Final Notes

- **Database:** One MySQL with 4 logical databases (could split later)
- **Authentication:** JWT stateless tokens (no server state)
- **Message Queue:** RabbitMQ persists to disk (reliable)
- **Service Discovery:** Eureka (auto-registration/deregistration)
- **Monitoring:** Zipkin (trace requests), SonarQube (code quality)
- **Deployment:** Docker (reproducible) + Docker Compose (orchestration)
- **Scalability:** Horizontal (more instances), Vertical (more resources)

**Bottom Line:** Production-ready architecture, can explain every decision, shows understanding of distributed systems challenges.


