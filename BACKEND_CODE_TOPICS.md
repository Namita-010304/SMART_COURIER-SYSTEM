# SmartCourier Backend - Main Code Topics

## Overview
SmartCourier backend is built with **Spring Boot microservices**, using **Java 17**, **Spring Cloud**, **MySQL**, and **RabbitMQ**. Below is a comprehensive breakdown of all main topics covered in the backend code.

---

## 1. MICROSERVICES ARCHITECTURE

### Core Services

#### **Auth Service** (Port 8081)
**Location:** `auth-service/src/main/java/com/smartcourier/auth/`

**Main Topics:**
- User registration and authentication
- JWT token generation and validation
- Role-based access control (CUSTOMER, ADMIN, SYSTEM)
- Password hashing and validation

**Key Components:**
```java
AuthController.java      // REST endpoints for login/register
AuthService.java         // Business logic for auth
User.java               // JPA entity for users
AuthRepository.java     // Database access for users
JwtUtil.java            // JWT token utilities
SecurityConfig.java     // Spring Security configuration
```

---

#### **Delivery Service** (Port 8082) - CORE SERVICE
**Location:** `delivery-service/src/main/java/com/smartcourier/delivery/`

**Main Topics:**
- Delivery booking (direct and wizard flow)
- Delivery lifecycle management
- Status transition validation (state machine)
- Shipping charge calculation
- Business rule enforcement
- Event publishing to RabbitMQ

**Key Components:**
```
delivery-service/src/main/java/com/smartcourier/delivery/
├── controller/
│   ├── DeliveryController.java      // REST endpoints
│   └── ServiceCatalogController.java // Service pricing endpoints
│
├── service/
│   └── DeliveryService.java         // Business logic (539 lines)
│       • createDelivery()           // Create new delivery
│       • updateStatus()             // Update delivery status
│       • getMyDeliveries()          // Get customer's deliveries
│       • calculateCharge()          // Calculate shipping cost
│       • publishStatusEvent()       // Publish to RabbitMQ
│       • sendTrackingEventSync()    // Fallback to sync call
│
├── entity/
│   ├── Delivery.java                // Main delivery entity
│   ├── Address.java                 // Sender/receiver address
│   └── ParcelPackage.java           // Package details
│
├── dto/
│   ├── DeliveryRequest.java         // API request model
│   ├── DeliveryResponseDTO.java     // API response model
│   ├── AddressDTO.java              // Address transfer object
│   ├── PackageDTO.java              // Package transfer object
│   └── DeliveryEvent.java           // Event for RabbitMQ
│
├── repository/
│   └── DeliveryRepository.java      // JPA repository
│       • findByUsernameOrderByCreatedAtDesc()
│       • findByTrackingNumber()
│       • findByStatus()
│
├── exception/
│   ├── InvalidStatusTransitionException.java
│   ├── BusinessRuleViolationException.java
│   ├── UnauthorizedAccessException.java
│   └── ResourceNotFoundException.java
│
├── client/
│   └── TrackingClient.java          // Feign client to Tracking Service
│
├── config/
│   ├── RabbitMQConfig.java          // RabbitMQ configuration
│   └── SecurityConfig.java          // Security configuration
│
└── event/
    └── DeliveryStatusChangedEvent.java // Spring event
```

**Database Tables:**
```sql
delivery                    -- Main delivery records
├── id (PK)
├── tracking_number (UQ)
├── username (customer)
├── status (enum: DRAFT, BOOKED, PICKED_UP, IN_TRANSIT, etc)
├── charge (Double)
├── paid (Boolean)
├── sender_address_id (FK)
├── receiver_address_id (FK)
├── parcel_package_id (FK)
├── created_at
└── updated_at

address                     -- Shipping addresses (sender/receiver)
├── id (PK)
├── full_name
├── phone
├── street
├── city
├── state
├── zip_code
└── country

parcel_package             -- Package details
├── id (PK)
├── weight (Double)
├── length, width, height
├── description
├── service_type (DOMESTIC/EXPRESS/INTERNATIONAL)
├── declared_value
└── fragile (Boolean)
```

---

#### **Tracking Service** (Port 8083)
**Location:** `tracking-service/src/main/java/com/smartcourier/tracking/`

**Main Topics:**
- Delivery event history management
- Proof of delivery (POD) handling
- File upload for signatures/photos
- RabbitMQ message consumption
- Real-time delivery timeline tracking

**Key Components:**
```
tracking-service/src/main/java/com/smartcourier/tracking/
├── controller/
│   └── TrackingController.java      // REST endpoints for tracking
│
├── service/
│   └── TrackingService.java         // Tracking business logic
│       • addTrackingEvent()         // Create history entry
│       • getDeliveryHistory()       // Get timeline
│       • addDeliveryProof()         // Add signature/photo
│
├── entity/
│   ├── TrackingEvent.java           // Event history entity
│   └── DeliveryProof.java           // Proof of delivery entity
│
├── repository/
│   ├── TrackingEventRepository.java
│   └── DeliveryProofRepository.java
│
├── listener/
│   └── DeliveryEventListener.java   // RabbitMQ consumer
│       • Listens to delivery.status queue
│       • Processes DeliveryEvent messages
│       • Creates TrackingEvent records
│
└── config/
    └── FileUploadConfig.java        // File storage configuration
```

**Database Tables:**
```sql
tracking_event                      -- Delivery history
├── id (PK)
├── delivery_id (FK)
├── tracking_number
├── status (enum)
├── location
├── message
└── timestamp

delivery_proof                      -- Proof of delivery
├── id (PK)
├── delivery_id (FK)
├── recipient_name
├── signature_url
├── photo_url
├── notes
└── created_at
```

---

#### **Admin Service** (Port 8084)
**Location:** `admin-service/src/main/java/com/smartcourier/admin/`

**Main Topics:**
- Dashboard and analytics
- User management (CRUD)
- Delivery hub management
- Report generation
- Exception resolution
- System-wide deliveries view

**Key Components:**
```
admin-service/src/main/java/com/smartcourier/admin/
├── controller/
│   └── AdminController.java         // Admin REST endpoints
│
├── service/
│   └── AdminService.java            // Admin business logic
│       • getDashboardData()         // KPI and analytics
│       • getAllDeliveries()         // System-wide view
│       • manageUsers()              // User CRUD
│       • generateReport()           // Report generation
│
├── entity/
│   ├── Hub.java                     // Delivery hub entity
│   └── Report.java                  // Report entity
│
├── dto/
│   ├── AdminUserCreateRequest.java
│   └── AdminUserUpdateRequest.java
│
├── client/
│   ├── DeliveryClient.java          // Feign to Delivery Service
│   ├── TrackingClient.java          // Feign to Tracking Service
│   └── AuthClient.java              // Feign to Auth Service
│
└── repository/
    ├── HubRepository.java
    └── ReportRepository.java
```

---

### API Gateway (Port 9090)
**Location:** `api-gateway/src/main/java/com/smartcourier/gateway/`

**Main Topics:**
- Request routing to microservices
- JWT token validation
- User context header propagation
- Cross-cutting concerns (CORS, security)
- Load balancing preparation

**Key Configuration:**
```java
application.yml             // Gateway routing rules
// Routes:
// /gateway/auth/** → auth-service:8081
// /gateway/deliveries/** → delivery-service:8082
// /gateway/tracking/** → tracking-service:8083
// /gateway/admin/** → admin-service:8084
```

---

### Service Registry (Port 8761)
**Location:** `ServiceRegistry/src/main/java/com/smartcourier/registry/`

**Main Topics:**
- Netflix Eureka configuration
- Service registration
- Service discovery
- Health checks
- Dynamic service lookup

---

### Config Server (Port 8889)
**Location:** `config-server/src/main/java/com/smartcourier/config/`

**Main Topics:**
- Centralized configuration management
- Environment-specific properties
- Git-backed configuration
- Hot reload support

**Configuration Files:** `config-repo/`
```
config-repo/
├── auth-service.yml
├── delivery-service.yml
├── tracking-service.yml
└── admin-service.yml
```

---

## 2. DATA MODELS & ENTITIES (JPA)

### Delivery Entity
```java
@Entity
@Table(name = "delivery")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String trackingNumber;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private String username;
    private Double charge;
    private Boolean paid;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Address senderAddress;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Address receiverAddress;
    
    @OneToOne(cascade = CascadeType.ALL)
    private ParcelPackage parcelPackage;
    
    private String specialInstructions;
    private LocalDateTime scheduledPickup;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Status Enum
```java
public enum DeliveryStatus {
    DRAFT,              // Initial customer draft
    BOOKED,             // Confirmed booking
    PICKED_UP,          // Picked up from sender
    IN_TRANSIT,         // On delivery route
    OUT_FOR_DELIVERY,   // Near recipient
    DELIVERED,          // Successfully delivered
    FAILED,             // Delivery failed
    DELAYED,            // Delayed delivery
    RETURNED            // Returned to sender
}
```

### Address Entity
```java
@Entity
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String fullName;
    private String phone;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
```

### ParcelPackage Entity
```java
@Entity
@Table(name = "parcel_package")
public class ParcelPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;
    
    private Double declaredValue;
    private Boolean fragile;
}
```

### TrackingEvent Entity
```java
@Entity
@Table(name = "tracking_event")
public class TrackingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long deliveryId;
    private String trackingNumber;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private String location;
    private String message;
    
    @CreationTimestamp
    private LocalDateTime timestamp;
}
```

---

## 3. DATA TRANSFER OBJECTS (DTOs)

### DeliveryRequest (API Input)
```java
public class DeliveryRequest {
    private AddressDTO senderAddress;
    private AddressDTO receiverAddress;
    private PackageDTO packageDetails;
    private String status;
    private String specialInstructions;
    private String scheduledPickup;
}
```

### DeliveryResponseDTO (API Output)
```java
public class DeliveryResponseDTO {
    private Long id;
    private String trackingNumber;
    private String username;
    private AddressDTO senderAddress;
    private AddressDTO receiverAddress;
    private PackageDTO packageDetails;
    private DeliveryStatus status;
    private Double charge;
    private String specialInstructions;
    private Boolean paid;
    private LocalDateTime scheduledPickup;
    private LocalDateTime createdAt;
}
```

---

## 4. REST API ENDPOINTS

### Authentication Endpoints
```
POST   /auth/register        - Register new user
POST   /auth/login          - User login (returns JWT)
GET    /auth/validate       - Validate JWT token
```

### Delivery Endpoints
```
// Wizard Flow (Step-by-step)
POST   /deliveries/draft                    - Start wizard (DRAFT)
PUT    /deliveries/{id}/sender             - Add sender address
PUT    /deliveries/{id}/receiver           - Add receiver address
PUT    /deliveries/{id}/package            - Add package details
PUT    /deliveries/{id}/finalize           - Finalize (DRAFT → BOOKED)

// Direct Operations
POST   /deliveries                          - Create delivery directly
GET    /deliveries                          - Get all deliveries
GET    /deliveries/{id}                     - Get delivery by ID
GET    /deliveries/tracking/{number}        - Track by tracking number
PUT    /deliveries/{id}/status              - Update status

// Service Catalog
GET    /services/info                       - Get service catalog
```

### Tracking Endpoints
```
GET    /tracking/{deliveryId}/events        - Get delivery history
POST   /tracking/{deliveryId}/proof         - Upload proof of delivery
GET    /tracking/{deliveryId}/proof         - Get proof of delivery
```

### Admin Endpoints
```
GET    /admin/dashboard                     - Dashboard data (KPIs)
GET    /admin/deliveries                    - All deliveries
GET    /admin/deliveries/{id}               - Delivery by ID
PUT    /admin/deliveries/{id}/resolve       - Resolve exception
GET    /admin/hubs                          - List delivery hubs
POST   /admin/hubs                          - Create hub
GET    /admin/users                         - List users
POST   /admin/users                         - Create user
PUT    /admin/users/{id}                    - Update user
DELETE /admin/users/{id}                    - Delete user
POST   /admin/reports                       - Generate report
```

---

## 5. BUSINESS LOGIC & ALGORITHMS

### Charge Calculation Algorithm
```java
private Double calculateCharge(ParcelPackage parcel) {
    // Base price by service type
    double base = switch(parcel.getServiceType()) {
        case EXPRESS -> 14.99;
        case INTERNATIONAL -> 29.99;
        default -> 5.99;  // DOMESTIC
    };
    
    // Add weight-based charge
    double weightCharge = parcel.getWeight() * 0.5;
    
    // Add fragile surcharge
    double fragileCharge = Boolean.TRUE.equals(parcel.getFragile()) ? 3.0 : 0;
    
    // Total with rounding
    return Math.round((base + weightCharge + fragileCharge) * 100.0) / 100.0;
}

// Example: EXPRESS, 10kg, fragile
// = 14.99 + (10 * 0.5) + 3.0 = $22.99
```

### Status Transition Validation (State Machine)
```java
private static final Map<DeliveryStatus, List<DeliveryStatus>> ALLOWED_TRANSITIONS = 
    Map.of(
        DRAFT, List.of(BOOKED),
        BOOKED, List.of(PICKED_UP, FAILED),
        PICKED_UP, List.of(IN_TRANSIT, DELAYED, FAILED),
        IN_TRANSIT, List.of(OUT_FOR_DELIVERY, DELAYED, FAILED),
        OUT_FOR_DELIVERY, List.of(DELIVERED, FAILED, DELAYED),
        DELIVERED, List.of(),  // Terminal
        DELAYED, List.of(IN_TRANSIT, OUT_FOR_DELIVERY, FAILED),
        FAILED, List.of(RETURNED, IN_TRANSIT, OUT_FOR_DELIVERY),
        RETURNED, List.of()    // Terminal
    );

private boolean canTransition(String role, DeliveryStatus from, 
                             DeliveryStatus to, Delivery delivery, String username) {
    if ("CUSTOMER".equalsIgnoreCase(role)) {
        if (!delivery.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("Not your delivery");
        }
        return from == DRAFT && to == BOOKED;
    }
    
    if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
    }
    
    return false;
}
```

### Tracking Number Generation
```java
private String generateTrackingNumber() {
    return "SC" +                           // Prefix
           System.currentTimeMillis() +     // Timestamp
           (int)(Math.random() * 1000);     // Random suffix
    // Example: SC1704200000XXX
}
```

---

## 6. DATABASE ACCESS (JPA & REPOSITORIES)

### Custom JPA Queries
```java
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    // Custom queries for common operations
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByStatus(DeliveryStatus status);
    Long countByStatus(DeliveryStatus status);
    
    // Used in admin service for analytics
}

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByDeliveryIdOrderByTimestampDesc(Long deliveryId);
    List<TrackingEvent> findByTrackingNumber(String trackingNumber);
}
```

### Database Transactions
```java
@Service
@Transactional  // All methods run in transaction
public class DeliveryService {
    
    @Transactional  // Can override at method level
    public DeliveryResponseDTO createDelivery(...) {
        // All DB operations rolled back if exception occurs
        Delivery delivery = Delivery.builder()...build();
        Delivery saved = deliveryRepository.save(delivery);  // Within transaction
        publishStatusEvent(saved, null, "...");             // Within transaction
        return mapToResponseDTO(saved);
    }
    
    @Transactional(readOnly = true)  // Read-only transaction
    public List<DeliveryResponseDTO> getMyDeliveries(String username) {
        return deliveryRepository.findByUsernameOrderByCreatedAtDesc(username)
            .stream()
            .map(this::mapToResponseDTO)
            .toList();
    }
}
```

---

## 7. COMMUNICATION & MESSAGING

### Asynchronous Communication (RabbitMQ)

#### Configuration
```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "delivery.status";
    public static final String ROUTING_KEY = "delivery.status";
    public static final String QUEUE = "tracking-service-events";
    
    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
    
    @Bean
    public Queue trackingServiceQueue() {
        return new Queue(QUEUE, true);
    }
    
    @Bean
    public Binding bindingTrackingQueue(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(ROUTING_KEY);
    }
}
```

#### Publishing Events
```java
@Service
public class DeliveryService {
    private final RabbitTemplate rabbitTemplate;
    
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
            // Publish to RabbitMQ (async)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
            );
        } catch (Exception e) {
            // Fallback to sync if RabbitMQ fails
            sendTrackingEventSync(delivery, location, message);
        }
    }
}
```

#### Consuming Events
```java
@Service
public class TrackingService {
    private final TrackingRepository trackingRepository;
    
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onDeliveryStatusChanged(DeliveryEvent event) {
        log.info("Received event: {} for delivery {}", 
                 event.getStatus(), event.getDeliveryId());
        
        TrackingEvent trackingEvent = TrackingEvent.builder()
            .deliveryId(event.getDeliveryId())
            .trackingNumber(event.getTrackingNumber())
            .status(DeliveryStatus.valueOf(event.getStatus()))
            .location(event.getLocation())
            .message(event.getMessage())
            .timestamp(event.getTimestamp())
            .build();
        
        trackingRepository.save(trackingEvent);
    }
}
```

### Synchronous Communication (Feign Client)

#### Define Client Interface
```java
@FeignClient(name = "tracking-service")
public interface TrackingClient {
    @PostMapping("/tracking/event")
    void addTrackingEvent(Map<String, Object> request);
}
```

#### Use Client
```java
@Service
public class DeliveryService {
    private final TrackingClient trackingClient;
    
    public void sendTrackingEventSync(Delivery delivery, String location, String message) {
        Map<String, Object> request = new HashMap<>();
        request.put("deliveryId", delivery.getId());
        request.put("trackingNumber", delivery.getTrackingNumber());
        request.put("status", delivery.getStatus().name());
        request.put("location", location);
        request.put("description", message);
        
        // Synchronous HTTP call to Tracking Service
        trackingClient.addTrackingEvent(request);
    }
}
```

---

## 8. EXCEPTION HANDLING

### Custom Exceptions
```java
// Custom exception hierarchy for delivery context

public class BusinessRuleViolationException extends RuntimeException {
    // Thrown when business rule is violated
    // Example: Invalid charge calculation
}

public class InvalidStatusTransitionException extends RuntimeException {
    // Thrown when status transition is not allowed
    // Example: DRAFT → DELIVERED (skips intermediate states)
}

public class UnauthorizedAccessException extends RuntimeException {
    // Thrown when user doesn't have permission
    // Example: Customer accessing another customer's delivery
}

public class ResourceNotFoundException extends RuntimeException {
    // Thrown when resource doesn't exist
    // Example: Delivery ID not found
}
```

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage(), 404));
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(e.getMessage(), 403));
    }
    
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStatusTransitionException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage(), 400));
    }
}
```

---

## 9. SECURITY & AUTHENTICATION

### JWT Token Management
```java
@Component
public class JwtUtil {
    
    public String generateToken(String username, String role) {
        return Jwts.builder()
            .setSubject(username)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }
    
    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### Spring Security Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/auth/register", "/auth/login").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .httpBasic();
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Role-Based Access Control
```java
@Service
public class DeliveryService {
    
    public boolean canTransition(String role, DeliveryStatus from, DeliveryStatus to,
                                Delivery delivery, String username) {
        // CUSTOMER: Can only DRAFT → BOOKED
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            if (!delivery.getUsername().equals(username)) {
                throw new UnauthorizedAccessException("Not your delivery");
            }
            return from == DRAFT && to == BOOKED;
        }
        
        // ADMIN: Can do most transitions
        if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
            return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
        }
        
        return false;
    }
}
```

---

## 10. VALIDATION & CONSTRAINTS

### Input Validation
```java
@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
    
    @PostMapping
    public ResponseEntity<DeliveryResponseDTO> createDelivery(
            @Valid @RequestBody DeliveryRequest request,  // Validates against annotations
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        
        return ResponseEntity.ok(deliveryService.createDelivery(request, username, role));
    }
}

@Data
public class DeliveryRequest {
    @NotNull(message = "Sender address required")
    private AddressDTO senderAddress;
    
    @NotNull(message = "Receiver address required")
    private AddressDTO receiverAddress;
    
    @NotNull(message = "Package details required")
    private PackageDTO packageDetails;
    
    @Pattern(regexp = "^(DRAFT|BOOKED|PICKED_UP|IN_TRANSIT|OUT_FOR_DELIVERY|DELIVERED|FAILED|DELAYED|RETURNED)$")
    private String status;
}
```

### Database Constraints
```java
@Entity
@Table(name = "delivery")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)  // Database constraint
    private String trackingNumber;
    
    @Column(nullable = false)
    private String username;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;
    
    @Column(nullable = false)
    private Double charge;
}
```

---

## 11. CONFIGURATION MANAGEMENT

### Application Properties
```yaml
# application.yml (environment-specific)
spring:
  application:
    name: delivery-service
  datasource:
    url: jdbc:mysql://mysql:3306/delivery_db
    username: root
    password: system
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.MySQL8Dialect
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest

eureka:
  client:
    serviceUrl:
      defaultZone: http://service-registry:8761/eureka/

management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Config Server (Centralized)
```yaml
# In config-repo/delivery-service.yml
delivery:
  wizard:
    enabled: true
  charge:
    domestic:
      base: 5.99
    express:
      base: 14.99
    international:
      base: 29.99
  status-transitions:
    validated: true
```

---

## 12. LOGGING & MONITORING

### Structured Logging
```java
@Service
@Slf4j  // Lombok annotation for SLF4J
public class DeliveryService {
    
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, 
                                             String username, String role) {
        log.info("Creating delivery for user: {} with role: {}", username, role);
        
        try {
            Delivery delivery = buildDelivery(request, username, role);
            Delivery saved = deliveryRepository.save(delivery);
            
            log.debug("Delivery created with ID: {} and tracking: {}", 
                     saved.getId(), saved.getTrackingNumber());
            
            publishStatusEvent(saved, null, "Initial status: " + delivery.getStatus());
            
            return mapToResponseDTO(saved);
        } catch (Exception e) {
            log.error("Error creating delivery for user: {}", username, e);
            throw e;
        }
    }
}
```

### Distributed Tracing Integration
```java
// Automatic with Spring Cloud Sleuth + Zipkin
// Every HTTP request gets:
// - Trace ID (unique request ID)
// - Span ID (operation ID)
// - Parent Span ID (for nested calls)

// Trace example:
// Request: GET /gateway/deliveries/123
// └─ api-gateway span (2ms)
//    └─ delivery-service span (30ms)
//       └─ database query span (15ms)

// All automatically captured and sent to Zipkin (9411)
```

---

## 13. TESTING STRATEGY

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {
    
    @Mock
    private DeliveryRepository deliveryRepository;
    
    @Mock
    private TrackingClient trackingClient;
    
    @InjectMocks
    private DeliveryService service;
    
    @Test
    void testCalculateCharge() {
        ParcelPackage pkg = ParcelPackage.builder()
            .weight(10.0)
            .serviceType(ServiceType.EXPRESS)
            .fragile(true)
            .build();
        
        Double charge = service.calculateCharge(pkg);
        
        // EXPRESS: 14.99 + (10 * 0.5) + 3.0 = 22.99
        assertEquals(22.99, charge);
    }
    
    @Test
    void testStatusTransitionValidation() {
        Delivery delivery = Delivery.builder()
            .status(DeliveryStatus.DRAFT)
            .username("john")
            .build();
        
        boolean canTransition = service.canTransition(
            "CUSTOMER", DeliveryStatus.DRAFT, DeliveryStatus.BOOKED, delivery, "john");
        
        assertTrue(canTransition);
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private DeliveryRepository deliveryRepository;
    
    @Test
    void testCreateDeliveryEndpoint() throws Exception {
        DeliveryRequest request = new DeliveryRequest();
        // ... setup request ...
        
        mockMvc.perform(post("/deliveries")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request))
            .header("X-User-Username", "john")
            .header("X-User-Role", "CUSTOMER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackingNumber").exists());
    }
}
```

---

## 14. BUILD & DEPLOYMENT

### Maven Build
```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Spring AMQP (RabbitMQ) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- Spring Cloud (Eureka, Feign, Config) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- MySQL Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    
    <!-- Code Generation -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- JaCoCo (Code Coverage) -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
        </plugin>
        
        <!-- SonarQube (Code Quality) -->
        <plugin>
            <groupId>org.sonarsource.scanner.maven</groupId>
            <artifactId>sonar-maven-plugin</artifactId>
            <version>3.10.0.2594</version>
        </plugin>
    </plugins>
</build>
```

### Docker Build
```dockerfile
# Dockerfile
FROM maven:3.8.1-openjdk-17 as build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
COPY --from=build /app/target/delivery-service-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 15. KEY ALGORITHMS & PATTERNS

### Wizard Flow Pattern
```
Step 1: Init Draft
  POST /deliveries/draft
  → Create DRAFT delivery

Step 2-4: Build Iteratively
  PUT /deliveries/{id}/sender
  PUT /deliveries/{id}/receiver
  PUT /deliveries/{id}/package
  → Update as needed

Step 5: Finalize
  PUT /deliveries/{id}/finalize
  → Validate all complete
  → Transition DRAFT → BOOKED
  → Publish event
```

### Charge Calculation Strategy
```
charge = basePrice(serviceType) + weight * 0.5 + fragile ? 3.0 : 0

ServiceType:
- DOMESTIC: 5.99
- EXPRESS: 14.99
- INTERNATIONAL: 29.99
```

### Event Sourcing Pattern (Implicit)
```
1. Delivery status changes → Event created
2. Event published to RabbitMQ
3. Tracking Service consumes → Creates history
4. History is audit trail of all changes
```

### State Machine Pattern
```
ALLOWED_TRANSITIONS map defines valid transitions
canTransition() validates before allowing change
Prevents invalid state sequences
```

---

## Summary: Main Backend Topics

1. **Microservices** - 7 independent services (Auth, Delivery, Tracking, Admin, Gateway, Eureka, Config)
2. **REST APIs** - 30+ endpoints for delivery lifecycle management
3. **Database Design** - 4 databases with JPA entities and repositories
4. **Business Logic** - Charge calculation, status machine, validation
5. **Async Messaging** - RabbitMQ for event-driven communication
6. **Synchronous Communication** - Feign clients for inter-service calls
7. **Security** - JWT authentication, role-based access control
8. **Exception Handling** - Custom exceptions and global error handling
9. **Configuration Management** - Centralized Config Server
10. **Testing** - Unit and integration tests with mocks
11. **Monitoring** - Zipkin tracing, SonarQube quality
12. **Deployment** - Docker containerization, Maven build
13. **Design Patterns** - Repository, Factory, Strategy, State Machine, Saga
14. **SOLID Principles** - Applied throughout codebase
15. **Data Models** - Delivery, Address, Package, TrackingEvent entities


