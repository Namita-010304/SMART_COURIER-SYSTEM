# SmartCourier - System Design Principles & Patterns

## Overview
SmartCourier implements multiple industry-standard system design principles and architectural patterns. Below is a detailed breakdown with code examples.

---

## 1. MICROSERVICES ARCHITECTURE

### Principle
The system is decomposed into small, independent, loosely-coupled services that can be developed, deployed, and scaled independently.

### Implementation in SmartCourier
```
7 separate microservices:
├── Auth Service (8081)      - Focused on authentication
├── Delivery Service (8082)  - Focused on delivery management
├── Tracking Service (8083)  - Focused on tracking & history
├── Admin Service (8084)     - Focused on administration
├── API Gateway (9090)       - Focused on routing & gateway
├── Eureka (8761)           - Service discovery
└── Config Server (8889)    - Configuration management
```

### Benefits
✓ **Scalability** - Scale only the services that need it (e.g., scale Delivery Service during booking rush)
✓ **Independence** - Teams can work on different services simultaneously
✓ **Technology Flexibility** - Each service could use different tech stacks
✓ **Fault Isolation** - If one service fails, others continue operating
✓ **Deployment Flexibility** - Deploy services independently without coordinating with entire system

### Code Reference
DeliveryService is focused ONLY on delivery business logic:
- Create deliveries
- Manage status
- Calculate charges
- Publish events

It doesn't handle tracking, user management, or admin functions (those are other services).

---

## 2. EVENT-DRIVEN ARCHITECTURE

### Principle
Services communicate through events published to a message broker, enabling loose coupling and asynchronous processing.

### Implementation in SmartCourier

```java
// From DeliveryService.java - Publishing events

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
        // Primary: RabbitMQ (Async)
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
    } catch (Exception e) {
        // Fallback: Sync REST call
        sendTrackingEventSync(delivery, location, message);
    }
}
```

### Flow
```
Delivery Service                RabbitMQ              Tracking Service
    │                             │                         │
    ├─ Status changes             │                         │
    ├─ Create event               │                         │
    ├─ Publish to queue ─────────►│                         │
    └─ Returns immediately        │                         │
    (non-blocking)                │                         │
                                  ├─ Event queued           │
                                  ├─ Persisted to disk      │
                                  │                         │
                                  ├─ Consumer listens ──────┤
                                  │                         │
                                  │                  ┌─ Deserialize
                                  │                  ├─ Process event
                                  │                  ├─ Save to DB
                                  │                  ├─ Send ACK
                                  │                  └─ Remove from queue
```

### Benefits
✓ **Loose Coupling** - Sender doesn't know who receivers are
✓ **Scalability** - Multiple receivers can process same event independently
✓ **Reliability** - Messages persisted to disk, retried if needed
✓ **Async Processing** - Non-blocking, better throughput
✓ **Peak Handling** - Queues absorb traffic spikes

### Code Reference
- Event publishing happens in `DeliveryService.publishStatusEvent()`
- Tracking Service listens via `@RabbitListener` (consumed from queue)
- Fallback mechanism ensures eventual consistency

---

## 3. API GATEWAY PATTERN

### Principle
A single entry point routes all client requests to appropriate backend services, handling cross-cutting concerns like authentication and routing.

### Implementation in SmartCourier

```
Clients (Browser, Mobile, API)
         │
         ▼
┌─────────────────────────────────┐
│       API Gateway (9090)        │
│ (Spring Cloud Gateway)          │
├─────────────────────────────────┤
│ • JWT Validation                │
│ • Request Routing               │
│ • Header Enrichment             │
│ • CORS Handling                 │
│ • Rate Limiting (ready)         │
└─────────────────────────────────┘
         │
    ┌────┼────┬────┬────┐
    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼
  Auth Delivery Tracking Admin  ...
  (8081) (8082) (8083) (8084)
```

### Benefits
✓ **Single Entry Point** - Clients don't need to know service URLs
✓ **Centralized Security** - All requests validated in one place
✓ **Service Location Abstraction** - Can move services without impacting clients
✓ **Cross-Cutting Concerns** - Logging, rate limiting, CORS in one place
✓ **Load Balancing** - Gateway can load balance across service instances

### Code Reference
API routes defined for:
- `/gateway/auth/**` → Auth Service
- `/gateway/deliveries/**` → Delivery Service
- `/gateway/tracking/**` → Tracking Service
- `/gateway/admin/**` → Admin Service

---

## 4. SERVICE DISCOVERY PATTERN

### Principle
Services dynamically register themselves and discover other services without hardcoding URLs. Netflix Eureka handles this.

### Implementation in SmartCourier

```
┌──────────────────────────────────┐
│  Eureka Service Registry (8761)  │
│                                  │
│  Registered Services:            │
│  ├─ auth-service:8081 (UP)      │
│  ├─ delivery-service:8082 (UP)  │
│  ├─ tracking-service:8083 (UP)  │
│  ├─ admin-service:8084 (UP)     │
│  └─ api-gateway:9090 (UP)       │
└──────────────────────────────────┘
         ▲
         │ Heartbeat every 30s
         │
    ┌────┴────┬────────┬────────┐
    │          │        │        │
 Auth      Delivery Tracking  Admin
Service    Service  Service  Service

On startup:
1. Service registers itself with Eureka
2. Sends periodic heartbeats
3. If heartbeat stops for 3 periods → deregistered

When calling another service:
1. Ask Eureka: "Where is delivery-service?"
2. Eureka responds with instance URL
3. Call service at that URL
4. If service down, Eureka marks as DOWN
5. API Gateway stops routing to it
```

### Benefits
✓ **Dynamic Discovery** - Services can start/stop without reconfiguration
✓ **Load Balancing** - Multiple instances automatically discovered
✓ **Health Monitoring** - Automatic detection of unhealthy services
✓ **Scalability** - Add/remove instances on the fly
✓ **Resilience** - Unhealthy services automatically deregistered

### Code Reference
- Services use `@EnableEurekaClient` annotation
- Eureka URL configured: `spring.eureka.client.serviceurl.defaultzone`
- Feign clients use service names (not URLs): `@FeignClient("delivery-service")`

---

## 5. CONFIGURATION MANAGEMENT PATTERN

### Principle
Centralize application configuration in a Config Server, allowing changes without redeployment.

### Implementation in SmartCourier

```
┌──────────────────────────────┐
│  Config Server (8889)        │
│  (Spring Cloud Config)       │
│                              │
│  Stores configs in:          │
│  /config-repo/               │
│  ├─ auth-service.yml         │
│  ├─ delivery-service.yml     │
│  ├─ tracking-service.yml     │
│  ├─ admin-service.yml        │
│  └─ api-gateway.yml          │
└──────────────────────────────┘
         ▲
         │ Fetch config on startup
         │
    ┌────┴────┬────────┬────────┐
    │          │        │        │
 Auth      Delivery Tracking  Admin
Service    Service  Service  Service

Startup sequence:
1. Service boots
2. Connects to Config Server
3. Fetches configuration based on service name
4. Applies configuration
5. Proceeds with startup
```

### Benefits
✓ **Centralized Management** - Change config in one place
✓ **Environment Parity** - Same service code, different configs per environment
✓ **Hot Reload** - Can update configs without redeployment (with `@RefreshScope`)
✓ **Profile Support** - Different configs for dev/staging/prod
✓ **Git Backed** - Version control for configurations

### Code Reference
- Configuration stored in `config-repo/` directory
- Services use Spring Cloud Config client
- Configured via `spring.config.import=configserver:`

---

## 6. DATABASE-PER-SERVICE PATTERN

### Principle
Each microservice owns its own database schema, ensuring loose coupling and independent evolution.

### Implementation in SmartCourier

```
┌─────────────────────────────────────────────┐
│            MySQL Instance                   │
│                                             │
│  ├─ auth_db (Auth Service)                 │
│  │  └─ user, role tables                   │
│  │                                         │
│  ├─ delivery_db (Delivery Service)         │
│  │  └─ delivery, address, parcel_package   │
│  │                                         │
│  ├─ tracking_db (Tracking Service)         │
│  │  └─ tracking_event, delivery_proof      │
│  │                                         │
│  └─ admin_db (Admin Service)               │
│     └─ hub, report tables                  │
│                                             │
│  Key: NO SHARED TABLES                      │
│  Each service is isolated                   │
└─────────────────────────────────────────────┘
```

### Benefits
✓ **Independence** - Services can evolve independently
✓ **No Shared State** - No database-level dependencies
✓ **Scalability** - Can split to different database servers in future
✓ **Technology Flexibility** - Each service could use different DB (future)
✓ **Team Autonomy** - Each team owns their data schema

### Code Reference
- Each service has its own `persistence.xml` or Spring Data configuration
- DeliveryRepository connects only to `delivery_db`
- TrackingRepository connects only to `tracking_db`
- No cross-database joins

### Tradeoff: Eventual Consistency
```
Multi-service operation isn't ACID anymore:
1. Delivery Service updates status ✓
2. Publishes event to RabbitMQ
3. Tracking Service receives event
4. Tracking Service updates tracking_db
   (may lag 1-2 seconds)

Result: Eventually consistent (not immediately consistent)
This is acceptable for delivery tracking use case.
```

---

## 7. STATE MACHINE PATTERN

### Principle
Explicit, validated state transitions prevent invalid states and enforce business rules.

### Implementation in SmartCourier

```java
// From DeliveryService.java

private static final Map<DeliveryStatus, List<DeliveryStatus>> ALLOWED_TRANSITIONS = Map.of(
    DeliveryStatus.DRAFT, 
        List.of(DeliveryStatus.BOOKED),
    DeliveryStatus.BOOKED, 
        List.of(DeliveryStatus.PICKED_UP, DeliveryStatus.FAILED),
    DeliveryStatus.PICKED_UP, 
        List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.DELAYED, DeliveryStatus.FAILED),
    DeliveryStatus.IN_TRANSIT, 
        List.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.DELAYED, DeliveryStatus.FAILED),
    DeliveryStatus.OUT_FOR_DELIVERY, 
        List.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED, DeliveryStatus.DELAYED),
    DeliveryStatus.DELIVERED, 
        List.of(),  // Terminal state
    // ... more states
);

// Validate transitions
private boolean canTransition(String role, DeliveryStatus from, DeliveryStatus to, 
                             Delivery delivery, String username) {
    if ("CUSTOMER".equalsIgnoreCase(role)) {
        // Customers can only DRAFT -> BOOKED
        if (!delivery.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("Access denied");
        }
        return from == DeliveryStatus.DRAFT && to == DeliveryStatus.BOOKED;
    }

    if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
        // Admins can use any allowed transition
        return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
    }

    return false;
}
```

### State Diagram
```
DRAFT ─────────────────┐
  │                    │
  └─► BOOKED ─────────┬┴─► PICKED_UP ┬─► IN_TRANSIT ─┬─► OUT_FOR_DELIVERY ─► DELIVERED ✓
  ↑   ↑               │    ↑   ▲    │     ↑  ▲      ▼
  │   └─FAILED        │    │   │    │     │  │   DELAYED
  │   ↑   │           │    │   │    │     │  └────┐
  │   │   ▼           │    │   │    │     │       │
  │   └───────────────│────└───┼────└─────┴───────┘
  │                   │        │
  └───────────────────┴─► FAILED ──┬─► RETURNED ✓
                             ▲     │
                             └─────┘ (Retry)

Status progression:
✓ DRAFT → BOOKED (Customer finalizes)
✓ BOOKED → PICKED_UP (Valid next step)
✗ BOOKED → DELIVERED (INVALID - must go through PICKED_UP)
✓ IN_TRANSIT → DELAYED (Temporarily pause)
✓ DELAYED → IN_TRANSIT (Resume)
✓ OUT_FOR_DELIVERY → DELIVERED (Complete)
✓ FAILED → RETURNED (Mark as returned)
```

### Benefits
✓ **Prevents Invalid States** - Can't skip intermediate states
✓ **Clear Business Logic** - State transitions documented explicitly
✓ **Easy Testing** - Can test all valid/invalid transitions
✓ **Audit Trail** - Clear history of how delivery evolved
✓ **Role-Based Rules** - Different rules for different roles

### Code Reference
- `ALLOWED_TRANSITIONS` map defines all valid transitions
- `canTransition()` method validates before allowing change
- Exception thrown for invalid transitions: `InvalidStatusTransitionException`

---

## 8. FALLBACK / RESILIENCE PATTERN

### Principle
Gracefully degrade when primary mechanism fails, ensuring system continues to function.

### Implementation in SmartCourier

```java
// From DeliveryService.java

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
        log.info("Publishing async status update for delivery {}", delivery.getId());
        // PRIMARY: Try async RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
    } catch (Exception e) {
        // FALLBACK: If RabbitMQ fails, use sync REST call
        log.warn("RabbitMQ failed, falling back to synchronous Feign call for delivery {}", 
                 delivery.getId());
        sendTrackingEventSync(delivery, location, message);
    }
}

public void sendTrackingEventSync(Delivery delivery, String location, String message) {
    log.info("Executing sync fallback for delivery {}", delivery.getId());
    Map<String, Object> request = new HashMap<>();
    request.put("deliveryId", delivery.getId());
    request.put("trackingNumber", delivery.getTrackingNumber());
    request.put("status", delivery.getStatus().name());
    request.put("location", location);
    request.put("description", message);
    
    // Direct REST call to Tracking Service
    trackingClient.addTrackingEvent(request);
}
```

### Flow Diagram
```
Scenario 1: RabbitMQ Working ✓
  Delivery Service
       │
       ├─ Create event
       ├─ Publish to RabbitMQ ──► Success
       └─ Return immediately

Scenario 2: RabbitMQ Down ✗
  Delivery Service
       │
       ├─ Create event
       ├─ Try RabbitMQ ──► FAILS
       ├─ Catch exception
       ├─ Call Tracking Service directly (sync)
       └─ Tracking Service persists event anyway

Result: Tracking data is ALWAYS saved
Either via fast async route OR slower sync route
```

### Other Resilience Features
1. **Retry Logic** - RabbitMQ automatically retries failed messages
2. **Dead Letter Queue** - Failed messages collected for manual recovery
3. **Health Checks** - Eureka pings services every 30s
4. **Circuit Breaker** - (Ready to implement) Stop calling failing services temporarily
5. **Timeout** - Don't wait forever for slow responses

### Benefits
✓ **Continuous Operation** - System works even if one component fails
✓ **Data Consistency** - Tracking data is persisted regardless
✓ **Better UX** - User experiences degraded service, not failure
✓ **Reliability** - No silent failures; attempts alternative paths

---

## 9. SEPARATION OF CONCERNS PRINCIPLE

### Principle
Each layer/component has single, well-defined responsibility.

### Implementation in SmartCourier

```
Delivery Service Architecture:

┌──────────────────────────────────────┐
│  DeliveryController                  │  Responsibility: Handle HTTP requests
│  • Map HTTP routes to methods        │  (REST layer)
│  • Validate request format           │
│  • Convert DTOs to entities          │
└──────────────────────────────────────┘
             ↑
             │ Uses
             ▼
┌──────────────────────────────────────┐
│  DeliveryService (Business Logic)    │  Responsibility: Core business logic
│  • Calculate charges                 │  (Service layer)
│  • Manage status transitions         │
│  • Validate business rules           │
│  • Orchestrate events                │
└──────────────────────────────────────┘
             ↑
             │ Uses
             ▼
┌──────────────────────────────────────┐
│  DeliveryRepository                  │  Responsibility: Data access
│  • Query database                    │  (Data access layer)
│  • Save/Update/Delete entities       │
│  • Manage transactions               │
└──────────────────────────────────────┘
             ↑
             │ Uses
             ▼
┌──────────────────────────────────────┐
│  MySQL Database                      │  Responsibility: Persist data
│  • Store/retrieve entities           │  (Data layer)
└──────────────────────────────────────┘
```

### Layered Architecture Benefits
```
Request Flow:
Browser
  │
  ├─ HTTP Request
  │
  ▼
Controller Layer
  │ (Validates HTTP format, routing)
  │
  ├─ Converts JSON → DTO
  │
  ▼
Service Layer
  │ (Core business logic)
  │
  ├─ Validates business rules
  ├─ Calculates charges
  ├─ Checks permissions
  ├─ Publishes events
  │
  ▼
Repository Layer
  │ (Data persistence)
  │
  ├─ Builds queries
  ├─ Manages transactions
  ├─ Maps objects to tables
  │
  ▼
Database
  │ (Persists data)
  │
  ├─ Saves delivery record
  ├─ Maintains constraints
  ├─ Ensures ACID
```

### Code Reference
```java
// Layer 1: REST Controller
@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
    @PostMapping
    public ResponseEntity<DeliveryResponseDTO> createDelivery(
        @Valid @RequestBody DeliveryRequest request,
        @RequestHeader("X-User-Username") String username,
        @RequestHeader("X-User-Role") String role) {
        
        // Calls service layer
        DeliveryResponseDTO response = deliveryService.createDelivery(request, username, role);
        return ResponseEntity.ok(response);
    }
}

// Layer 2: Business Logic
@Service
public class DeliveryService {
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        // Business logic here
        Double charge = calculateCharge(parcel);
        Delivery delivery = Delivery.builder()...build();
        
        // Calls repository layer
        Delivery saved = deliveryRepository.save(delivery);
        
        // Publish events
        publishStatusEvent(saved, null, "Initial status...");
        
        return mapToResponseDTO(saved);
    }
}

// Layer 3: Data Access
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
}
```

### Benefits
✓ **Maintainability** - Easy to find and fix issues in specific layer
✓ **Testability** - Can mock each layer independently
✓ **Reusability** - Service can be called from different controllers
✓ **Flexibility** - Can change database without touching business logic
✓ **Clear Responsibilities** - Each layer knows what it's supposed to do

---

## 10. SINGLE RESPONSIBILITY PRINCIPLE (SRP)

### Principle
Each class should have one reason to change.

### Implementation in SmartCourier

```java
// GOOD: Each class has single responsibility

// 1. DeliveryController - Responsible ONLY for HTTP handling
@RestController
public class DeliveryController {
    public ResponseEntity<DeliveryResponseDTO> createDelivery(...) {
        // Only handles HTTP request/response
        DeliveryResponseDTO response = deliveryService.createDelivery(...);
        return ResponseEntity.ok(response);
    }
}

// 2. DeliveryService - Responsible ONLY for delivery business logic
@Service
public class DeliveryService {
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, ...) {
        // Only handles business logic
        Double charge = calculateCharge(parcel);
        Delivery delivery = Delivery.builder()...build();
        publishStatusEvent(saved, null, "...");
        return mapToResponseDTO(saved);
    }
}

// 3. DeliveryRepository - Responsible ONLY for data access
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    // Only defines data access methods
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
}

// 4. TrackingClient - Responsible ONLY for calling Tracking Service
@FeignClient("tracking-service")
public interface TrackingClient {
    // Only defines inter-service communication
    @PostMapping("/tracking/event")
    void addTrackingEvent(Map<String, Object> request);
}

// BAD: Violates SRP (Don't do this!)
public class DeliveryServiceBad {
    // Mixes too many responsibilities
    
    public void createDelivery(...) {
        // Business logic mixed with HTTP handling
        // Data access mixed with business logic
        // Event publishing mixed with database operations
        // Email notifications mixed with delivery logic
        // Logging mixed everywhere
    }
}
```

### Benefits
✓ **Easier Testing** - Can test each class independently
✓ **Easier Maintenance** - Changes to business logic don't affect HTTP layer
✓ **Reusability** - Service can be reused from different controllers
✓ **Clear Intent** - Name says what class does
✓ **Lower Coupling** - Less interconnected code

---

## 11. DEPENDENCY INJECTION PRINCIPLE

### Principle
Inject dependencies rather than creating them internally, enabling loose coupling and easier testing.

### Implementation in SmartCourier

```java
// GOOD: Using Dependency Injection

@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final TrackingClient trackingClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;

    // Constructor injection - dependencies provided externally
    public DeliveryService(DeliveryRepository deliveryRepository,
                          TrackingClient trackingClient,
                          ApplicationEventPublisher eventPublisher,
                          RabbitTemplate rabbitTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.trackingClient = trackingClient;
        this.eventPublisher = eventPublisher;
        this.rabbitTemplate = rabbitTemplate;
    }

    public DeliveryResponseDTO createDelivery(...) {
        // Use injected dependencies
        Delivery saved = deliveryRepository.save(delivery);
        publishStatusEvent(saved, null, "...");
        return mapToResponseDTO(saved);
    }
}

// BAD: Hard-coded dependencies (Don't do this!)
public class DeliveryServiceBad {
    private DeliveryRepository repo = new DeliveryRepositoryImpl();  // TIGHTLY COUPLED
    private TrackingClient client = new TrackingClientImpl();        // TIGHTLY COUPLED
    
    // Hard to test (can't mock)
    // Hard to change implementation
    // Violates Open/Closed Principle
}
```

### Testing Benefit
```java
// With DI, testing is easy - mock dependencies

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    
    @Mock
    private DeliveryRepository mockRepo;
    
    @Mock
    private TrackingClient mockClient;
    
    @InjectMocks
    private DeliveryService service;
    
    @Test
    void testCreateDelivery() {
        // Mock repository behavior
        Delivery mockDelivery = new Delivery();
        when(mockRepo.save(any())).thenReturn(mockDelivery);
        
        // Call service with mocked dependencies
        DeliveryResponseDTO response = service.createDelivery(...);
        
        // Verify behavior
        verify(mockRepo).save(any());
        assertNotNull(response);
    }
}
```

### Benefits
✓ **Loose Coupling** - Service doesn't depend on specific implementation
✓ **Easy Testing** - Can inject mocks for testing
✓ **Flexibility** - Can swap implementations easily (e.g., use different DB)
✓ **Clear Dependencies** - Constructor shows what service needs
✓ **Configuration** - Spring manages lifecycle and dependencies

---

## 12. STATELESS SERVICES PRINCIPLE

### Principle
Services don't store per-request state; each request is independent and can be handled by any instance.

### Implementation in SmartCourier

```java
// GOOD: Stateless Service

@Service
public class DeliveryService {
    // NO instance variables storing request state
    
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        // All data passed as parameters
        // No state stored on service instance
        
        Double charge = calculateCharge(request.getPackageDetails());  // Computed
        Delivery delivery = Delivery.builder()                         // Built locally
            .charge(charge)
            .username(username)
            .build();
        
        Delivery saved = deliveryRepository.save(delivery);  // Persisted to DB
        return mapToResponseDTO(saved);
    }
}

// Request specific data stored only in:
// 1. Method parameters (input)
// 2. Local variables (computed)
// 3. Database (persistent)
// NOT in service instance variables


// BAD: Stateful Service (Don't do this!)
@Service
public class DeliveryServiceBad {
    private Long currentDeliveryId;        // ❌ SHARED STATE
    private String currentUsername;       // ❌ WRONG
    private DeliveryRequest currentRequest;  // ❌ CAUSES ISSUES
    
    public void createDelivery(DeliveryRequest request, String username) {
        this.currentRequest = request;     // Parallel requests interfere!
        this.currentUsername = username;   // Thread safety issues!
        // ...
        
        // If two users call this simultaneously:
        // Request 1: Sets currentUsername = "alice"
        // Request 2: Sets currentUsername = "bob"  (overwrites!)
        // Request 1: Uses currentUsername = "bob"  (WRONG!)
    }
}
```

### Benefits
✓ **Horizontal Scaling** - Any instance can handle any request
✓ **Thread Safety** - No shared state between threads
✓ **Load Balancing** - Can easily add/remove instances
✓ **Fault Tolerance** - If one instance fails, others handle requests
✓ **Predictability** - Same request always produces same result

### Architecture Impact
```
Stateless Services enable:

┌─────────────────────────────────────┐
│    API Gateway (9090)               │
│    Load Balancer                    │
└─────┬───────────────┬───────────────┘
      │               │
      ▼               ▼
┌──────────────┐  ┌──────────────┐
│ Delivery Svc │  │ Delivery Svc │
│ Instance 1   │  │ Instance 2   │
└──────────────┘  └──────────────┘

Request 1 → Instance 1
Request 2 → Instance 2
Request 3 → Instance 1 (can reuse if Instance 1 free)

No coordination needed between instances!
```

---

## 13. IDEMPOTENCY PRINCIPLE

### Principle
Same request can be safely retried multiple times with same result.

### Implementation in SmartCourier

```java
// POST endpoints create new records (idempotent if implemented with unique constraints)
@PostMapping("/deliveries")
public ResponseEntity<DeliveryResponseDTO> createDelivery(...) {
    // Generates unique tracking number
    String trackingNumber = generateTrackingNumber();  // SC + timestamp + random
    
    Delivery delivery = Delivery.builder()
        .trackingNumber(trackingNumber)  // UNIQUE - ensures idempotency
        .username(username)
        .build();
    
    Delivery saved = deliveryRepository.save(delivery);
    return ResponseEntity.ok(mapToResponseDTO(saved));
}

// PUT endpoints are naturally idempotent
@PutMapping("/deliveries/{id}/status")
public ResponseEntity<DeliveryResponseDTO> updateStatus(
    @PathVariable Long id, 
    @RequestParam String status) {
    
    Delivery delivery = deliveryRepository.findById(id).orElseThrow(...);
    delivery.setStatus(DeliveryStatus.valueOf(status));
    
    Delivery updated = deliveryRepository.save(delivery);
    return ResponseEntity.ok(mapToResponseDTO(updated));
    
    // Calling same endpoint multiple times:
    // Request 1: BOOKED → PICKED_UP ✓
    // Request 2: PICKED_UP → PICKED_UP (no change) ✓
    // Request 3: PICKED_UP → PICKED_UP (no change) ✓
    // Safe to retry!
}

// Problem to avoid:
@PostMapping("/deliveries/{id}/charge-payment")
public void chargePayment(@PathVariable Long id) {
    // ❌ NOT idempotent - calling twice charges twice!
    // Must implement idempotency key or tracking
    
    paymentService.charge(id, delivery.getCharge());
}
```

### Benefits
✓ **Network Resilience** - Can retry if request dropped
✓ **Reliability** - Transient failures don't cause duplicate operations
✓ **User Confidence** - "Click again if we're not sure it worked" safe
✓ **Distributed Systems** - Critical for async/message-driven systems

---

## 14. EVENTUAL CONSISTENCY PRINCIPLE

### Principle
In distributed systems, accept that not all data is immediately consistent; data converges to consistency over time.

### Implementation in SmartCourier

```
Scenario: Delivery status changes

Time T0:
  Delivery Service: status = BOOKED
  Tracking Service: (hasn't processed event yet)
  
  ├─ Delivery Service updates database: status = BOOKED ✓
  ├─ Publishes event: BOOKED
  └─ Returns to client

Time T0+100ms:
  ├─ Event consumed by Tracking Service
  ├─ Creates TrackingEvent in tracking_db
  ├─ History now updated ✓

State at T0: INCONSISTENT (event not processed yet)
State at T0+100ms: CONSISTENT (both services updated)

Result: EVENTUAL CONSISTENCY ✓
```

### Code Manifestation
```java
// DeliveryService publishes async, returns before Tracking Service consumes

public DeliveryResponseDTO updateStatus(Long id, String status, ...) {
    Delivery delivery = deliveryRepository.findById(id).orElseThrow(...);
    delivery.setStatus(DeliveryStatus.valueOf(status));
    
    Delivery saved = deliveryRepository.save(delivery);  // Saved immediately
    
    // But event processing is async!
    publishStatusEvent(saved, null, reason);  // Publishes to RabbitMQ
    // Returns BEFORE Tracking Service processes
    
    return mapToResponseDTO(saved);  // Returns now
}

// Tracking Service processes event later
@RabbitListener(queues = "tracking-service-events")
public void onDeliveryStatusChanged(DeliveryEvent event) {
    // Called asynchronously, AFTER response sent to client
    
    // Create history entry
    TrackingEvent trackingEvent = TrackingEvent.builder()
        .deliveryId(event.getDeliveryId())
        .status(event.getStatus())
        .timestamp(event.getTimestamp())
        .build();
    
    trackingRepository.save(trackingEvent);  // Saved to DB
}
```

### Usage
```
Acceptable for: Tracking, analytics, notifications
NOT acceptable for: Payment processing, inventory deduction

Tradeoff:
- Benefit: High performance (don't wait for all services)
- Cost: Brief inconsistency (1-2 second delay seeing full picture)
```

### Benefits
✓ **Performance** - Services can return immediately
✓ **Scalability** - Don't block on slow operations
✓ **Resilience** - If Tracking Service slow, Delivery Service unaffected
✓ **Throughput** - Process more requests per second

---

## 15. SAGA PATTERN (Implicit)

### Principle
For distributed transactions, coordinate a series of steps across services; compensate if a step fails.

### Implementation in SmartCourier (Simplified)

```
Delivery Creation Saga:

Step 1: Create Delivery (Delivery Service)
  ├─→ Create delivery record
  └─ Delivered: DRAFT status

Step 2: Validate Sender Address (Delivery Service)
  ├─→ Ensure sender info complete
  └─ Delivered: Ready for next step

Step 3: Add Receiver Address (Delivery Service)
  ├─→ Ensure receiver info complete
  └─ Delivered: Ready for setup

Step 4: Add Package Details (Delivery Service)
  ├─→ Calculate charge
  └─ Delivered: Charge calculated

Step 5: Finalize (Delivery Service → Tracking Service)
  ├─→ Change status DRAFT → BOOKED
  ├─→ Publish event to RabbitMQ
  └─ Delivered: Tracking history created

Optional Compensation:
  If Step N fails:
  ├─ Step 5 fails: Delivery remains DRAFT (customer can retry)
  ├ Tracking event not created (RabbitMQ will retry)
  └─ System self-heals through fallback mechanisms
```

### Code Reference
```java
@Transactional
public DeliveryResponseDTO finalizeDelivery(Long id, String username) {
    // Step 1-3: Validate all required data present
    Delivery delivery = getAndValidateDraft(id, username);
    if (delivery.getSenderAddress() == null || 
        delivery.getReceiverAddress() == null ||
        delivery.getParcelPackage() == null) {
        throw new BusinessRuleViolationException(
            "Cannot finalize delivery. All steps must be completed.");
    }

    // Step 4: State transition
    DeliveryStatus oldStatus = delivery.getStatus();
    delivery.setStatus(DeliveryStatus.BOOKED);
    Delivery saved = deliveryRepository.save(delivery);

    // Step 5: Publish event to Tracking Service
    publishStatusEvent(saved, null, "Delivery confirmed via wizard");
    
    // Step 6: Publish domain event for notifications
    eventPublisher.publishEvent(new DeliveryStatusChangedEvent(
        this, saved.getId(), saved.getTrackingNumber(), 
        oldStatus, DeliveryStatus.BOOKED, username));

    return mapToResponseDTO(saved);
}
```

### Compensation Strategy
```
If Delivery Service fails at step 5:
  ├─ Transaction rolls back (no BOOKED state saved)
  ├─ Delivery remains DRAFT
  ├─ Customer can retry again (wizard reopens)
  └─ By retry, all data re-entered and process completes

If Tracking Service fails to consume event:
  ├─ Event remains in RabbitMQ queue
  ├─ RabbitMQ retries automatically
  ├─ Or falls back to sync call
  └─ History eventually created
```

### Benefits
✓ **Distributed Transactions** - Can coordinate across services
✓ **Failure Handling** - Clear compensation logic
✓ **Error Recovery** - Explicit rollback/retry strategy
✓ **Business Logic** - May represent actual business process

---

## 16. DISTRIBUTED TRACING PRINCIPLE

### Principle
Track requests across all services to understand flow, performance, and failures.

### Implementation in SmartCourier

```
With Zipkin:

Request: GET /gateway/deliveries/123

Spans Created:
  ├─ api-gateway span
  │  └─ JWT validation (2ms)
  │  └─ Service lookup in Eureka (0.5ms)
  │  └─ Forward to delivery-service (rest is nested span)
  │
  ├─ delivery-service span
  │  └─ DeliveryController.getDeliveryById (1ms)
  │  └─ Database query (15ms)
  │  └─ JSON serialization (1ms)
  │
  └─ Total latency: ~50ms
```

### Code Integration
```java
// Spring automatically creates spans for:
// - HTTP requests (@RestController)
// - Database calls (JPA)
// - Inter-service calls (Feign)
// - Message publishing (RabbitMQ)

// Custom tracing for business logic
private static final Tracer tracer = ...;

public DeliveryResponseDTO createDelivery(...) {
    try (Scope scope = tracer.buildSpan("createDelivery")
            .withTag("username", username)
            .withTag("role", role)
            .startActive(true)) {
        
        // Business logic
        Double charge = calculateCharge(parcel);
        // ... more logic ...
        
    } catch (Exception e) {
        // Errors automatically tagged in span
        tracer.activeSpan().setTag("error", true);
        throw e;
    }
}
```

### Observable Metrics
```
Zipkin Dashboard shows:
├─ Service name
├─ Operation name
├─ Duration
├─ Timestamp
├─ Dependencies
├─ Errors (if any)
├─ Tags (custom data)
└─ Logs (messages)
```

### Benefits
✓ **Debugging** - See exactly where request failed
✓ **Performance Tuning** - Identify bottlenecks
✓ **Dependency Analysis** - Understand service interactions
✓ **Production Monitoring** - Real-time visibility

---

## 17. CONTINUOUS INTEGRATION & CODE QUALITY

### Principle
Automate testing, quality checks, and deployment to maintain code reliability.

### Implementation in SmartCourier

```
┌────────────────────────────────────────┐
│        Code Commit to Git              │
└────────────┬─────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│  Run Unit Tests (JUnit + Mockito)      │
│  ✓ All services tested                 │
│  ✓ Coverage tracked by JaCoCo          │
└────────────┬─────────────────────────┘
             │ if fail → stop
             ▼
┌────────────────────────────────────────┐
│  Run Integration Tests                 │
│  ✓ TestContainers spin up DB           │
│  ✓ Test service with real dependencies │
└────────────┬─────────────────────────┘
             │ if fail → stop
             ▼
┌────────────────────────────────────────┐
│  Code Quality Analysis (SonarQube)     │
│  ✓ Scan for vulnerabilities            │
│  ✓ Check code coverage > 70%           │
│  ✓ Analyze code smells                 │
└────────────┬─────────────────────────┘
             │ if fail → stop
             ▼
┌────────────────────────────────────────┐
│  Build Docker Images                   │
│  ✓ Create container for each service   │
└────────────┬─────────────────────────┘
             │
             ▼
┌────────────────────────────────────────┐
│  Deploy to Environment                 │
│  ✓ Dev → Staging → Production          │
└────────────────────────────────────────┘

Metrics:
• Code Coverage: JaCoCo tracks %
• Quality Gates: SonarQube enforces rules
• Test Results: All tests must pass
• Security: Vulnerability scanning
```

### Code Reference
```xml
<!-- pom.xml - Quality tools configured -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <!-- Measures coverage on every build -->
</plugin>

<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <!-- Scans code quality -->
</plugin>
```

### Benefits
✓ **Quality Assurance** - Issues caught automatically
✓ **Risk Reduction** - Tests prevent regressions
✓ **Documentation** - Tests serve as API documentation
✓ **Confidence** - Deploy with confidence

---

## Summary: Design Principles Used

| Principle | Where Used | Benefit |
|-----------|-----------|---------|
| **Microservices** | 7 independent services | Scalability, independence |
| **Event-Driven** | RabbitMQ async communication | Loose coupling, throughput |
| **API Gateway** | Port 9090 routing | Single entry point, security |
| **Service Discovery** | Eureka registry | Dynamic discovery, health checks |
| **Configuration Mgmt** | Config Server | Centralized config, hot reload |
| **Database-per-Service** | 4 isolated databases | Independence, schema control |
| **State Machine** | Delivery status transitions | Prevent invalid states |
| **Resilience** | RabbitMQ fallback | Graceful degradation |
| **Separation of Concern** | Layered architecture | Maintainability, testability |
| **SRP** | Each class one responsibility | Clarity, reusability |
| **Dependency Injection** | Constructor injection | Loose coupling, testability |
| **Stateless Services** | No instance state | Horizontal scaling |
| **Idempotency** | Unique constraints | Safe retries |
| **Eventual Consistency** | Async event propagation | Performance, scalability |
| **Saga Pattern** | Multi-step workflows | Distributed transactions |
| **Distributed Tracing** | Zipkin integration | Debugging, performance |
| **CI/CD** | Automated testing & QA | Quality, reliability |

---

## Interview Talking Points

**"In SmartCourier, we applied multiple design principles:"**

1. **Microservices Pattern** - 7 independent services allow teams to work in parallel and scale specific components
2. **Event-Driven Architecture** - RabbitMQ decouples services; one service publishes, others subscribe
3. **API Gateway** - Single entry point handles security, routing, and cross-cutting concerns
4. **State Machine** - Explicit delivery status transitions prevent invalid states
5. **Resilience** - Fallback mechanisms ensure system functions even if components fail
6. **Database-per-Service** - Each service owns its schema, enabling independent evolution
7. **Distributed Tracing** - Zipkin traces requests across all services for observability
8. **Idempotency** - Safe to retry requests without duplicate operations
9. **Eventual Consistency** - Accept brief inconsistency for better performance

**"These principles make the system: scalable, resilient, maintainable, and observable"**


