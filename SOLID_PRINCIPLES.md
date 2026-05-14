# SOLID Principles in SmartCourier Project

## Overview
SmartCourier implements all **5 SOLID principles** in its architecture and code design. SOLID is a mnemonic for five design principles that make code more maintainable, flexible, and scalable.

---

## 1. SINGLE RESPONSIBILITY PRINCIPLE (SRP)

### Definition
A class should have **only one reason to change** - it should have only one responsibility.

### Implementation in SmartCourier

#### ✅ GOOD: Separated Responsibilities

```java
// 1. DeliveryController - ONLY handles HTTP requests/responses
@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryResponseDTO> createDelivery(
            @Valid @RequestBody DeliveryRequest request,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        
        // Only responsibility: Handle HTTP request and return response
        DeliveryResponseDTO response = deliveryService.createDelivery(request, username, role);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryById(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id));
    }
}

// Reason to change: Only if HTTP contract changes


// 2. DeliveryService - ONLY handles business logic
@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final TrackingClient trackingClient;
    private final RabbitTemplate rabbitTemplate;

    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        // Business logic responsibilities:
        // - Validate user role
        // - Calculate charges
        // - Enforce business rules
        // - Coordinate with other services

        if ("CUSTOMER".equalsIgnoreCase(role) &&
            (initialStatus != DeliveryStatus.DRAFT && initialStatus != DeliveryStatus.BOOKED)) {
            throw new UnauthorizedAccessException("Customers can only create DRAFT or BOOKED");
        }

        Double charge = calculateCharge(parcel);  // Business calculation
        
        Delivery delivery = Delivery.builder()
                .trackingNumber(generateTrackingNumber())
                .username(username)
                .charge(charge)
                .status(initialStatus)
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);
        publishStatusEvent(savedDelivery, null, "Initial status: " + initialStatus);
        
        return mapToResponseDTO(savedDelivery);
    }

    // Reason to change: Only if business logic changes
}

// 3. DeliveryRepository - ONLY handles data persistence
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByStatus(DeliveryStatus status);
}

// Reason to change: Only if data access strategy changes


// 4. TrackingClient - ONLY handles inter-service communication
@FeignClient(name = "tracking-service")
public interface TrackingClient {
    @PostMapping("/tracking/event")
    void addTrackingEvent(Map<String, Object> request);
}

// Reason to change: Only if Tracking Service API changes


// 5. RabbitMQConfig - ONLY handles RabbitMQ configuration
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "delivery.status";
    public static final String ROUTING_KEY = "delivery.status";

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue trackingServiceQueue() {
        return new Queue("tracking-service-events", true);
    }

    @Bean
    public Binding bindingTrackingQueue(Queue trackingServiceQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(trackingServiceQueue)
                .to(deliveryExchange)
                .with(ROUTING_KEY);
    }
}

// Reason to change: Only if RabbitMQ configuration changes
```

#### ❌ BAD: Multiple Responsibilities (Violates SRP)

```java
// ❌ DON'T DO THIS - Violates SRP
@RestController
@Service  // Already problematic - mixing controller and service
public class DeliveryBadService {
    
    @PostMapping("/deliveries")
    public ResponseEntity<DeliveryResponseDTO> createDelivery(...) {
        // Handles HTTP concerns
        // Validates request format
        HttpServletRequest request = ...; // HTTP-specific code in service
        
        // Business logic mixed in
        Double charge = calculateCharge(parcel);
        
        // Data persistence mixed in
        List<Delivery> deliveries = em.createQuery("SELECT d FROM Delivery d").getResultList();
        
        // Event publishing mixed in
        rabbitTemplate.convertAndSend(...);
        
        // Email notification mixed in
        emailService.send(...);
        
        // Logging mixed everywhere
        log.debug("Creating delivery");
        
        return ResponseEntity.ok(...);
        
        // Reasons to change: HTTP changes, business logic changes, DB changes, 
        // message format changes, email format changes, etc.
        // TOO MANY REASONS = VIOLATION OF SRP!
    }
}
```

### Benefits of SRP in SmartCourier

| Class | Single Responsibility | Easy to Test | Easy to Change |
|-------|----------------------|--------------|----------------|
| DeliveryController | HTTP handling | ✓ Mock service | ✓ Only if routes change |
| DeliveryService | Business logic | ✓ Mock repository | ✓ Only if rules change |
| DeliveryRepository | Data access | ✓ Mock DB | ✓ Only if DB changes |
| TrackingClient | Service calls | ✓ Mock HTTP | ✓ Only if API changes |

---

## 2. OPEN/CLOSED PRINCIPLE (OCP)

### Definition
**Open for extension, closed for modification** - Software entities should be easily extensible without modifying existing code.

### Implementation in SmartCourier

#### ✅ GOOD: Status Transitions Using Strategy Pattern

```java
// Instead of checking all statuses with if-else scattered everywhere,
// use a map that's OPEN for extension

@Service
public class DeliveryService {
    private static final Map<DeliveryStatus, List<DeliveryStatus>> ALLOWED_TRANSITIONS = 
        Map.of(
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
                List.of(),
            DeliveryStatus.DELAYED,
                List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.FAILED),
            DeliveryStatus.FAILED, 
                List.of(DeliveryStatus.RETURNED, DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY),
            DeliveryStatus.RETURNED, 
                List.of()
        );

    private boolean canTransition(String role, DeliveryStatus from, DeliveryStatus to,
                                 Delivery delivery, String username) {
        // This logic is CLOSED for modification - doesn't change
        // But transitions are OPEN for extension - just add to map
        
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            if (!delivery.getUsername().equals(username)) {
                throw new UnauthorizedAccessException("Access denied");
            }
            return from == DeliveryStatus.DRAFT && to == DeliveryStatus.BOOKED;
        }

        if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
            return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
        }

        return false;
    }

    public DeliveryResponseDTO updateStatus(Long id, String status, String role, 
                                           String username, String reason) {
        // ... validation ...
        
        if (!canTransition(role, currentStatus, targetStatus, delivery, username)) {
            throw new InvalidStatusTransitionException(
                "Invalid state transition from " + currentStatus + " to " + targetStatus);
        }
        
        delivery.setStatus(targetStatus);
        Delivery savedDelivery = deliveryRepository.save(delivery);
        publishStatusEvent(savedDelivery, null, reason);
        
        return mapToResponseDTO(savedDelivery);
    }
}

// To add new transition: just update the map - NO CODE MODIFICATION needed
// Example: Want to add DELAYED → DELIVERED transition?
// Just modify the map:
// DeliveryStatus.DELAYED, 
//     List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY, 
//             DeliveryStatus.FAILED, DeliveryStatus.DELIVERED)  // NEW
```

#### Charge Calculation - Open for Extension

```java
@Service
public class DeliveryService {
    
    private Double calculateCharge(ParcelPackage parcel) {
        // Calculation is CLOSED for modification (core logic)
        // But extensible - can add new service types without changing logic
        
        double base;
        switch (parcel.getServiceType()) {
            case EXPRESS:
                base = 14.99;
                break;
            case INTERNATIONAL:
                base = 29.99;
                break;
            default:
                base = 5.99;  // DOMESTIC
                break;
        }
        
        double weightCharge = parcel.getWeight() * 0.5;
        double fragileCharge = Boolean.TRUE.equals(parcel.getFragile()) ? 3.0 : 0;
        
        return Math.round((base + weightCharge + fragileCharge) * 100.0) / 100.0;
    }
}

// To add new service type (SAME_DAY with base 49.99):
// 1. Add to ServiceType enum
// 2. Add case in switch statement
// CLOSED for modification in calculation logic, but OPEN for new service types
```

#### ❌ BAD: Violates OCP

```java
// ❌ DON'T DO THIS - Violates OCP (must modify method for each new type)
public double calculateChargeOld(ParcelPackage parcel) {
    if (parcel.getServiceType().equals("DOMESTIC")) {
        return 5.99 + parcel.getWeight() * 0.5 + (parcel.getFragile() ? 3.0 : 0);
    } else if (parcel.getServiceType().equals("EXPRESS")) {
        return 14.99 + parcel.getWeight() * 0.5 + (parcel.getFragile() ? 3.0 : 0);
    } else if (parcel.getServiceType().equals("INTERNATIONAL")) {
        return 29.99 + parcel.getWeight() * 0.5 + (parcel.getFragile() ? 3.0 : 0);
    }
    // Adding new service type requires modifying this method!
}
```

### Benefits in SmartCourier

- **Adding new status transitions** - Just update map, don't modify validation logic
- **Adding new service types** - Just add enum value, don't modify charge calculation structure
- **Adding new roles** - Just add case in permission logic, don't change core method
- **Easy feature extensions** - Can add features without touching existing code

---

## 3. LISKOV SUBSTITUTION PRINCIPLE (LSP)

### Definition
Derived classes must be substitutable for their base classes without breaking functionality.

### Implementation in SmartCourier

#### ✅ GOOD: Repository Interface Pattern

```java
// Base interface - contract that all implementations must follow
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByStatus(DeliveryStatus status);
}

// Spring Data JPA provides implementation
// DeliveryService uses the interface, not the implementation
@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;  // Interface type
    
    // Can inject ANY implementation of DeliveryRepository
    // - Spring Data JPA implementation
    // - Mock implementation for testing
    // - Alternative database implementation
    
    public DeliveryResponseDTO getDeliveryById(Long id) {
        return deliveryRepository.findById(id)  // Works with ANY implementation
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
    }
}

// Test code - substitute mock implementation
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    @Mock
    private DeliveryRepository mockRepository;  // Mock is substitutable
    
    @InjectMocks
    private DeliveryService service;
    
    @Test
    void testGetDeliveryById() {
        Delivery mockDelivery = new Delivery();
        when(mockRepository.findById(123L)).thenReturn(Optional.of(mockDelivery));
        
        // Service works the same with mock as with real repository
        DeliveryResponseDTO response = service.getDeliveryById(123L);
        
        assertNotNull(response);
        verify(mockRepository).findById(123L);
    }
}

// LSP Contract: Any DeliveryRepository implementation must:
// - Return List<Delivery> from findByUsername (cannot return null or wrong type)
// - Return Optional<Delivery> from findById (cannot return single object)
// - Return List<Delivery> from findByStatus (must be consistent type)
```

#### Feign Client Substitution

```java
// Interface for calling Tracking Service
@FeignClient(name = "tracking-service")
public interface TrackingClient {
    @PostMapping("/tracking/event")
    void addTrackingEvent(Map<String, Object> request);
}

// In production: Real Feign implementation calls actual HTTP
// In testing: Can substitute mock implementation
@Service
public class DeliveryService {
    private final TrackingClient trackingClient;  // Interface dependency
    
    public void sendTrackingEventSync(Delivery delivery, String location, String message) {
        Map<String, Object> request = new HashMap<>();
        request.put("deliveryId", delivery.getId());
        request.put("trackingNumber", delivery.getTrackingNumber());
        request.put("status", delivery.getStatus().name());
        request.put("location", location);
        request.put("description", message);
        
        // Works with real or mock TrackingClient
        trackingClient.addTrackingEvent(request);
    }
}

// Test: Mock implementation works identically
@Test
void testFallbackTracking() {
    TrackingClient mockClient = mock(TrackingClient.class);
    
    delivery.setStatus(DeliveryStatus.PICKED_UP);
    service.sendTrackingEventSync(delivery, "Hub A", "Picked up");
    
    verify(mockClient).addTrackingEvent(any());
}
```

#### ❌ BAD: Violates LSP

```java
// ❌ DON'T DO THIS - Violates LSP

@Component
public class BrokenDeliveryRepository {
    
    // Violates contract: returns null sometimes (breaks LSP)
    public Optional<Delivery> findById(Long id) {
        if (id == null) {
            return null;  // ❌ Should return Optional.empty(), not null
        }
        // ...
    }
    
    // Violates contract: has side effects (breaks LSP)
    public List<Delivery> findByStatus(DeliveryStatus status) {
        List<Delivery> results = ...;
        // Unexpected side effect in what should be pure query
        emailService.sendNotification("Query executed");  // ❌ Side effect
        return results;
    }
    
    // Violates contract: returns wrong type (breaks LSP)
    public List<Delivery> findByUsername(String username) {
        // Client expects List<Delivery>, but...
        return null;  // ❌ Returns null, not empty list
    }
}

// Service would break with this implementation
DeliveryService service = new DeliveryService(brokenRepository);
service.getDeliveryById(456L);  // Throws NullPointerException
```

### Benefits in SmartCourier

- **Testability** - Can substitute mocks for real implementations
- **Flexibility** - Can swap implementations without changing calling code
- **Contract adherence** - Implementations must follow interface contract
- **Confidence** - Any implementation follows same rules

---

## 4. INTERFACE SEGREGATION PRINCIPLE (ISP)

### Definition
Clients should not be forced to depend on interfaces they don't use. Create focused, specific interfaces.

### Implementation in SmartCourier

#### ✅ GOOD: Segregated, Focused Interfaces

```java
// Small, focused interface - clients depend ONLY on what they need
@FeignClient(name = "tracking-service")
public interface TrackingClient {
    // Only ONE responsibility: add tracking event
    @PostMapping("/tracking/event")
    void addTrackingEvent(Map<String, Object> request);
}

// If we used a fat interface (ISP violation):
// ❌ public interface TrackingService {
// ❌     void addTrackingEvent(...);
// ❌     DeliveryProof getProof(...);
// ❌     List<TrackingEvent> getHistory(...);
// ❌     void updateProof(...);
// ❌ }
// Delivery Service would be forced to implement all methods!

@Service
public class DeliveryService {
    private final TrackingClient trackingClient;  // Only depends on ONE method
    
    // Only uses addTrackingEvent method
    public void sendTrackingEventSync(Delivery delivery, String location, String message) {
        Map<String, Object> request = new HashMap<>();
        request.put("deliveryId", delivery.getId());
        request.put("trackingNumber", delivery.getTrackingNumber());
        request.put("status", delivery.getStatus().name());
        request.put("location", location);
        request.put("description", message);
        
        trackingClient.addTrackingEvent(request);  // ← Uses ONLY this method
    }
}
```

#### Repository Segregation

```java
// Small, focused repository interface for Delivery data
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByStatus(DeliveryStatus status);
}

// Separate, focused repository for Tracking data
@Repository
public interface TrackingRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByDeliveryIdOrderByTimestampDesc(Long deliveryId);
    List<TrackingEvent> findByTrackingNumber(String trackingNumber);
}

// Delivery Service depends ONLY on DeliveryRepository
@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;  // Focused interface
    
    // Doesn't depend on TrackingRepository - uses TrackingClient instead
}

// Tracking Service depends ONLY on TrackingRepository
@Service
public class TrackingService {
    private final TrackingRepository trackingRepository;  // Focused interface
}
```

#### Event Interface Segregation

```java
// Focused event interfaces for different purposes

// Event for publishing (only Delivery Service needs to publish)
@FunctionalInterface
public interface DeliveryEventPublisher {
    void publishDeliveryStatusChanged(DeliveryEvent event);
}

// Event for consuming (only Tracking Service needs to consume)
@Component
public interface DeliveryEventListener {
    void onDeliveryStatusChanged(DeliveryEvent event);
}

// Implementation segregates responsibilities
@Service
public class DeliveryService implements DeliveryEventPublisher {
    public void publishDeliveryStatusChanged(DeliveryEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
    }
}

@Service
public class TrackingService implements DeliveryEventListener {
    @RabbitListener(queues = "tracking-service-events")
    public void onDeliveryStatusChanged(DeliveryEvent event) {
        TrackingEvent trackingEvent = TrackingEvent.builder()
                .deliveryId(event.getDeliveryId())
                .status(event.getStatus())
                .timestamp(event.getTimestamp())
                .build();
        trackingRepository.save(trackingEvent);
    }
}
```

#### ❌ BAD: Fat Interface Violates ISP

```java
// ❌ DON'T DO THIS - SAT (Segregated Adapter Toilet) Principle violation

@FeignClient(name = "delivery-service")
public interface FatDeliveryClient {
    // Delivery Service client would be forced to implement ALL of these
    // even if it only needs one method!
    
    @GetMapping("/deliveries/{id}")
    Delivery getDelivery(@PathVariable Long id);
    
    @PostMapping("/deliveries")
    Delivery createDelivery(@RequestBody DeliveryRequest request);
    
    @PutMapping("/deliveries/{id}/status")
    void updateStatus(@PathVariable Long id, @RequestParam String status);
    
    @DeleteMapping("/deliveries/{id}")
    void deleteDelivery(@PathVariable Long id);
    
    @GetMapping("/deliveries/{id}/history")
    List<TrackingEvent> getHistory(@PathVariable Long id);
    
    @GetMapping("/deliveries/reports")
    ReportDTO generateReport();
    
    @PostMapping("/deliveries/bulk")
    List<Delivery> createBulk(@RequestBody List<DeliveryRequest> requests);
}

// A client that only needs to add tracking event is forced to depend on all these!
```

### Benefits in SmartCourier

- **Focused dependencies** - Services depend only on what they need
- **Easy mocking** - Mock only the methods you use
- **Clear contracts** - Interface shows exactly what client does
- **Reduced coupling** - Changes to unused methods don't affect you
- **Single responsibility** - Each interface has one purpose

---

## 5. DEPENDENCY INVERSION PRINCIPLE (DIP)

### Definition
**Depend on abstractions, not concretions.** High-level modules should not depend on low-level modules; both should depend on abstractions.

### Implementation in SmartCourier

#### ✅ GOOD: Depend on Abstractions

```java
// High-level business logic
@Service
public class DeliveryService {
    // Depend on ABSTRACTIONS (interfaces), not concrete classes
    
    private final DeliveryRepository deliveryRepository;              // Interface
    private final TrackingClient trackingClient;                      // Interface
    private final ApplicationEventPublisher eventPublisher;           // Interface
    private final RabbitTemplate rabbitTemplate;                      // Interface

    // Constructor injection - dependencies provided externally
    public DeliveryService(DeliveryRepository deliveryRepository,
                          TrackingClient trackingClient,
                          ApplicationEventPublisher eventPublisher,
                          RabbitTemplate rabbitTemplate) {
        this.deliveryRepository = deliveryRepository;      // Abstraction
        this.trackingClient = trackingClient;              // Abstraction
        this.eventPublisher = eventPublisher;              // Abstraction
        this.rabbitTemplate = rabbitTemplate;              // Abstraction
    }

    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        // Uses abstractions, not concrete implementations
        
        // Repository abstraction (implementation injected)
        Delivery savedDelivery = deliveryRepository.save(delivery);
        
        // Event publisher abstraction
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(...));
        
        // RabbitTemplate abstraction
        rabbitTemplate.convertAndSend(...);
        
        // Tracking client abstraction
        trackingClient.addTrackingEvent(...);
        
        return mapToResponseDTO(savedDelivery);
    }
}

// Low-level modules (Spring provides implementations):
// - DeliveryRepository → Spring Data JPA implementation
// - TrackingClient → Feign client implementation
// - ApplicationEventPublisher → Spring framework implementation
// - RabbitTemplate → Spring AMQP implementation

// HIGH-LEVEL (DeliveryService) depends on ABSTRACTIONS
// LOW-LEVEL (concrete Spring implementations) depend on ABSTRACTIONS
// Both depend on abstractions ✓ (DIP satisfied)
```

#### Layer Dependencies Using DIP

```java
// Abstraction layer (interfaces)
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
}

public interface TrackingClient {
    void addTrackingEvent(Map<String, Object> request);
}

// Business layer (depends on abstractions)
@Service
public class DeliveryService {
    private final DeliveryRepository repository;  // Dependency on abstraction
    private final TrackingClient client;          // Dependency on abstraction
}

// Persistence layer (implements abstraction)
@Repository
public class DeliveryRepositoryImpl implements DeliveryRepository {
    // Spring Data JPA provides implementation
}

// Configuration layer (wires everything)
@Configuration
public class BeanConfig {
    @Bean
    public DeliveryService deliveryService(
            DeliveryRepository repository,      // Inject abstraction
            TrackingClient client) {            // Inject abstraction
        return new DeliveryService(repository, client);
    }
}

// Dependency flow:
// DeliveryService (high-level)
//       ↓
//    depends on
//       ↓
// DeliveryRepository (abstraction)
//       ↓
//    implemented by
//       ↓
// DeliveryRepositoryImpl (low-level)

// ✓ Both depend on abstraction (DIP satisfied)
```

#### Testing Benefits of DIP

```java
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    
    @Mock
    private DeliveryRepository mockRepository;      // Mock abstraction
    
    @Mock
    private TrackingClient mockClient;              // Mock abstraction
    
    @InjectMocks
    private DeliveryService service;
    
    @Test
    void testCreateDeliverySuccess() {
        // Arrange: Service depends on abstractions, so we can substitute mocks
        Delivery mockDelivery = new Delivery();
        when(mockRepository.save(any())).thenReturn(mockDelivery);
        
        // Act: Call service with mocked dependencies
        DeliveryResponseDTO response = service.createDelivery(
            new DeliveryRequest(), "john", "CUSTOMER");
        
        // Assert: Verify behavior without real database or HTTP calls
        verify(mockRepository).save(any());
        assertNotNull(response);
    }

    @Test
    void testFallbackWhenTrackingClientFails() {
        // Arrange: Mock client to throw exception
        Delivery savedDelivery = new Delivery();
        when(mockRepository.save(any())).thenReturn(savedDelivery);
        doThrow(new RuntimeException("Service down"))
            .when(mockClient).addTrackingEvent(any());
        
        // Act: Service should handle gracefully (depends on abstraction)
        DeliveryResponseDTO response = service.createDelivery(
            new DeliveryRequest(), "john", "CUSTOMER");
        
        // Assert: Service continues despite client failure
        assertNotNull(response);
    }
}
```

#### ❌ BAD: Concrete Dependencies Violate DIP

```java
// ❌ DON'T DO THIS - Violates DIP (depends on concrete classes)

@Service
public class DeliveryServiceBad {
    
    // Direct dependency on concrete implementations (WRONG!)
    private DeliveryRepositoryImpl repository = new DeliveryRepositoryImpl();
    private TrackingClientImpl client = new TrackingClientImpl();
    private MySQLConnection dbConnection = new MySQLConnection();
    private RabbitMQBroker broker = new RabbitMQBroker();
    
    public void createDelivery(DeliveryRequest request, String username, String role) {
        // Tightly coupled to concrete implementations
        Delivery delivery = new Delivery();
        
        // Can't mock - hardcoded dependency!
        repository.save(delivery);  // Must use real repository
        
        // Can't substitute implementation
        client.addTrackingEvent(request);  // Must use real HTTP client
        
        // Testing impossible - real database and HTTP calls required
        broker.publish(...);  // Must use real RabbitMQ
    }
    
    // Problems:
    // 1. Can't test - must use real database/HTTP/messaging
    // 2. Can't mock - dependencies hardcoded
    // 3. Can't substitute - always uses same implementations
    // 4. Changes in low-level affect high-level (bad coupling)
}
```

### Architectural Impact of DIP

```
GOOD Architecture (DIP Satisfied)
┌─────────────────────────────────────┐
│  High-Level (Business Logic)        │
│  DeliveryService                    │
└────────────┬────────────────────────┘
             │
             ├─ depends on
             │
             ▼
┌─────────────────────────────────────┐
│  Abstractions (Interfaces)          │
│  • DeliveryRepository               │
│  • TrackingClient                   │
│  • ApplicationEventPublisher        │
└──┬──────────────────────────────┬───┘
   │                              │
   │ implemented by               │ implemented by
   │                              │
   ▼                              ▼
Low-Level 1                    Low-Level 2
Spring Data JPA                Feign Client

Result: Both depend on abstractions ✓


BAD Architecture (Violates DIP)
┌─────────────────────────────────────┐
│  High-Level (Business Logic)        │
│  DeliveryService                    │
└────────────┬────────────────────────┘
             │
             ├─ hardcoded dependency on
             │
             ▼
┌─────────────────────────────────────┐
│  Low-Level Implementations          │
│  DeliveryRepositoryImpl              │
│  TrackingClientImpl                  │
│  MySQLConnection                    │
└─────────────────────────────────────┘

Result: High-level depends on low-level ✗ (tight coupling)
```

### Benefits in SmartCourier

- **Flexibility** - Can change implementations without affecting business logic
- **Testability** - Can inject mocks for any dependency
- **Reusability** - Service can work with different implementations
- **Scalability** - Can add new implementations without changing service code
- **Loose coupling** - High-level changes don't ripple through system

---

## SOLID Principles Summary Table

| Principle | What It Means | How SmartCourier Uses It |
|-----------|--------------|------------------------|
| **S**RP | One responsibility per class | Controller handles HTTP, Service handles logic, Repo handles data |
| **O**CP | Open for extension, closed for modification | Status transition map, charge calculation strategy |
| **L**SP | Substitutable implementations | Mock repositories and clients for testing |
| **I**SP | Small, focused interfaces | Separate repositories, focused Feign clients |
| **D**IP | Depend on abstractions | Constructor injection of interfaces, Spring-managed dependencies |

---

## Real-World SOLID Application in DeliveryService

### The FileFlow demonstrates ALL SOLID principles:

```java
@Service
@Transactional
public class DeliveryService {
    
    // DIP: Depend on abstractions
    private final DeliveryRepository deliveryRepository;          // Interface
    private final TrackingClient trackingClient;                  // Interface
    private final ApplicationEventPublisher eventPublisher;       // Interface
    private final RabbitTemplate rabbitTemplate;                  // Interface
    
    // SRP: Only one responsibility - delivery business logic
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        
        // SRP: Validation (one responsibility)
        if ("CUSTOMER".equalsIgnoreCase(role) &&
            !isValidCustomerStatus(initialStatus)) {
            throw new UnauthorizedAccessException("...");
        }

        // SRP: Charge calculation (one responsibility)
        Double charge = calculateCharge(parcel);
        
        // SRP: Entity creation (one responsibility)
        Delivery delivery = Delivery.builder()
                .trackingNumber(generateTrackingNumber())
                .username(username)
                .charge(charge)
                .status(initialStatus)
                .build();

        // DIP: Use abstraction (not concrete class)
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // ISP + DIP: Use focused interface
        publishStatusEvent(savedDelivery, null, "Initial status");

        // LSP: mapToResponseDTO works with any Delivery object
        return mapToResponseDTO(savedDelivery);
    }

    // OCP: Transitions are data-driven (can add without code changes)
    @Transactional
    public DeliveryResponseDTO updateStatus(Long id, String status, String role, 
                                           String username, String reason) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("..."));

        DeliveryStatus currentStatus = delivery.getStatus();
        DeliveryStatus targetStatus = DeliveryStatus.valueOf(status.toUpperCase());

        // OCP: Check transitions from map (closed for mods, open for ext)
        if (!canTransition(role, currentStatus, targetStatus, delivery, username)) {
            throw new InvalidStatusTransitionException("...");
        }

        delivery.setStatus(targetStatus);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // DIP: Publish to abstractions
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(...));

        // ISP: Use focused interface
        publishStatusEvent(savedDelivery, null, reason);

        return mapToResponseDTO(savedDelivery);
    }

    // SRP: Only one responsibility - publish events
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
            // DIP: Use abstraction (RabbitTemplate)
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
        } catch (Exception e) {
            // Fallback: DIP still applies
            sendTrackingEventSync(delivery, location, message);
        }
    }
}
```

---

## Interview Talking Points about SOLID

### Statement 1: Single Responsibility
*"In our DeliveryService, we follow SRP strictly. The controller handles HTTP concerns, the service handles business logic, and the repository handles data access. Each class has only one reason to change."*

### Statement 2: Open/Closed
*"Our status transitions are OCP-compliant. We store valid transitions in a map instead of scattering if-else statements. Adding new transition is just a map update—no code modification needed."*

### Statement 3: Liskov Substitution
*"All our repositories and clients are interfaces. We can substitute mock implementations for testing without changing service code, because the service depends on the contract, not the implementation."*

### Statement 4: Interface Segregation
*"We have focused interfaces—DeliveryRepository, TrackingClient, etc. Services depend only on what they need. Our Feign client has just one method: addTrackingEvent. We don't force services to depend on unused methods."*

### Statement 5: Dependency Inversion
*"We inject all dependencies through constructors, depending on abstractions. This makes testing trivial—we mock the interfaces. If we need to switch from RabbitMQ to another message broker, the service doesn't change."*

### Final Statement:
*"Applying SOLID principles makes our code maintainable, testable, and flexible. Each service is independent, focused, and easy to change. This is why we can handle complex workflows across 7 microservices without massive coupling."*

---

## SOLID Principles Checklist for SmartCourier

- ✅ **SRP** - Each class has single responsibility
  - DeliveryController → HTTP
  - DeliveryService → Business logic
  - DeliveryRepository → Data access
  - RabbitMQConfig → Message config

- ✅ **OCP** - Open for extension, closed for modification
  - Status transitions via map
  - Charge calculations via strategy
  - Roles via permission logic

- ✅ **LSP** - Substitutable implementations
  - Repositories for real/mock
  - Clients for real/mock
  - All follow interface contract

- ✅ **ISP** - Focused interfaces
  - DeliveryRepository (1 responsibility)
  - TrackingClient (1 method)
  - Separate repositories for separate services

- ✅ **DIP** - Depend on abstractions
  - Constructor injection
  - Interface dependencies
  - Spring manages implementations


