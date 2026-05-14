# SmartCourier - Visual Architecture & System Diagrams

## 1. High-Level System Architecture

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                                  CLIENTS                                        ┃
┃  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐             ┃
┃  │   Web Browser    │  │   Mobile App     │  │  Admin Dashboard │             ┃
┃  │   (Angular)      │  │   (iOS/Android)  │  │  (Angular)       │             ┃
┃  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘             ┃
┗━━━━━━━━━━┃────────────────────────┃─────────────────────┃───────────────────━━┛
           │                        │                     │ HTTP(S) Requests
           └────────────┬───────────┴─────────┬───────────┘
                        │                     │
           ┌────────────▼─────────────────────▼────────────┐
           │         API Gateway (Port 9090)               │
           │  • JWT Token Validation                       │
           │  • Request Routing                            │
           │  • Header Propagation (User Context)          │
           │  • CORS & Security Headers                    │
           └┬─────────────┬──────────────┬──────────────┬──┘
            │             │              │              │
    ┌───────▼──────┐ ┌────▼────┐ ┌──────▼────┐ ┌──────▼─────┐
    │  Auth Service│ │ Delivery │ │  Tracking │ │   Admin    │
    │   (8081)     │ │ Service  │ │  Service  │ │  Service   │
    │              │ │ (8082)   │ │  (8083)   │ │  (8084)    │
    │ • Register   │ │          │ │           │ │            │
    │ • Login      │ │ • Create │ │ • History │ │ • Dashboard│
    │ • JWT Issue  │ │ • Status │ │ • Events  │ │ • Reports  │
    │ • Validate   │ │ • Charge │ │ • POD     │ │ • Users    │
    └───────┬──────┘ └────┬─────┘ │ • Proof   │ │ • Hubs     │
            │             │       │           │ │            │
            │      ┌──────┴───────┤           │ └──────┬─────┘
            │      │              │                    │
    ┌───────▼──────▼────┐    ┌────▼──────────────────┼─────┐
    │    MySQL 8.0      │    │  RabbitMQ Message     │     │
    │   (Persistent)    │    │  Broker (5672/15672)  │     │
    │  ┌─────────────┐  │    │                       │     │
    │  │  auth_db    │  │    │ DeliveryEvent Stream  │     │
    │  │ delivery_db │  │    │ (delivery.status)     │     │
    │  │ tracking_db │  │    │                       │     │
    │  │  admin_db   │  │    └───────────────────────┘     │
    │  └─────────────┘  │                                  │
    └───────────────────┘                                  │
                             ┌──────────────────────────────┘
                             │
                    RabbitMQ Consumer
                    (Tracking Service)
                             │
                    Creates TrackingEvent
```

---

## 2. Service Discovery & Registration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    EUREKA SERVICE REGISTRY                  │
│                    (Port 8761 - Eureka)                     │
│                                                             │
│  Registered Services:                                      │
│  ├─ auth-service:8081 (status: UP)                         │
│  ├─ delivery-service:8082 (status: UP)                     │
│  ├─ tracking-service:8083 (status: UP)                     │
│  ├─ admin-service:8084 (status: UP)                        │
│  └─ api-gateway:9090 (status: UP)                          │
│                                                             │
│  Health Checks: Every 30 seconds                           │
│  Auto-deregister: After 3 failed health checks             │
└─────────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲
         │                    │                    │
         │ Heartbeat          │ Heartbeat          │ Heartbeat
         │ (every 30s)        │ (every 30s)        │ (every 30s)
         │                    │                    │
    ┌────┴────┐          ┌───┴──────┐        ┌────┴────┐
    │  Auth   │          │ Delivery │        │ Tracking│
    │ Service │          │ Service  │        │ Service │
    │         │          │          │        │         │
    │ Queries │          │ Queries  │        │ Queries │
    │ Eureka  │          │ Eureka   │        │ Eureka  │
    │ for     │          │ for      │        │ for     │
    │ other   │          │ other    │        │ other   │
    │ service │          │ service  │        │ service │
    │ URLs    │          │ URLs     │        │ URLs    │
    └────────┘          └──────────┘        └────────┘
```

---

## 3. Authentication & Authorization Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT AUTHENTICATION FLOW                  │
└─────────────────────────────────────────────────────────────────┘

STEP 1: User Registration
  ┌─────────────┐
  │   Client    │
  └─────┬───────┘
        │
        │ POST /gateway/auth/register
        │ {username: "john", password: "123", role: "CUSTOMER"}
        │
        ▼
  ┌──────────────────┐
  │  API Gateway     │
  │ (No validation   │
  │  needed yet)     │
  └────────┬─────────┘
           │
           │ Forward to Auth Service
           │
           ▼
  ┌──────────────────────────────┐
  │    Auth Service (8081)       │
  │ • Save user in auth_db       │
  │ • Hash password              │
  │ • Response: {success: true}  │
  └──────────────────────────────┘


STEP 2: User Login
  ┌─────────────┐
  │   Client    │
  └─────┬───────┘
        │
        │ POST /gateway/auth/login
        │ {username: "john", password: "123"}
        │
        ▼
  ┌──────────────────────────────────────┐
  │    Auth Service (8081)               │
  │ • Verify credentials                 │
  │ • Generate JWT: sign(payload, key)   │
  │ • Payload contains: username, role   │
  └────────┬─────────────────────────────┘
           │
           │ Response JWT
           │ { token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
           │
           ▼
  ┌─────────────┐
  │   Client    │
  │ (Store JWT) │
  │ localStorage│
  └─────────────┘


STEP 3: Subsequent Requests (Protected)
  ┌─────────────┐
  │   Client    │
  └─────┬───────┘
        │
        │ GET /gateway/deliveries/123
        │ Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        │
        ▼
  ┌─────────────────────────────────────┐
  │    API Gateway (9090)               │
  │ JwtAuthenticationFilter:            │
  │ • Extract token from header         │
  │ • Verify signature                  │
  │ • Check expiration                  │
  │ • Extract username, role            │
  │ • Decode: {username: "john",        │
  │            role: "CUSTOMER"}        │
  └────────┬────────────────────────────┘
           │
           │ Validated ✓
           │ Add headers:
           │ X-User-Username: john
           │ X-User-Role: CUSTOMER
           │
           ▼
  ┌──────────────────────────────────┐
  │   Delivery Service (8082)        │
  │ • Check: Is "john" authorized?   │
  │ • Example: Check delivery.user   │
  │   equals "john"                  │
  │ • Re-validate in service layer   │
  │ • Response with data             │
  └──────────────────────────────────┘


AUTHENTICATION FAILURE FLOWS:

Invalid/Expired Token:
  → API Gateway returns 401 Unauthorized
  → Client should request new login

Insufficient Permissions:
  → Service returns 403 Forbidden
  → Example: Customer tries to update other's delivery
```

---

## 4. Delivery Creation & Status Update Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│      CUSTOMER DELIVERY BOOKING (WIZARD MODE)                    │
└─────────────────────────────────────────────────────────────────┘

STEP 1: Initialize Draft
  Customer                    API Gateway              Delivery Service
     │                              │                         │
     │─ POST /gateway/deliveries/draft
     │  (JWT in header)             │                         │
     │                              │─ Validate JWT           │
     │                              │────────────────────────►│
     │                              │
     │                              │─ Create new Delivery    │
     │                              │  status = DRAFT         │
     │                              │  Save to delivery_db    │
     │                              │                         │
     │                              │◄─ Return {id: 123, ...} │
     │◄─ Response with Delivery ID 123
     │


STEP 2-4: Build Delivery Piece by Piece
  For each PUT request:
  
  Customer                    API Gateway              Delivery Service
     │                              │                         │
     │─ PUT /gateway/deliveries/123/sender
     │  {fullName, phone, street, city, ...}
     │                              │─ Validate JWT           │
     │                              │────────────────────────►│
     │                              │
     │                              │─ findById(123)          │
     │                              │─ Check status == DRAFT  │
     │                              │─ Check username==auth   │
     │                              │─ Update senderAddress   │
     │                              │─ Save to DB             │
     │                              │                         │
     │◄───────────────────────────────────── Return updated delivery


STEP 5: Finalize (DRAFT → BOOKED)
  Customer                    API Gateway              Delivery Service
     │                              │                         │
     │─ PUT /gateway/deliveries/123/finalize
     │  (JWT in header)             │                         │
     │                              │─ Validate JWT           │
     │                              │────────────────────────►│
     │                              │
     │                              │─ Validate:             │
     │                              │  • Sender not null     │
     │                              │  • Receiver not null   │
     │                              │  • Package not null    │
     │                              │  • Status == DRAFT     │
     │                              │                         │
     │                              │─ UPDATE delivery       │
     │                              │  status = BOOKED       │
     │                              │  charge = calculated   │
     │                              │                         │
     │                              │─ publishStatusEvent()  │
     │                              │  to RabbitMQ           │
     │                              ├───────────────────────►│
     │                              │  DeliveryEvent {       │
     │                              │   deliveryId: 123,     │
     │                              │   status: BOOKED,      │
     │                              │   ...                  │
     │                              │  }                     │
     │                              │                        │
     │                              │  Publish to            │
     │                              │  RabbitMQ Exchange:    │
     │                              │  "delivery.status"     │
     │                              │                        │
     │                              │◄─ Response:           │
     │◄──────────────────────────────── {id:123, status:BOOKED}
     │                              
     │                              RabbitMQ Message Queue
     │                                      │
     │                                      ▼
     │                              Tracking Service
     │                              Consumer:
     │                              • Listens for messages
     │                              • Receives DeliveryEvent
     │                              • Creates TrackingEvent
     │                              • Saves to tracking_db
     │                              • Update delivery history
```

---

## 5. Status Update & Async Event Flow

```
┌─────────────────────────────────────────────────────────────────┐
│            DELIVERY STATUS UPDATE (ADMIN/SYSTEM)                │
│            Asynchronous Event Publishing Flow                   │
└─────────────────────────────────────────────────────────────────┘


SCENARIO: Delivery Partner picks up package (BOOKED → PICKED_UP)

  Admin/Partner              API Gateway           Delivery Service
       │                            │                      │
       │─ PUT /gateway/deliveries/123/status?status=PICKED_UP
       │  Header: X-User-Role: ADMIN                      │
       │                            │                      │
       │                            │─ Validate JWT        │
       │                            │────────────────────► │
       │                            │                      │
       │                            │─ canTransition()     │
       │                            │  BOOKED → PICKED_UP  │
       │                            │  ADMIN role? YES ✓   │
       │                            │                      │
       │                            │─ UPDATE delivery     │
       │                            │  status = PICKED_UP  │
       │                            │  Save to DB          │
       │                            │                      │
       │                            │─ publishStatusEvent()
       │                            │  ┌─────────────────┐ │
       │                            │  │ DeliveryEvent:  │ │
       │                            │  │ - deliveryId    │ │
       │                            │  │ - status=PICKED │ │
       │                            │  │ - timestamp     │ │
       │                            │  │ - location      │ │
       │                            │  │ - message       │ │
       │                            │  └─────────────────┘ │
       │                            │                      │
       │◄────────────────────────────────── Response sent
       │                                  immediately
       │
       │
       └──────────────────────────┬──────────────────────────┐
                                  │                          │
                          RabbitMQ Message Broker            │
                             (5672:5672)                      │
                                  │                          │
                     Exchange: "delivery.status"             │
                     Routing Key: "delivery.status"          │
                     Queue: "tracking-service-queue"         │
                                  │                          │
                                  ├──ack timeout──────────┐  │
                                  │     wait 30s          │  │
                                  │                       │  │
                            ┌─────▼──────────┐            │  │
                            │ Tracking Service
                            │   Consumer       │            │  │
                            │ (8083)           │            │  │
                            └──────┬──────────┘            │  │
                                   │                       │  │
                    ┌──────────────────────────────────────┘  │
                    │ Process Message:                        │
                    │ • Deserialize event                     │
                    │ • Create TrackingEvent entity           │
                    │ • Save to tracking_db                   │
                    │ • Send ack to RabbitMQ                  │
                    │   (message removed from queue)          │
                    │                                        │
                    └┬──────────────────────────────────────┘
                     │
             ┌───────▼────────────┐
             │   tracking_db      │
             │                    │
             │  TrackingEvent:    │
             │  • id: 456         │
             │  • deliveryId: 123 │
             │  • status: PICKED  │
             │  • location: Hub A │
             │  • timestamp: now  │
             │  • message: ...    │
             └────────────────────┘


FALLBACK SCENARIO (If RabbitMQ Down):

Delivery Service:
  try {
    rabbitTemplate.convertAndSend(...)   ← Fails (Connection refused)
  } catch (Exception e) {
    log.warn("RabbitMQ failed, falling back to sync");
    
    sendTrackingEventSync()  ← Direct Feign call
    │
    └─► REST call to /tracking/123/events
        (Synchronous, blocking)
        │
        └─► Tracking Service still persists event
```

---

## 6. Complete Request Journey

```
┌─────────────────────────────────────────────────────────────────┐
│        TRACING A SINGLE REQUEST (GET Delivery Details)          │
└─────────────────────────────────────────────────────────────────┘

TIME   │ COMPONENT           │ ACTION
───────┼─────────────────────┼──────────────────────────────────────────
T0     │ Client (Browser)    │ GET /delivery/123
       │                     │ Header: Authorization: Bearer JWT
       │
T1ms   │ Network             │ HTTP request travels to server
       │
T2ms   │ Nginx/Load Balancer │ Route to API Gateway instance
       │ (if exists)         │
       │
T3ms   │ API Gateway         │ • Extract JWT from header
       │ Port 9090           │ • Validate JWT signature
       │                     │ • Check token expiration
       │                     │ • Extract: username="john", role="CUSTOMER"
       │                     │ • Add headers: X-User-Username, X-User-Role
       │                     │ • Route to /deliveries/123
       │                     │ • Check Eureka for Delivery Service URL
       │
T8ms   │ Service Discovery   │ • Query Eureka: "where is delivery-service?"
       │ (Eureka via client- │ • Get: delivery-service:8082
       │  side lookup cache) │
       │
T15ms  │ Delivery Service    │ • Receive request with auth headers
       │ Port 8082           │ • DeliveryController (REST handler)
       │                     │ • @PreAuthorize checks role
       │                     │ • Call DeliveryService.getDeliveryById(123)
       │
T18ms  │ DeliveryService     │ • Check: delivery.username = "john"?
       │ (Business Logic)    │ • Look up delivery ID 123 in JPA repository
       │
T22ms  │ Hibernate ORM       │ • Generate SQL query:
       │                     │   SELECT * FROM delivery WHERE id = 123
       │
T28ms  │ MySQL Database      │ • Query delivery_db table: 'delivery'
       │ delivery_db         │ • Query joined tables (Address, ParcelPackage)
       │ Port 3306           │ • Return result set
       │
T35ms  │ Hibernate ORM       │ • Map result to Delivery entity
       │                     │ • Map nested Address entities
       │                     │ • Map nested ParcelPackage entity
       │
T40ms  │ DeliveryService     │ • Call mapToResponseDTO()
       │                     │ • Transform JPA entity to DTO
       │
T42ms  │ DeliveryController  │ • Response: {
       │                     │    id: 123,
       │                     │    status: BOOKED,
       │                     │    trackingNumber: SC1704200000XXX,
       │                     │    charge: 13.99,
       │                     │    ...
       │                     │  }
       │
T44ms  │ Spring JSON         │ • Serialize DTO to JSON
       │ Serializer          │
       │
T45ms  │ API Gateway         │ • Receive response from delivery service
       │                     │ • Pass through response
       │
T47ms  │ Network             │ • HTTP response travels back to client
       │
T50ms  │ Client (Browser)    │ • Receive JSON response
       │ JavaScript          │ • Display delivery details on page
       │
TOTAL: 50ms

WITH TRACING (Zipkin):
  Each span is tracked with:
  • Service name
  • Operation name (HTTP method + path)
  • Duration
  • Dependencies
  • Any errors/exceptions

Zipkin Dashboard shows:
  GET /deliveries/123
  ├─ api-gateway (5ms)
  │  └─ ServiceLookup (0.5ms)
  ├─ delivery-service (30ms)
  │  ├─ DeliveryController (2ms)
  │  ├─ DeliveryService.getDeliveryById (15ms)
  │  │  └─ Database query (10ms)
  │  └─ JSON serialization (1ms)
  └─ network latency (12ms)
```

---

## 7. Database Schema - Delivery Service

```
┌─────────────────────────────┐
│        delivery_db          │
└─────────────────────────────┘
         │
         ├─── delivery (main table)
         │    ┌────────────────────────┐
         │    │ id                (PK) │
         │    │ tracking_number   (UQ) │
         │    │ username              │
         │    │ status (enum)         │
         │    │ charge                │
         │    │ paid (boolean)        │
         │    │ special_instructions  │
         │    │ scheduled_pickup      │
         │    │ sender_address_id(FK) │───┐
         │    │ receiver_address_id(FK)—┐ │
         │    │ parcel_package_id(FK) │ │ │
         │    │ created_at            │ │ │
         │    │ updated_at            │ │ │
         │    └────────────────────────┘ │ │
         │                               │ │
         ├─── address (shipping addresses)
         │    ┌────────────────────────┐  │
         │    │ id                (PK) ◄──┴─┐
         │    │ full_name             │    │
         │    │ phone                 │    │
         │    │ street                │    │
         │    │ city                  │    │
         │    │ state                 │    │
         │    │ zip_code              │    │
         │    │ country               │    │
         │    └────────────────────────┘    │
         │                                  │
         ├─── parcel_package (goods info)
         │    ┌────────────────────────┐
         │    │ id                (PK) ◄─┐
         │    │ weight (double)       │   │
         │    │ length (double)       │   │
         │    │ width (double)        │   │
         │    │ height (double)       │   │
         │    │ description           │   │
         │    │ service_type (enum)   │   │
         │    │ declared_value        │   │
         │    │ fragile (boolean)     │   │
         │    └────────────────────────┘   │
         │                                 │
         └── (FK relationship from delivery)


Entity Relationships (JPA):

  Delivery (1) ──────┬──────────► (1) Address (Sender)
                     │
                     ├──────────► (1) Address (Receiver)
                     │
                     └──────────► (1) ParcelPackage

Status Enum Values:
  DRAFT, BOOKED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY,
  DELIVERED, FAILED, DELAYED, RETURNED

Service Type Enum Values:
  DOMESTIC, EXPRESS, INTERNATIONAL
```

---

## 8. Tracking Service - Event History Model

```
┌─────────────────────────────────────────┐
│          tracking_db                    │
└─────────────────────────────────────────┘
         │
         ├─── tracking_event (Event History)
         │    ┌──────────────────────────────┐
         │    │ id                      (PK) │
         │    │ delivery_id                  │
         │    │ tracking_number              │
         │    │ status (enum)                │
         │    │ location (string)            │
         │    │ message                      │
         │    │ timestamp                    │
         │    ├─ Index: delivery_id          │
         │    └─ Index: timestamp            │
         │    └──────────────────────────────┘
         │
         ├─── delivery_proof (Proof of Delivery)
         │    ┌──────────────────────────────┐
         │    │ id                      (PK) │
         │    │ delivery_id                  │
         │    │ recipient_name               │
         │    │ signature_url                │
         │    │ photo_url                    │
         │    │ notes                        │
         │    │ created_at                   │
         │    └──────────────────────────────┘
         │
         └─── file_uploads (Stored documents)
              Path: /app/uploads/{deliveryId}/
              Files:
              • signature_{uuid}.png
              • photo_{uuid}.jpg
              • receipt_{uuid}.pdf


Example Timeline for Delivery 123:

Delivery ID: 123
Timeline Events (ordered by timestamp):

  2024-05-01 10:00:00 | BOOKED          | (null)         | "Delivery confirmed via wizard"
  2024-05-01 11:30:00 | PICKED_UP       | "Hub A"        | "Package picked up from warehouse"
  2024-05-01 14:00:00 | IN_TRANSIT      | "Route 5"      | "On the way to destination"
  2024-05-01 16:45:00 | OUT_FOR_DELIVERY| "Near address" | "Out for delivery in your area"
  2024-05-01 17:20:00 | DELIVERED       | "Delivered"    | "Delivered successfully"
  2024-05-01 17:21:00 | [POD created]   | (null)         | "POD: signed by John Smith"
```

---

## 9. Message Flow Through RabbitMQ

```
┌───────────────────────────────────────────────────────────────────┐
│                  RabbitMQ Message Infrastructure                  │
│                   (Port 5672: AMQP, 15672: HTTP)                  │
└───────────────────────────────────────────────────────────────────┘

1. Exchange: "delivery.status"
   Type: Topic
   Durable: Yes

2. Queue: Groups of consumers
   Queue: "delivery-service-events"      → Delivery Service listeners
   Queue: "tracking-service-events"      → Tracking Service listeners
   Queue: "notification-service-events"  → Notification Service (if exists)

3. Routing Key: "delivery.status"

4. Message Format (in JSON):
   {
     "deliveryId": 123,
     "trackingNumber": "SC1704200000XXX",
     "status": "PICKED_UP",
     "location": "Hub A",
     "message": "Package picked up",
     "timestamp": "2024-05-01T11:30:00"
   }

Message Flow:
  ┌────────────────┐
  │ Event Occurs   │
  │ (Status change)│
  └────────┬───────┘
           │
           ├─ publishStatusEvent()
           │  └─ DeliveryEvent created
           │     (POJO with all fields)
           │
           ├─ rabbitTemplate.convertAndSend()
           │  ├─ Event serialized to JSON
           │  ├─ Sent to exchange: "delivery.status"
           │  └─ Using routing key: "delivery.status"
           │
           ▼
  ┌────────────────────────────────┐
  │  RabbitMQ Broker (message bus) │
  │  exchange: "delivery.status"   │
  └────────────────────────────────┘
           │
           ├─ Message routed to queues:
           │  ├─ Queue 1: delivery-service-events
           │  ├─ Queue 2: tracking-service-events
           │  └─ Queue 3: ... other consumers
           │
           ▼
  ┌──────────────────────────────────┐    ┌──────────────────┐
  │ Tracking Service Container       │    │ Other Services   │
  │ (Spring AMQP Listener)           │    │ (if any)         │
  │                                  │    │                  │
  │ @RabbitListener annotation:      │    └──────────────────┘
  │ listens to: tracking-service-    │
  │            events queue          │
  │                                  │
  │ When message arrives:            │
  │ 1. Deserialize JSON to Event     │
  │ 2. Method is invoked with event  │
  │ 3. Create TrackingEvent entity   │
  │ 4. Save to tracking_db           │
  │ 5. Send ack to RabbitMQ          │
  │    (message removed from queue)  │
  └──────────────────────────────────┘

Dead Letter Queue (DLQ): 
  If processing fails:
  • Retry 3 times (configurable)
  • If still fails after retries
  • Message sent to DLQ for investigation
  • Manual intervention required

Monitoring (RabbitMQ Management):
  http://localhost:15672 (guest/guest)
  • Queue depth (pending messages)
  • Consumer count
  • Message rate (in/out)
  • Connection status
```

---

## 10. API Gateway Routing Rules

```
┌───────────────────────────────────────────────┐
│        API GATEWAY (Spring Cloud Gateway)     │
│              Port 9090                         │
└───────────────────────────────────────────────┘

ROUTING CONFIGURATION:

Request Path                    Route To                  Port
────────────────────────────────────────────────────────────────
/gateway/auth/**                → auth-service            8081
/gateway/deliveries/**          → delivery-service        8082
/gateway/tracking/**            → tracking-service        8083
/gateway/admin/**               → admin-service           8084
/gateway/services/**            → delivery-service        8082


REQUEST PROCESSING PIPELINE FOR /gateway/deliveries/123:

1. REQUEST ARRIVES
   ├─ URL: /gateway/deliveries/123
   └─ Header: Authorization: Bearer eyJhbGc...

2. JWT VALIDATION FILTER
   ├─ Extract token from header
   ├─ Verify signature (using secret key)
   ├─ Check token expiration
   ├─ Decode to get: username, role, expiration
   └─ If invalid → Return 401 Unauthorized

3. REQUEST ENRICHMENT
   ├─ Create new HTTP headers:
   │  ├─ X-User-Username: john
   │  └─ X-User-Role: CUSTOMER
   ├─ Add trace ID for Zipkin
   └─ Add correlation ID for logging

4. ROUTING DECISION
   ├─ Parse path: /gateway/deliveries/123
   ├─ Extract service: "deliveries"
   ├─ Query Eureka: "delivery-service"?
   ├─ Get instance: delivery-service:8082
   └─ Load balance (if multiple instances)

5. FORWARDING
   ├─ Transform URL:
   │  /gateway/deliveries/123 → /deliveries/123
   ├─ Forward all headers + new ones
   ├─ Forward body (if any)
   └─ Send to: http://delivery-service:8082/deliveries/123

6. RESPONSE HANDLING
   ├─ Wait for response from Delivery Service
   ├─ Add response headers (CORS, etc)
   ├─ Stream response back to client
   └─ Log transaction (timing, status code)

7. ERROR HANDLING
   ├─ Service returns 5xx?
      ├─ Retry (configurable)
      └─ Circuit breaker (if many failures)
   ├─ Service timeout?
      └─ Return 504 Gateway Timeout
   ├─ Auth invalid?
      └─ Return 401 Unauthorized

SECURITY FILTERS (In Order):
  1. JWT Token Extraction Filter
  2. Token Signature Verification Filter
  3. Token Expiration Check Filter
  4. User Context Enrichment Filter
  5. CORS Filter (preflight requests)
  6. Rate Limiting Filter (if enabled)
  7. Request Logging Filter

CROSS-ORIGIN REQUESTS:
  Browser checks: Can javascript.com access api.my-site.com?
  
  Gateway adds headers:
  Access-Control-Allow-Origin: javascript.com
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE
  Access-Control-Allow-Headers: Authorization, Content-Type
  
  Browser allows request to proceed

RESPONSE FLOW:

  Delivery Service           API Gateway              Browser
       │                          │                      │
       │─ JSON Response           │                      │
       │  (200 OK)                │                      │
       │── HTTP Header ───────────│                      │
       │── Response Body ─────────│                      │
       │                          │─ Add CORS Headers    │
       │                          │─ Add Security Headers│
       │                          │─ Stream response ───►│
       │                          │                      │
       │                          │                  Browser parses
       │                          │                  updates DOM
```

---

## 11. Microservice to Microservice Communication

```
┌──────────────────────────────────────────────┐
│  Service-to-Service Communication Options   │
└──────────────────────────────────────────────┘

OPTION 1: REST + Feign Client (Synchronous)

  Admin Service wants to get Delivery data:
  
  ┌──────────────┐
  │ Admin Service│
  │ (8084)       │
  └───────┬──────┘
          │
          │ @FeignClient("delivery-service")
          │ interface DeliveryClient {
          │   @GetMapping("/deliveries/{id}")
          │   DeliveryDTO getDelivery(@PathVariable Long id);
          │ }
          │
          ├─ Service discovery (Eureka)
          │  "delivery-service" resolves to 8082
          │
          ├─ Creates HTTP request:
          │  GET http://delivery-service:8082/deliveries/123
          │  (uses Docker DNS name: delivery-service)
          │
          ├─ Adds correlation headers
          │  (for tracing)
          │
          ├─ Blocks waiting for response
          │  (synchronous)
          │
          ▼
  ┌──────────────────┐
  │ Delivery Service │
  │ (8082)           │
  ├─ Handle request  │
  ├─ Query database  │
  └─ Return JSON ───►
          │
          │ Response arrives
          │ ├─ Parse status: 200 OK
          │ ├─ Parse body (JSON)
          │ ├─ Map to DTO object
          │
          ▼
  ┌──────────────┐
  │ Admin Service│
  │ (continues)  │
  │ Now has data │
  └──────────────┘


OPTION 2: RabbitMQ (Asynchronous, Event-Driven)

  Delivery Service publishes event:
  
  ┌──────────────────┐
  │ Delivery Service │
  │ • Update status  │
  │ • Publish event  │
  └──────────┬───────┘
             │
             ├─ Create DeliveryEvent POJO
             │ {
             │   deliveryId: 123,
             │   status: "PICKED_UP",
             │   timestamp: now,
             │   ...
             │ }
             │
             ├─ rabbitTemplate.convertAndSend(...)
             │ ├─ Connect to RabbitMQ (5672)
             │ ├─ Serialize to JSON
             │ ├─ Publish to exchange
             │ └─ Return immediately
             │    (non-blocking)
             │
             │ Delivery Service continues
             │ without waiting for consumers
             │
             ▼
  ┌──────────────────────┐
  │ RabbitMQ Message Bus │
  │ Exchange: delivery.  │
  │ status              │
  └──────────┬───────────┘
             │
             ├─ Message queued
             │ ├─ Queue 1: tracking-service-events
             │ ├─ Queue 2: notification-service (if)
             │ └─ Queue 3: analytics-service (if)
             │
             ▼
  ┌──────────────────┐    ┌──────────────────┐
  │ Tracking Service │    │ Other Services   │
  │ (if listening)   │    │ (if listening)   │
  ├─ Consumer wakes  │    │                  │
  ├─ Deserialize msg │    ├─ Consume message │
  ├─ Process event   │    ├─ Process event   │
  ├─ Save to DB      │    ├─ Update state    │
  ├─ Send ACK        │    ├─ Send ACK        │
  └─ Message removed ┘    └──────────────────┘


COMPARISON:

                Synchronous (Feign)     │ Asynchronous (RabbitMQ)
────────────────────────────────────────┼─────────────────────────
Blocking?       YES (waits)             │ NO (fire & forget)
Scalability     LOWER                   │ HIGHER
Failure Impact  Caller blocked          │ Queued for retry
Data Consistency IMMEDIATE (ACID)       │ EVENTUAL
Complexity      LOWER                   │ HIGHER
Real-time Need  YES                     │ NO
Load Spike      Can overload sender     │ Absorbed by queue
Latency         ~ 200ms                 │ ~ 10ms (plus async delay)
```

---

## 12. Error Handling & Resilience Flow

```
┌────────────────────────────────────────────────────┐
│     Error Handling & Fallback Mechanisms           │
└────────────────────────────────────────────────────┘

SCENARIO 1: RabbitMQ Connection Fails

  Delivery Service publishes event:
  
  try {
    rabbitTemplate.convertAndSend(...)
  } catch (AmqpException e) {
    log.error("RabbitMQ failed, falling back...");
    sendTrackingEventSync(delivery, location, message);
  }
  
  Flow:
  ┌─ Attempt RabbitMQ send
  ├─ Connection refused error
  ├─ Catch exception
  ├─ Create sync request:
  │  POST http://tracking-service:8083/sync-event
  │  Body: {deliveryId, trackingNumber, status, ...}
  ├─ Wait for response (blocking)
  ├─ Tracking Service processes
  ├─ Event saved to database
  └─ Fallback succeeded ✓
  
  Note: Slower than RabbitMQ, but ensures consistency


SCENARIO 2: Service Not Available (500, Connection Refused)

  Feign Client call fails:
  
  @FeignClient(name = "delivery-service")
  public interface DeliveryClient {
    @GetMapping("/deliveries/{id}")
    DeliveryDTO getDelivery(@PathVariable Long id);
  }
  
  try {
    DeliveryDTO delivery = deliveryClient.getDelivery(123);
  } catch (FeignException e) {
    if (e.status() == 503 || e.getMessage().contains("Connection refused")) {
      // Service is down
      log.warn("Delivery Service unavailable, returning cached data");
      return cache.get(123); // Return stale data if available
    } else if (e.status() == 400) {
      throw new BadRequestException(...);
    } else if (e.status() == 404) {
      throw new ResourceNotFoundException(...);
    }
  }
  
  Strategies:
  1. Return cached response (if available)
  2. Return default/empty response
  3. Throw exception to caller
  4. Retry with exponential backoff


SCENARIO 3: Database Connection Lost

  Flow:
  ┌─ Execute JPA query
  ├─ Database unreachable
  ├─ Connection pool throws exception
  ├─ Spring transaction rolls back
  ├─ Exception propagated up
  ├─ Service returns 500 Internal Server Error
  └─ Client should retry
  
  Resilience:
  • Connection pool retries internally
  • Configurable timeout (wait_timeout)
  • Reconnect on next request
  • Metrics alert on high error rate


SCENARIO 4: Timeout

  Feign call timeout (default 60s):
  
  @FeignClient(
    name = "slow-service",
    configuration = FeignClientConfiguration.class
  )
  public interface SlowClient {
    @GetMapping(path = "/slow", 
                connect-timeout = "5000", 
                read-timeout = "10000")
    Response getSlow();
  }
  
  If takes > 5s to connect or > 10s to read:
  ├─ Feign throws timeoutException
  ├─ Request is abandoned
  ├─ Fallback method invoked (if exists)
  └─ Error returned to client


SCENARIO 5: Invalid State Transition

  Customer tries: DRAFT → DELIVERED (invalid)
  
  Exception thrown:
  ┌─ canTransition(DRAFT, DELIVERED) → false
  ├─ Throw InvalidStatusTransitionException
  ├─ Message: "Invalid transition DRAFT → DELIVERED"
  ├─ @ExceptionHandler converts to:
  │  {
  │    timestamp: "2024-05-01T10:00:00",
  │    status: 400,
  │    error: "Bad Request",
  │    message: "Invalid state transition ...",
  │    path: "/deliveries/123/status"
  │  }
  └─ HTTP 400 Bad Request returned


EXCEPTION HIERARCHY:

  RuntimeException
    │
    ├─ SmartCourierException (custom base)
    │  │
    │  ├─ BusinessRuleViolationException (400)
    │  ├─ InvalidStatusTransitionException (400)
    │  ├─ UnauthorizedAccessException (403)
    │  └─ ResourceNotFoundException (404)
    │
    └─ Spring/System exceptions
       ├─ DataAccessException (500)
       ├─ FeignException (depends on response)
       └─ AmqpException (500)

Global @ExceptionHandler:
  Maps each exception to HTTP status + JSON response
```

---

## Summary: Architecture Highlights

✅ **Scalable** - Each service can scale independently  
✅ **Resilient** - Graceful degradation, fallbacks, retry logic  
✅ **Observable** - Zipkin distributed tracing, logs, metrics  
✅ **Secure** - JWT authentication, role-based access control  
✅ **Maintainable** - Clear service boundaries, separation of concerns  
✅ **Event-Driven** - RabbitMQ for loose coupling  
✅ **Production-Ready** - Health checks, monitoring, code quality tools  


