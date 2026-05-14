# SmartCourier - Interview Q&A Cheat Sheet

## Quick Questions & Answers

### Q1: What is the project about?
**A:** SmartCourier is a cloud-native delivery management system built with microservices. It allows customers to book courier services, track deliveries in real-time, and enables admins to manage the entire logistics operation. It's built with Spring Boot microservices, MySQL, RabbitMQ, and deployed via Docker.

---

### Q2: What microservices are in this system?
**A:**
1. **Auth Service** (8081) - User authentication & JWT tokens
2. **Delivery Service** (8082) - Booking, status management, charge calculation
3. **Tracking Service** (8083) - Delivery history, events, proof of delivery
4. **Admin Service** (8084) - Dashboard, reporting, user management
5. **API Gateway** (9090) - Single entry point, routing, JWT validation
6. **Eureka** (8761) - Service discovery & registration
7. **Config Server** (8889) - Centralized configuration

---

### Q3: How do services communicate with each other?
**A:** Two patterns:
1. **Synchronous (REST/Feign):** Direct HTTP calls for real-time needs
   - Example: Admin Service calls Delivery Service to fetch data
2. **Asynchronous (RabbitMQ):** Event-driven for loose coupling
   - Example: When Delivery status changes, publish event → Tracking Service listens and creates history

```java
// Async example from DeliveryService:
rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
// Tracking Service consumer receives and processes
```

---

### Q4: What does the Delivery Service do?
**A:** It's the core business logic service. Key responsibilities:
- Creates deliveries in DRAFT status (wizard) or BOOKED directly
- Manages delivery lifecycle (status transitions): DRAFT → BOOKED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
- Calculates shipping charges: `basePrice + (weight × 0.5) + fragileCharge`
- Enforces role-based access control (customers only DRAFT→BOOKED, admins have more)
- Publishes events to RabbitMQ when status changes
- Has Feign client to call Tracking Service

---

### Q5: What's the wizard workflow?
**A:** Step-by-step delivery creation (UX-friendly):
1. `POST /deliveries/draft` → Create DRAFT delivery, get ID
2. `PUT /deliveries/{id}/sender` → Add sender address
3. `PUT /deliveries/{id}/receiver` → Add receiver address
4. `PUT /deliveries/{id}/package` → Add package details, charge calculated
5. `PUT /deliveries/{id}/finalize` → Validate all complete, transition to BOOKED

Alternative: Single submission with all fields at once.

---

### Q6: How is tracking implemented?
**A:** 
- When Delivery Service updates status, it publishes event to RabbitMQ
- Tracking Service listens to these events
- Creates TrackingEvent entries in its database (complete history)
- Customer calls `GET /tracking/{deliveryId}/events` to see full timeline
- Optional: Delivery proof with signature/photos at completion

---

### Q7: What's the key data model for Delivery?
**A:**
```java
Delivery {
  id: Long (primary key)
  trackingNumber: String (unique, auto-generated "SC" + timestamp)
  username: String (customer who created it)
  status: DeliveryStatus enum
  charge: Double (calculated)
  paid: Boolean
  senderAddress: Address
  receiverAddress: Address
  parcelPackage: ParcelPackage {
    weight, length, width, height
    serviceType: DOMESTIC | EXPRESS | INTERNATIONAL
    declaredValue, fragile
  }
  specialInstructions: String
  scheduledPickup: LocalDateTime
  createdAt, updatedAt: Timestamps
}
```

---

### Q8: How is security handled?
**A:**
1. **Auth Service** issues JWT tokens on login
2. **API Gateway** validates JWT before routing
3. Extracts username & role from token
4. Propagates via headers: `X-User-Username: john`, `X-User-Role: CUSTOMER`
5. Service layer re-validates (e.g., Delivery Service checks if user owns delivery)
6. Three roles: CUSTOMER (limited), ADMIN (full access), SYSTEM (background operations)

```java
// Example from Delivery Service:
if ("CUSTOMER".equalsIgnoreCase(role) && 
    (initialStatus != DeliveryStatus.DRAFT && initialStatus != DeliveryStatus.BOOKED)) {
    throw new UnauthorizedAccessException("Customers can only create DRAFT or BOOKED");
}
```

---

### Q9: What's the charge calculation logic?
**A:**
```java
double basePrice;
if (serviceType == EXPRESS) basePrice = 14.99;
else if (serviceType == INTERNATIONAL) basePrice = 29.99;
else basePrice = 5.99; // DOMESTIC (default)

double weightCharge = weight * 0.5;
double fragileCharge = fragile ? 3.0 : 0;

charge = Math.round((basePrice + weightCharge + fragileCharge) * 100.0) / 100.0;
```

Example: DOMESTIC, 10kg, fragile = 5.99 + 5.0 + 3.0 = $13.99

---

### Q10: What happens if RabbitMQ fails?
**A:** Delivery Service has a fallback:
```java
try {
    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
} catch (Exception e) {
    log.warn("RabbitMQ failed, falling back to sync Feign call");
    sendTrackingEventSync(delivery, location, message);
}
```
Falls back to synchronous REST call to Tracking Service, ensuring data is always saved.

---

### Q11: How is role-based access controlled?
**A:**
- **CUSTOMER:** Can only create DRAFT/BOOKED for own deliveries
- **ADMIN:** Can view/update all deliveries, manage hubs, users, reports
- **SYSTEM:** Full access (background operations)

```java
// From canTransition() method:
if ("CUSTOMER".equalsIgnoreCase(role)) {
    if (!delivery.getUsername().equals(username)) {
        throw new UnauthorizedAccessException("Access denied");
    }
    return from == DeliveryStatus.DRAFT && to == DeliveryStatus.BOOKED;
}
if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
    return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
}
```

---

### Q12: What are valid delivery status transitions?
**A:**
```
DRAFT → BOOKED (customer finalizes)
BOOKED → PICKED_UP | FAILED
PICKED_UP → IN_TRANSIT | DELAYED | FAILED
IN_TRANSIT → OUT_FOR_DELIVERY | DELAYED | FAILED
OUT_FOR_DELIVERY → DELIVERED | FAILED | DELAYED
DELIVERED (terminal)
DELAYED → IN_TRANSIT | OUT_FOR_DELIVERY | FAILED
FAILED → RETURNED | IN_TRANSIT | OUT_FOR_DELIVERY
RETURNED (terminal)
```
Only admins can transition between most statuses; customers only DRAFT→BOOKED.

---

### Q13: How is the system deployed?
**A:** Docker Compose with multiple containers:
- MySQL (multi-database setup)
- RabbitMQ (async messaging)
- Zipkin (distributed tracing)
- Eureka (service discovery)
- Config Server (configuration)
- 4 microservices (Auth, Delivery, Tracking, Admin)
- API Gateway
- Frontend (Angular)
- SonarQube (code quality)

```bash
docker compose up --build
# Starts ~13 containers in an orchestrated manner
```

---

### Q14: What databases are used?
**A:**
- **Single MySQL instance** with 4 logical databases (database-per-service pattern):
  - `auth_db` - Auth Service
  - `delivery_db` - Delivery Service
  - `tracking_db` - Tracking Service (with TrackingEvent table, DeliveryProof table)
  - `admin_db` - Admin Service

Each service is isolated; no shared tables. Enables independent scaling and refactoring.

---

### Q15: How is testing and code quality ensured?
**A:**
- **JUnit + Spring Test:** Unit tests in each service
- **JaCoCo:** Measures code coverage percentage
- **SonarQube:** Static code analysis, vulnerability detection
- **Maven:** Build pipeline includes test + coverage + SonarQube scan

Each service has `src/test/java` with tests. Quality gates ensure coverage threshold before deployment.

---

### Q16: How would you scale this system?
**A:**
**Horizontal Scaling:**
- Run multiple instances of each service in Docker/Kubernetes
- Load balance via API Gateway or Kubernetes Service
- Eureka auto-discovers new instances
- RabbitMQ distributes work across workers

**Vertical Scaling:**
- Increase Docker container CPU/memory limits
- Database connection pool tuning
- RabbitMQ prefetch optimization

**Data Scaling:**
- Implement database sharding per service
- Archive old tracking events
- Cache frequently accessed data

---

### Q17: What observability tools are available?
**A:**
1. **Eureka Dashboard** (8761) - See service instances, status
2. **Zipkin** (9411) - Distributed tracing, latency analysis
3. **SonarQube** (9000) - Code quality metrics, vulnerabilities
4. **Adminer** (8080) - Database GUI for queries
5. **Application Logs** - Slf4j logs with correlation IDs (from Zipkin)

Example trace in Zipkin: Request → API Gateway → Delivery Service → Tracking Service with timing info.

---

### Q18: What are the key design patterns used?
**A:**
1. **Microservices Pattern** - Independently deployable services
2. **API Gateway Pattern** - Single entry point (Spring Cloud Gateway)
3. **Service Registry Pattern** - Eureka for dynamic discovery
4. **Event-Driven Architecture** - RabbitMQ async communication
5. **State Machine Pattern** - Delivery status transitions enforced
6. **Saga Pattern** - Multi-step workflows with compensation
7. **Database-per-Service** - Each service owns its schema
8. **Fallback Pattern** - RabbitMQ → Sync fallback for resilience

---

### Q19: What's in the API Gateway?
**A:**
- Routes incoming requests to microservices:
  ```
  /gateway/auth/** → Auth Service (8081)
  /gateway/deliveries/** → Delivery Service (8082)
  /gateway/tracking/** → Tracking Service (8083)
  /gateway/admin/** → Admin Service (8084)
  ```
- Validates JWT tokens before forwarding
- Adds security headers
- Propagates user context (username, role)
- Handles CORS

---

### Q20: How would you debug a production issue?
**A:**
1. **Check logs** - Service logs via Docker logs or aggregated logging
2. **View Zipkin traces** - Trace failing request across services, see timing
3. **Check Eureka** - Verify service is registered and UP
4. **Check RabbitMQ** - Verify messages in queues, no dead-letter queues
5. **Database checks** - Use Adminer to query data, check consistency
6. **Re-run in Postman** - Manually test the failing endpoint
7. **Check SonarQube** - Look for recent code quality issues

Example: Tracking data not appearing → Check if RabbitMQ consumer is running → Check Tracking Service logs → Check if message reached queue.

---

### Q21: Why use RabbitMQ instead of direct REST calls?
**A:**
- **Decoupling:** Delivery Service doesn't wait for Tracking Service
- **Scalability:** Can have multiple Tracking Service instances consuming from queue
- **Reliability:** Message persisted; retried if consumer fails
- **Peak Handling:** Queues absorb traffic spikes
- **Async:** Non-blocking calls, better throughput
- **Eventual Consistency:** Tracking data might lag, but will be consistent

Trade-off: Tracking data isn't immediately available; there's slight delay.

---

### Q22: What happens if Delivery Service goes down?
**A:**
- **Customers can't create/update deliveries** (obvious)
- **Tracking Service still works** - Can view historical data from database
- **Admin Service degraded** - Can't fetch delivery data (catches exception, returns limited info)
- **API Gateway returns 503** when routing to Delivery Service
- **Eureka deregisters the unhealthy instance** within ~30 seconds
- **Recovery:** Fix issue, restart service, Eureka re-registers automatically

---

### Q23: How many databases are there?
**A:**
- **One MySQL instance** running in Docker container
- **Four logical databases:**
  - auth_db
  - delivery_db
  - tracking_db
  - admin_db
- **Separate databases follow microservices principle** - Each service is independent
- **Could be split to separate MySQL instances** for production scaling

---

### Q24: What's the purpose of each service?

| Service | Purpose | Key Endpoints | Database |
|---------|---------|---------------|----------|
| Auth | User login, JWT tokens | POST /auth/login, POST /auth/register | auth_db |
| Delivery | Booking, status mgmt, charges | POST /deliveries, PUT /deliveries/{id}/status | delivery_db |
| Tracking | History, events, proof of delivery | GET /tracking/{id}/events, POST /tracking/{id}/proof | tracking_db |
| Admin | Dashboard, reporting, user mgmt | GET /admin/dashboard, POST /admin/reports | admin_db |
| API Gateway | Route requests, validate JWT | (all /gateway/** routes) | (none) |

---

### Q25: What's the complete user journey?

```
1. USER REGISTRATION
   POST /auth/register (username, password, role=CUSTOMER)
   
2. LOGIN
   POST /auth/login (username, password)
   ← Response: JWT token
   
3. CREATE DELIVERY (WIZARD)
   POST /deliveries/draft
   ← {id: 123, status: DRAFT}
   
   PUT /deliveries/123/sender, /receiver, /package
   
   PUT /deliveries/123/finalize
   ← {id: 123, status: BOOKED, charge: $13.99}
   
4. TRACK PACKAGE
   GET /tracking/123/events
   ← [{timestamp, status, location, message}, ...]
   
5. ADMIN VIEWS DASHBOARD
   GET /admin/dashboard (requires admin token)
   ← {stats, revenue, delivery distribution}
```

---

## Quick Glossary

- **JWT** - JSON Web Token, bearer token for stateless auth
- **RabbitMQ** - Message broker for async communication
- **Zipkin** - Distributed tracing system for debugging
- **Eureka** - Service discovery (Netflix component)
- **Feign** - Spring Cloud REST client for inter-service calls
- **Transactional** - Method runs in database transaction
- **Entity** - JPA mapped database table
- **DTO** - Data Transfer Object (request/response model)
- **AMQP** - Advanced Message Queuing Protocol (RabbitMQ protocol)
- **Spring Security** - Framework for authentication/authorization
- **@PreAuthorize** - Spring annotation to check roles before method execution

---

## Common Endpoints to Know

### Auth Service
```
POST /auth/register
POST /auth/login
GET /auth/validate
```

### Delivery Service
```
POST /deliveries - Create direct
POST /deliveries/draft - Init wizard
PUT /deliveries/{id}/sender - Update sender
PUT /deliveries/{id}/receiver - Update receiver
PUT /deliveries/{id}/package - Update package
PUT /deliveries/{id}/finalize - Finalize wizard
PUT /deliveries/{id}/status - Update status
GET /deliveries - Get all
GET /deliveries/{id} - Get by ID
GET /deliveries/tracking/{trackingNumber} - Track by number
```

### Tracking Service
```
GET /tracking/{deliveryId}/events - Get history
POST /tracking/{deliveryId}/proof - Add proof of delivery
GET /tracking/{deliveryId}/proof - Get proof
```

### Admin Service
```
GET /admin/dashboard
GET /admin/deliveries
GET /admin/users
POST /admin/reports
PUT /admin/deliveries/{id}/resolve - Resolve exception
```

### API Gateway (prefix /gateway)
```
All above routes prefixed with /gateway
Example: /gateway/deliveries/123
```

---

## Interview Tips

1. **Start with the big picture** - Microservices architecture, 7 services
2. **Explain the workflow** - Customer books → Status updates → Tracking
3. **Focus on design patterns** - Event-driven, microservices, API gateway
4. **Discuss trade-offs** - Complexity vs. scalability, eventual consistency
5. **Show code understanding** - Reference actual files (DeliveryService.java)
6. **Mention monitoring** - Zipkin, Eureka, SonarQube
7. **Address scaling** - Horizontal (more instances), vertical (more resources)
8. **Security** - JWT, role-based access, per-service validation
9. **Resilience** - RabbitMQ fallback, graceful degradation
10. **Data flow** - Show complete request path: User → Gateway → Service → DB

---

## Tough Questions (Be Prepared!)

### Q: How would you handle duplicate deliveries?
**A:** 
- Tracking number is unique (database constraint)
- Idempotent POST (check if exists by identity)
- Client-side deduplication (unique request ID)

### Q: What if tracking event is lost?
**A:**
- RabbitMQ persists messages to disk
- Consumer acknowledges only after saving to DB
- Dead-letter queue for failed messages
- Manual replay from admin service

### Q: How do you handle eventual consistency?
**A:**
- Tracking data might lag a few seconds
- Is OK for this use case (track timing doesn't need to be instant)
- Could add polling if needed (customer refreshes page)
- Could use WebSocket for real-time updates

### Q: What if admin deletes a user while they have pending deliveries?
**A:**
- Deliveries remain (no cascade delete)
- Orphaned deliveries can be managed by admin service
- Could soft-delete (mark inactive) instead

### Q: How do you test across services?
**A:**
- Unit tests for each service independently
- Integration tests with embedded databases
- Contract testing (verify service interfaces)
- Docker Compose for full system test
- SonarQube to enforce coverage


