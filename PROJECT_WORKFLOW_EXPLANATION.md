# SmartCourier Delivery Management System - Project Workflow

## Executive Summary
SmartCourier is a **microservices-based delivery management platform** designed to handle courier operations at scale. It allows customers to book deliveries, track shipments in real-time, and enables admins to manage the entire delivery lifecycle. The system is built using **Spring Boot microservices** with event-driven architecture and distributed tracing.

---

## System Architecture Overview

### Architecture Pattern: Microservices with API Gateway

```
┌─────────────────────────────────────────────────────────────────┐
│                     Angular Frontend (Port 4200)                 │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP Requests
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│               API Gateway (Port 9090)                            │
│     • Centralized request routing                               │
│     • JWT validation & authentication                           │
│     • Request/response transformation                           │
└──────────┬──────────┬──────────┬──────────┬────────────────────┘
           │          │          │          │
    ┌──────▼──┐ ┌────▼────┐ ┌───▼────┐ ┌──▼────┐
    │  Auth   │ │ Delivery│ │ Track  │ │ Admin │
    │ Service │ │ Service │ │Service │ │Service│
    │(8081)   │ │ (8082)  │ │(8083)  │ │(8084) │
    └──────┬──┘ └────┬────┘ └───┬────┘ └──┬────┘
           │         │          │         │
           └─────────┴──────────┴─────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
   ┌────▼───┐  ┌─────▼──────┐ ┌──▼────────┐
   │ MySQL  │  │ RabbitMQ   │ │ Eureka    │
   │ (Multi-│  │(Messaging) │ │(Service   │
   │Database)  │            │ │Registry)  │
   └────────┘  └────────────┘ └───────────┘
```

---

## Core Services & Responsibilities

### 1. **Auth Service** (Port 8081)
**Purpose:** User authentication and authorization

**Key Features:**
- Registers new users (CUSTOMER, ADMIN roles)
- Authenticates users and issues JWT tokens
- Validates JWT tokens on behalf of other services
- Manages user credentials and roles

**Database:** `auth_db`

**Key Endpoints:**
- `POST /auth/register` - Register new user
- `POST /auth/login` - Authenticate user
- `GET /auth/validate` - Validate JWT token

---

### 2. **Delivery Service** (Port 8082)
**Purpose:** Core business logic for managing deliveries

**Key Responsibilities:**
- Create and manage delivery bookings
- Handle delivery lifecycle (DRAFT → BOOKED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED)
- Calculate shipping charges based on weight, service type, and fragility
- Enforce state transition rules (e.g., customers can only DRAFT→BOOKED, admins have more flexibility)
- Support two workflow modes:
  - **Direct Submission:** Create complete delivery in one request
  - **Wizard Flow:** Step-by-step delivery creation (Sender → Receiver → Package → Finalize)

**Database:** `delivery_db`

**Entity Model:**
```
Delivery
├── id (Long)
├── trackingNumber (String, unique)
├── username (String)
├── status (DeliveryStatus enum)
├── charge (Double)
├── paid (Boolean)
├── senderAddress (Address)
├── receiverAddress (Address)
├── parcelPackage (ParcelPackage)
│   ├── weight, length, width, height
│   ├── description
│   ├── serviceType (DOMESTIC, EXPRESS, INTERNATIONAL)
│   ├── declaredValue
│   └── fragile (Boolean)
├── specialInstructions
├── scheduledPickup (LocalDateTime)
└── timestamps (createdAt, updatedAt)
```

**Key Endpoints:**
- `POST /deliveries` - Create delivery (direct)
- `POST /deliveries/draft` - Init wizard draft
- `PUT /deliveries/{id}/sender` - Update sender address
- `PUT /deliveries/{id}/receiver` - Update receiver address
- `PUT /deliveries/{id}/package` - Update package details
- `PUT /deliveries/{id}/finalize` - Finalize draft to BOOKED
- `PUT /deliveries/{id}/status` - Update delivery status
- `GET /deliveries/{id}` - Get delivery by ID
- `GET /deliveries` - Get all deliveries
- `GET /deliveries/tracking/{trackingNumber}` - Track by number

**Status Transitions (State Machine):**
```
DRAFT → BOOKED (customer finalizes wizard)
BOOKED → PICKED_UP | FAILED (provider picks up)
PICKED_UP → IN_TRANSIT | DELAYED | FAILED
IN_TRANSIT → OUT_FOR_DELIVERY | DELAYED | FAILED
OUT_FOR_DELIVERY → DELIVERED | FAILED | DELAYED
DELIVERED → (terminal state)
FAILED → RETURNED | IN_TRANSIT (retry)
DELAYED → IN_TRANSIT | OUT_FOR_DELIVERY | FAILED
RETURNED → (terminal state)
```

---

### 3. **Tracking Service** (Port 8083)
**Purpose:** Real-time tracking and delivery history

**Key Features:**
- Maintains delivery event history (every status change creates an event)
- Accepts tracking events from Delivery Service (via RabbitMQ)
- Stores proof of delivery (POD) - signatures, photos, recipient confirmation
- Provides delivery timeline/history view
- File upload for delivery proof documents

**Database:** `tracking_db`

**Entities:**
- `TrackingEvent` - Location, status, timestamp, message
- `DeliveryProof` - Recipient name, signature, photo URLs, notes

**Key Endpoints:**
- `GET /tracking/{deliveryId}/events` - Get delivery history
- `POST /tracking/{deliveryId}/proof` - Submit delivery proof
- `GET /tracking/{deliveryId}/proof` - Get delivery proof

---

### 4. **Admin Service** (Port 8084)
**Purpose:** Administrative operations and system management

**Key Features:**
- Dashboard with delivery statistics and system health
- View all deliveries (ADMIN role only)
- Manage delivery hubs/locations
- User management (create, update, delete users)
- Report generation (delivery metrics, performance, etc.)
- Exception resolution (handle exceptional delivery cases)

**Database:** `admin_db`

**Key Endpoints:**
- `GET /admin/dashboard` - Dashboard data
- `GET /admin/deliveries` - All deliveries
- `GET /admin/hubs` - List delivery hubs
- `POST /admin/hubs` - Create hub
- `GET /admin/users` - List users
- `POST /admin/users` - Create user
- `PUT /admin/deliveries/{id}/resolve` - Resolve delivery exception
- `POST /admin/reports` - Generate report

---

### 5. **API Gateway** (Port 9090) - Spring Cloud Gateway
**Purpose:** Single entry point for all client requests

**Responsibilities:**
- Request routing to microservices
- JWT token validation before forwarding to services
- User context headers propagation (X-User-Username, X-User-Role)
- Cross-cutting concerns (CORS, rate limiting preparation)

**Client Route Mapping:**
```
/gateway/auth/**      → Auth Service (8081)
/gateway/deliveries/** → Delivery Service (8082)
/gateway/tracking/**  → Tracking Service (8083)
/gateway/admin/**     → Admin Service (8084)
/gateway/services/**  → Delivery Service (8082) - catalog
```

---

### 6. **Service Registry (Eureka)** (Port 8761)
**Purpose:** Service discovery and registration

**Benefits:**
- All microservices automatically register themselves
- Enables dynamic service-to-service communication
- Provides health checks and automatic deregistration of unhealthy services
- Used by Spring Cloud Load Balancer for client-side load balancing

---

### 7. **Config Server** (Port 8889)
**Purpose:** Centralized configuration management

**Features:**
- Stores service configurations in `config-repo` directory
- Services fetch configuration on startup
- Enables hot configuration updates without redeployment
- Configuration files: `auth-service.yml`, `delivery-service.yml`, etc.

---

## Communication Patterns

### Pattern 1: **Synchronous (REST/HTTP)**
- Client → API Gateway → Service
- Service → Service (via Feign client for inter-service calls)
- Example: Admin Service calls Delivery Service to fetch delivery data

**Code Example (from Delivery Service):**
```java
// Delivery Service publishes event synchronously to Tracking Service
sendTrackingEventSync(delivery, location, message);
// Uses TrackingClient (Feign) to call Tracking Service REST API
```

### Pattern 2: **Asynchronous (RabbitMQ / Event-Driven)**
- When Delivery status changes, publish a `DeliveryEvent` to RabbitMQ
- Tracking Service listens to these events and creates history records
- Provides decoupling and scale-out capability

**Flow:**
```
1. Delivery Service updates delivery status
2. Publishes DeliveryStatusChangedEvent
3. Publishes async event to RabbitMQ exchange: "delivery.status"
4. Tracking Service consumes this message
5. Creates corresponding TrackingEvent in its database
6. Updates delivery history/timeline
```

**Code Example (from Delivery Service):**
```java
private void publishStatusEvent(Delivery delivery, String location, String message) {
    DeliveryEvent event = DeliveryEvent.builder()
        .deliveryId(delivery.getId())
        .trackingNumber(delivery.getTrackingNumber())
        .status(delivery.getStatus().name())
        .location(location)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
    
    try {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
    } catch (Exception e) {
        // Fallback to synchronous call if RabbitMQ fails
        sendTrackingEventSync(delivery, location, message);
    }
}
```

---

## Key Business Workflows

### Workflow 1: Customer Creates and Books a Delivery (Wizard Flow)

```
Step 1: Init Draft
  POST /deliveries/draft
  Response: {id: 123, status: DRAFT, trackingNumber: SC1704200000XXX}
  
Step 2: Add Sender Address
  PUT /deliveries/123/sender
  Body: {fullName, phone, street, city, state, zipCode, country}
  
Step 3: Add Receiver Address
  PUT /deliveries/123/receiver
  Body: {fullName, phone, street, city, state, zipCode, country}
  
Step 4: Add Package Details
  PUT /deliveries/123/package
  Body: {weight, length, width, height, description, serviceType, declaredValue, fragile}
  Response includes calculated charge: price = basePrice + (weight × 0.5) + fragileCharge
  
Step 5: Finalize Delivery
  PUT /deliveries/123/finalize
  - Validates all fields are complete
  - Transitions from DRAFT → BOOKED
  - Publishes events to RabbitMQ
  - Tracking Service creates initial history entry
  - Response: {id: 123, status: BOOKED}
  
Result: Delivery is now active and awaiting pickup
```

### Workflow 2: Delivery Lifecycle (Status Updates)

```
Customer calls finalize
  ↓
BOOKED
  ↓ (Delivery partner picks up at scheduled time)
PICKED_UP → publishes event → Tracking Service creates event
  ↓
IN_TRANSIT → publishes event
  ↓
OUT_FOR_DELIVERY → publishes event
  ↓
DELIVERED → publishes event
  ↓ (Optional: Add proof of delivery with signature/photo)
[Proof Added]
```

### Workflow 3: Customer Tracks Delivery

```
GET /tracking/{deliveryId}/events
Response: [
  {timestamp: 2024-05-01 10:00, status: BOOKED, location: null, message: "Delivery confirmed"},
  {timestamp: 2024-05-01 11:30, status: PICKED_UP, location: "Hub A", message: "Package picked up"},
  {timestamp: 2024-05-01 14:00, status: IN_TRANSIT, location: "Route 5", message: "On the way"},
  {timestamp: 2024-05-01 16:45, status: OUT_FOR_DELIVERY, location: "Near address", message: "Out for delivery"},
  {timestamp: 2024-05-01 17:20, status: DELIVERED, location: "Delivered", message: "Delivered successfully"}
]
```

### Workflow 4: Admin Dashboard & Reporting

```
Admin views dashboard:
  GET /admin/dashboard
  Response: {
    totalDeliveries: 1450,
    statusDistribution: {
      DELIVERED: 1300,
      IN_TRANSIT: 120,
      BOOKED: 30
    },
    revenue: 45000,
    averageDeliveryTime: 3.2 days
  }

Admin generates report:
  POST /admin/reports?type=DELIVERY_METRICS&title=May 2024 Report
  Response: Report ID, charts, summary statistics
```

---

## Technology Stack

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.2.0
- **Microservices:** Spring Cloud (Eureka, Gateway, Feign, Config)
- **Database:** MySQL 8.0 (4 separate databases)
- **Messaging:** RabbitMQ 3.12
- **ORM:** Spring Data JPA with Hibernate
- **Build Tool:** Maven
- **Build/Container:** Docker & Docker Compose

### Frontend
- **Framework:** Angular
- **Styling:** CSS
- **Build:** Angular CLI (ng serve on port 4200)

### DevOps & Monitoring
- **Service Registry:** Netflix Eureka
- **Distributed Tracing:** Zipkin (port 9411)
- **Code Quality:** SonarQube (port 9000)
- **Code Coverage:** JaCoCo
- **Database Admin UI:** Adminer (port 8080)

### Security
- **Authentication:** JWT (JSON Web Tokens)
- **Spring Security:** Role-based access control (CUSTOMER, ADMIN, SYSTEM)
- **Rate Limiting:** API Gateway ready

---

## Deployment Model

### Docker Compose Services

**Development/Testing Environment:**

```yaml
Services:
├── MySQL (3307) - Multi-database setup
├── RabbitMQ (5672/15672) - Message broker + Management UI
├── Zipkin (9411) - Distributed tracing
├── Eureka (8761) - Service discovery
├── Config Server (8889) - Configuration management
├── Auth Service (8081) - JWT generation/validation
├── Delivery Service (8082) - Core business logic
├── Tracking Service (8083) - History & tracking
├── Admin Service (8084) - Admin operations
├── API Gateway (9090) - Entry point
├── SonarQube (9000) - Code analysis
└── Frontend (4200) - Angular application
```

**Key Environment Variables:**
```
MYSQL_ROOT_PASSWORD: system
RABBITMQ_DEFAULT_USER: guest / RABBITMQ_DEFAULT_PASS: guest
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://service-registry:8761/eureka/
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/{db_name}
```

---

## Data Flow Example: Complete Delivery Creation & Tracking

```
┌─────────────────────────────────────────────────────────────────────┐
│ CUSTOMER INITIATES DELIVERY                                         │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ├─ POST /gateway/deliveries/draft
                     ├─ API Gateway validates JWT
                     └─→ Delivery Service creates DRAFT delivery
                       └─→ Saved to delivery_db
                       └─ Returns: {id: 123, status: DRAFT}
                     │
    ┌────────────────┴────────────────┐
    │ CUSTOMER COMPLETES WIZARD STEPS │
    └────────────────┬────────────────┘
                     │
    ┌────────────────┴────────────────┐
    │                                  │
PUT /sender              PUT /receiver  PUT /package
    │                        │            │
    └────────────────────────┴────────────┘
                     │
    ┌────────────────▼────────────────────────┐
    │ PUT /deliveries/123/finalize           │
    │ Delivery: DRAFT → BOOKED                │
    └────────────────┬────────────────────────┘
                     │
                     ├─ Save to delivery_db
                     ├─ Publish RabbitMQ Event: "BOOKED"
                     │      │
                     │      └─→ Tracking Service listens
                     │          └─ Creates initial TrackingEvent
                     │          └─ Saves to tracking_db
                     │
                     └─ Publish ApplicationEvent
                        └─ Event listeners notified
                        └─ Response: {status: BOOKED}

┌─────────────────────────────────────────────────────────────────────┐
│ DELIVERY PARTNER UPDATES STATUS (PICKED_UP)                         │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ├─ PUT /deliveries/123/status?status=PICKED_UP
                     ├─ Validate role (ADMIN/SYSTEM)
                     ├─ Validate state transition (BOOKED→PICKED_UP OK)
                     │
                     ├─ Update delivery_db: status = PICKED_UP
                     │
                     ├─ publishStatusEvent() publishes RabbitMQ message:
                     │  {
                     │    deliveryId: 123,
                     │    trackingNumber: SC1704200000XXX,
                     │    status: PICKED_UP,
                     │    location: "Hub A",
                     │    message: "Package picked up",
                     │    timestamp: 2024-05-01 11:30
                     │  }
                     │
                     ├─→ RabbitMQ Message Broker receives
                     │   └─ Tracking Service consumer processes
                     │      └─ Creates TrackingEvent in tracking_db
                     │      └─ Delivery history updated
                     │
                     └─ Response: {id: 123, status: PICKED_UP}

┌─────────────────────────────────────────────────────────────────────┐
│ CUSTOMER TRACKS DELIVERY                                            │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ├─ GET /gateway/tracking/123/events
                     ├─ API Gateway validates JWT
                     └─→ Tracking Service queries tracking_db
                        └─ Returns all TrackingEvents for delivery 123
                        └─ Response: [event1, event2, event3, ...]

┌─────────────────────────────────────────────────────────────────────┐
│ DELIVERY COMPLETE - ADD PROOF OF DELIVERY                           │
└────────────────────┬────────────────────────────────────────────────┘
                     │
                     ├─ POST /tracking/123/proof
                     │   Body: {recipientName, signatureUrl, photoUrl, notes}
                     │
                     └─→ Tracking Service saves DeliveryProof
                        └─ Associates with delivery 123
                        └─ Response: {proof_id, ...}
```

---

## Key Design Patterns Used

### 1. **Microservice Pattern**
- Each service has its own database (database-per-service)
- Loose coupling, independent scaling
- Services communicate via REST or messaging

### 2. **Event-Driven Architecture**
- RabbitMQ enables async communication
- Decouples Delivery Service from Tracking Service
- Enables scalability and fault tolerance

### 3. **API Gateway Pattern**
- Single entry point for all clients
- Centralized security and routing
- Abstracts backend complexity from frontend

### 4. **Service Registry Pattern (Eureka)**
- Dynamic service discovery
- Health checking and automatic deregistration
- Enables zero-downtime deployments

### 5. **Configuration Management Pattern**
- Externalized configuration (Config Server)
- Environment-specific properties
- Hot configuration reload capability

### 6. **Saga Pattern** (Implicit)
- Delivery workflow is a saga with steps
- Each step published as event
- Compensation in case of failures (e.g., FAILED → RETURNED)

### 7. **State Machine Pattern**
- Delivery lifecycle enforced by state transitions
- Prevents invalid state changes
- `ALLOWED_TRANSITIONS` map defines valid transitions

---

## Security Architecture

### Authentication Flow
```
1. Customer registers: POST /auth/register
   ↓
2. Customer logs in: POST /auth/login
   Request: {username, password}
   Response: {token: "eyJhbGc..."}
   ↓
3. Subsequent requests include Authorization header:
   Authorization: Bearer eyJhbGc...
   ↓
4. API Gateway validates JWT
   ↓
5. Token decoded to extract: username, role
   ↓
6. Headers propagated to backend services:
   X-User-Username: john_doe
   X-User-Role: CUSTOMER
   ↓
7. Service layer performs role-based access control
   Example: Delivery Service checks if user owns the delivery
   if (!delivery.getUsername().equals(username)) {
       throw new UnauthorizedAccessException(...)
   }
```

### Role-Based Access Control (RBAC)
- **CUSTOMER:** Can create deliveries, view own deliveries, track their packages
- **ADMIN:** Can view all deliveries, manage hubs, users, generate reports
- **SYSTEM:** Has administrative access for background operations

---

## Error Handling & Resilience

### Exception Hierarchy
```
BusinessRuleViolationException - Invalid business operation
InvalidStatusTransitionException - Illegal state change
UnauthorizedAccessException - User not allowed
ResourceNotFoundException - Resource doesn't exist
```

### Resilience Patterns
1. **Fallback Strategy**
   - RabbitMQ fails → Falls back to synchronous Feign call to Tracking Service
   - Ensures tracking data is always persisted

2. **Graceful Degradation**
   - Service Registry health checks
   - Automatic unhealthy service removal

3. **Retry Logic**
   - RabbitMQ automatic retries
   - Exponential backoff (configurable)

---

## Performance Considerations

### Optimization Strategies
1. **Database Indexing**
   - Tracking number (unique index)
   - User ID (for faster queries)
   - Status (for filtering)

2. **Asynchronous Processing**
   - RabbitMQ for non-blocking updates
   - Enables high throughput

3. **Service Discovery Caching**
   - Eureka caches service instances
   - Reduces latency on repeated calls

4. **Distributed Tracing (Zipkin)**
   - Identifies bottlenecks
   - Monitors inter-service communication latency
   - Helps with debugging in production

---

## Testing & Code Quality

### Code Coverage Tools
- **JaCoCo:** Measures test coverage
- **SonarQube:** Code quality analysis, vulnerability detection
- Each service has unit tests (in `src/test/java`)

### Deployment Quality Gate
```
Tests must pass     → Code coverage > 70%     → SonarQube passes
                                    ↓
                            Build Docker image
                                    ↓
                            Push to registry
                                    ↓
                            Deploy to environment
```

---

## Scaling Strategy

### Horizontal Scaling
- Multiple instances of each service can run
- Load balanced by API Gateway
- Eureka automatically discovers new instances

### Vertical Scaling
- Adjust Docker container resource limits
- Database connection pool optimization
- RabbitMQ queue optimization

### Data Scaling
- Each service has its own database (no shared tables)
- Can implement database sharding per service if needed
- Tracking Service has persistent file uploads directory

---

## Monitoring & Observability

### Available Dashboards
1. **Eureka Dashboard** (http://localhost:8761)
   - View all registered services
   - Instance status (UP, DOWN)
   - Instance metadata

2. **Zipkin Dashboard** (http://localhost:9411)
   - Trace individual requests across services
   - Latency analysis
   - Error tracking

3. **SonarQube Dashboard** (http://localhost:9000)
   - Code quality metrics
   - Vulnerability detection
   - Test coverage reporting

4. **Adminer UI** (http://localhost:8080)
   - Database management
   - Query execution
   - Schema exploration

---

## Summary: Key Takeaways for Interview

### Strengths of This Architecture
1. ✅ **Scalability** - Each service scales independently
2. ✅ **Resilience** - Failures isolated to single service
3. ✅ **Maintainability** - Clear separation of concerns
4. ✅ **Flexibility** - Each service can use different tech stack
5. ✅ **Observability** - Distributed tracing for debugging
6. ✅ **Async First** - RabbitMQ enables non-blocking operations

### Trade-offs
1. ⚠️ **Complexity** - More services to manage and monitor
2. ⚠️ **Data Consistency** - Eventual consistency model (not ACID across services)
3. ⚠️ **Network Latency** - Inter-service communication adds latency
4. ⚠️ **Testing Complexity** - End-to-end testing is more challenging
5. ⚠️ **Operational Overhead** - Requires monitoring and logging infrastructure

### Production-Ready Features
- ✅ JWT authentication and authorization
- ✅ Distributed tracing (Zipkin)
- ✅ Service health checks
- ✅ Configuration management
- ✅ Docker containerization
- ✅ Code quality analysis (SonarQube)
- ✅ Test coverage reporting (JaCoCo)
- ✅ Role-based access control
- ✅ Event-driven architecture with RabbitMQ
- ✅ Graceful error handling and fallbacks

---

## Quick Start for Interview Demo

```bash
# Start all services with Docker
docker compose up --build

# Wait 2-3 minutes for services to start

# Access dashboards:
# - Eureka: http://localhost:8761
# - Zipkin: http://localhost:9411
# - SonarQube: http://localhost:9000
# - API Gateway: http://localhost:9090
# - Frontend: http://localhost:4200
# - Adminer DB: http://localhost:8080
```


