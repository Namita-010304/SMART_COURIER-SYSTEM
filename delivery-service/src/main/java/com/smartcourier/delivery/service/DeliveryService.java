package com.smartcourier.delivery.service;

import com.smartcourier.delivery.client.TrackingClient;
import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.entity.*;
import com.smartcourier.delivery.exception.*;
import com.smartcourier.delivery.repository.DeliveryRepository;
import com.smartcourier.delivery.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;

@Slf4j
@Service
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final TrackingClient trackingClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;

    private static final Map<DeliveryStatus, List<DeliveryStatus>> ALLOWED_TRANSITIONS = Map.of(
            DeliveryStatus.DRAFT, List.of(DeliveryStatus.BOOKED),
            DeliveryStatus.BOOKED, List.of(DeliveryStatus.PICKED_UP, DeliveryStatus.FAILED),
            DeliveryStatus.PICKED_UP, List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.DELAYED, DeliveryStatus.FAILED),
            DeliveryStatus.IN_TRANSIT, List.of(DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.DELAYED, DeliveryStatus.FAILED),
            DeliveryStatus.OUT_FOR_DELIVERY, List.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED, DeliveryStatus.DELAYED),
            DeliveryStatus.DELIVERED, List.of(),
            DeliveryStatus.DELAYED,
            List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY, DeliveryStatus.FAILED),
            DeliveryStatus.FAILED, List.of(DeliveryStatus.RETURNED, DeliveryStatus.IN_TRANSIT, DeliveryStatus.OUT_FOR_DELIVERY),
            DeliveryStatus.RETURNED, List.of());

    public DeliveryService(DeliveryRepository deliveryRepository,
            TrackingClient trackingClient,
            ApplicationEventPublisher eventPublisher,
            RabbitTemplate rabbitTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.trackingClient = trackingClient;
        this.eventPublisher = eventPublisher;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Creates a new delivery directly (Single-Submission).
     * Validates user roles and initial status transitions.
     */
    @Transactional
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, String username, String role) {
        log.info("Creating delivery for user: {} with role: {}", username, role);

        Address sender = mapToAddressEntity(request.getSenderAddress());
        Address receiver = mapToAddressEntity(request.getReceiverAddress());
        ParcelPackage parcel = mapToParcelPackageEntity(request.getPackageDetails());

        Double charge = calculateCharge(parcel);
         
        DeliveryStatus initialStatus = DeliveryStatus.BOOKED; // default
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            initialStatus = DeliveryStatus.valueOf(request.getStatus().toUpperCase());
        }

        if ("CUSTOMER".equalsIgnoreCase(role)
                && (initialStatus != DeliveryStatus.DRAFT && initialStatus != DeliveryStatus.BOOKED)) {
            throw new UnauthorizedAccessException("Customers can only create DRAFT or BOOKED deliveries.");
        }

        Delivery delivery = Delivery.builder()
                .trackingNumber(generateTrackingNumber())
                .username(username)
                .senderAddress(sender)
                .receiverAddress(receiver)
                .parcelPackage(parcel)
                .status(initialStatus)
                .charge(charge)
                .specialInstructions(request.getSpecialInstructions())
                .build();

        if (request.getScheduledPickup() != null && !request.getScheduledPickup().isEmpty()) {
            delivery.setScheduledPickup(LocalDateTime.parse(request.getScheduledPickup()));
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);

        // ---  Async Communication: RabbitMQ ---
        publishStatusEvent(savedDelivery, null, "Initial status: " + initialStatus);

        return mapToResponseDTO(savedDelivery);
    }

    /**
     * Retrieves all deliveries associated with a specific user.
     *
     * @param username the username of the customer
     * @return a list of DeliveryResponseDTO objects
     */
    public List<DeliveryResponseDTO> getMyDeliveries(String username) {
        return deliveryRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream().map(this::mapToResponseDTO).toList();
    }

    /**
     * Fetches a delivery by its unique ID.
     *
     * @param id the delivery ID
     * @return the DeliveryResponseDTO
     * @throws ResourceNotFoundException if the delivery is not found
     */
    public DeliveryResponseDTO getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));
    }

    /**
     * Fetches a delivery by its tracking number.
     *
     * @param trackingNumber the tracking number
     * @return the DeliveryResponseDTO
     * @throws ResourceNotFoundException if the delivery is not found
     */
    public DeliveryResponseDTO getDeliveryByTrackingNumber(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with tracking number: " + trackingNumber));
    }

    /**
     * Updates the status of an existing delivery, enforcing lifecycle rules.
     *
     * @param id the delivery ID
     * @param status the new target status
     * @param role the user's role
     * @param username the user's username
     * @param reason optional reason for status change
     * @return the updated DeliveryResponseDTO
     * @throws InvalidStatusTransitionException if the status change is forbidden
     */
    @Transactional
    public DeliveryResponseDTO updateStatus(Long id, String status, String role, String username, String reason) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        DeliveryStatus currentStatus = delivery.getStatus();
        DeliveryStatus targetStatus;
        try {
            targetStatus = DeliveryStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException("Invalid status: " + status + ". Allowed: " + java.util.Arrays.toString(DeliveryStatus.values()));
        }

        if (!canTransition(role, currentStatus, targetStatus, delivery, username)) {
            throw new InvalidStatusTransitionException(
                    "Invalid state transition from " + currentStatus + " to " + targetStatus + " for role " + role);
        }

        delivery.setStatus(targetStatus);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        eventPublisher.publishEvent(new com.smartcourier.delivery.event.DeliveryStatusChangedEvent(this, savedDelivery.getId(),
                savedDelivery.getTrackingNumber(), currentStatus, targetStatus, username));

        publishStatusEvent(savedDelivery, null, reason != null ? reason : "Status updated to: " + status);

        return mapToResponseDTO(savedDelivery);
    }



public List<DeliveryResponseDTO> getAllDeliveries() {
        return deliveryRepository.findAll()
                .stream().map(this::mapToResponseDTO).toList();
    }

    public List<DeliveryResponseDTO> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatus(status)
                .stream().map(this::mapToResponseDTO).toList();
    }

    public ServiceInfoDTO getServiceInfo() {
        List<ServiceItemDTO> services = Arrays.asList(
                ServiceItemDTO.builder().type("DOMESTIC").name("Domestic Courier")
                        .description("Standard delivery within the country").estimatedDays("3-5 business days")
                        .basePrice(5.99).build(),
                ServiceItemDTO.builder().type("EXPRESS").name("Express Delivery")
                        .description("Priority delivery with faster transit").estimatedDays("1-2 business days")
                        .basePrice(14.99).build(),
                ServiceItemDTO.builder().type("INTERNATIONAL").name("International Shipping")
                        .description("Worldwide delivery with tracking").estimatedDays("7-14 business days")
                        .basePrice(29.99).build());

        return ServiceInfoDTO.builder()
                .services(services)
                .company("SmartCourier")
                .tagline("Fast, reliable, and smart delivery solutions")
                .build();
    }

    public java.util.Map<String, Long> getStatusDistribution() {
        return java.util.Arrays.stream(DeliveryStatus.values())
                .collect(java.util.stream.Collectors.toMap(
                        status -> status.name(),
                        deliveryRepository::countByStatus
                ));
    }

    /**
     * Creates sample deliveries for testing purposes
     */
    public void createSampleDeliveries(String username) {
        String[] statuses = {"DRAFT", "BOOKED", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "FAILED"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix"};
        
        for (int i = 0; i < 6; i++) {
            Address sender = Address.builder()
                    .fullName("Sender " + i)
                    .phone("98765432" + i + i)
                    .street("Street " + i)
                    .city(cities[i % cities.length])
                    .state("State" + i)
                    .zipCode("123456")
                    .country("USA")
                    .build();
                    
            Address receiver = Address.builder()
                    .fullName("Receiver " + i)
                    .phone("98765432" + (i+1) + (i+1))
                    .street("Street " + (i+1))
                    .city(cities[(i+1) % cities.length])
                    .state("State" + (i+1))
                    .zipCode("654321")
                    .country("USA")
                    .build();
                    
            ParcelPackage pkg = ParcelPackage.builder()
                    .weight(2.5 + i)
                    .length(10.0)
                    .width(10.0)
                    .height(10.0)
                    .description("Test Package " + i)
                    .serviceType(ServiceType.DOMESTIC)
                    .declaredValue(100.0 + (i * 50))
                    .fragile(i % 2 == 0)
                    .build();
                    
            Delivery delivery = Delivery.builder()
                    .trackingNumber(generateTrackingNumber())
                    .username(username)
                    .senderAddress(sender)
                    .receiverAddress(receiver)
                    .parcelPackage(pkg)
                    .status(DeliveryStatus.valueOf(statuses[i]))
                    .charge(250.0)
                    .specialInstructions("Test delivery " + i)
                    .paid(i % 2 == 0)
                    .build();
                    
            deliveryRepository.save(delivery);
        }
        log.info("Created 6 sample deliveries for user: {}", username);
    }

    /**
     * Initializes a new delivery draft for the wizard flow.
     */
    @Transactional
    public DeliveryResponseDTO initDraft(String username) {
        Delivery delivery = Delivery.builder()
                .trackingNumber(generateTrackingNumber())
                .username(username)
                .status(DeliveryStatus.DRAFT)
                .build();
        Delivery saved = deliveryRepository.save(delivery);
        // History is now maintained exclusively by Tracking Service
        return mapToResponseDTO(saved);
    }

    @Transactional
    public DeliveryResponseDTO updateSender(Long id, AddressDTO addressDTO, String username) {
        log.info("Updating sender for delivery: {} by user: {}", id, username);
        Delivery delivery = getAndValidateDraft(id, username);
        if (delivery.getSenderAddress() != null) {
            updateAddressEntity(delivery.getSenderAddress(), addressDTO);
        } else {
            delivery.setSenderAddress(mapToAddressEntity(addressDTO));
        }
        return mapToResponseDTO(deliveryRepository.save(delivery));
    }

    @Transactional
    public DeliveryResponseDTO updateReceiver(Long id, AddressDTO addressDTO, String username) {
        log.info("Updating receiver for delivery: {} by user: {}", id, username);
        Delivery delivery = getAndValidateDraft(id, username);
        if (delivery.getReceiverAddress() != null) {
            updateAddressEntity(delivery.getReceiverAddress(), addressDTO);
        } else {
            delivery.setReceiverAddress(mapToAddressEntity(addressDTO));
        }
        return mapToResponseDTO(deliveryRepository.save(delivery));
    }

    @Transactional
    public DeliveryResponseDTO updatePackage(Long id, PackageDTO packageDTO, String username) {
        log.info("Updating package for delivery: {} by user: {}", id, username);
        Delivery delivery = getAndValidateDraft(id, username);
        if (delivery.getParcelPackage() != null) {
            updateParcelPackageEntity(delivery.getParcelPackage(), packageDTO);
            delivery.setCharge(calculateCharge(delivery.getParcelPackage()));
        } else {
            ParcelPackage parcel = mapToParcelPackageEntity(packageDTO);
            delivery.setParcelPackage(parcel);
            delivery.setCharge(calculateCharge(parcel));
        }
        return mapToResponseDTO(deliveryRepository.save(delivery));
    }

    /**
     * Transitions a draft delivery to BOOKED status after validating all wizard
     * steps are complete.
     */
    @Transactional
    public DeliveryResponseDTO finalizeDelivery(Long id, String username) {
        Delivery delivery = getAndValidateDraft(id, username);
        if (delivery.getSenderAddress() == null || delivery.getReceiverAddress() == null
                || delivery.getParcelPackage() == null) {
            throw new BusinessRuleViolationException("Cannot finalize delivery. All steps must be completed.");
        }

        DeliveryStatus oldStatus = delivery.getStatus();
        delivery.setStatus(DeliveryStatus.BOOKED);
        Delivery saved = deliveryRepository.save(delivery);

        // --- 🚀 Async Communication: RabbitMQ ---
        publishStatusEvent(saved, null, "Delivery confirmed via wizard");

        // Publish event for notifications/other services
        eventPublisher.publishEvent(new com.smartcourier.delivery.event.DeliveryStatusChangedEvent(this, saved.getId(),
                saved.getTrackingNumber(), oldStatus, DeliveryStatus.BOOKED, username));

        return mapToResponseDTO(saved);
    }

    private Delivery getAndValidateDraft(Long id, String username) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        if (!delivery.getUsername().equals(username)) {
            throw new UnauthorizedAccessException("Access denied: Not your delivery");
        }
        if (delivery.getStatus() != DeliveryStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT deliveries can be modified via wizard steps");
        }
        return delivery;
    }

    private DeliveryResponseDTO mapToResponseDTO(Delivery delivery) {
        return DeliveryResponseDTO.builder()
                .id(delivery.getId())
                .trackingNumber(delivery.getTrackingNumber())
                .username(delivery.getUsername())
                .senderAddress(delivery.getSenderAddress() == null ? null
                        : AddressDTO.builder()
                                .fullName(delivery.getSenderAddress().getFullName())
                                .phone(delivery.getSenderAddress().getPhone())
                                .street(delivery.getSenderAddress().getStreet())
                                .city(delivery.getSenderAddress().getCity())
                                .state(delivery.getSenderAddress().getState())
                                .zipCode(delivery.getSenderAddress().getZipCode())
                                .country(delivery.getSenderAddress().getCountry())
                                .build())
                .receiverAddress(delivery.getReceiverAddress() == null ? null
                        : AddressDTO.builder()
                                .fullName(delivery.getReceiverAddress().getFullName())
                                .phone(delivery.getReceiverAddress().getPhone())
                                .street(delivery.getReceiverAddress().getStreet())
                                .city(delivery.getReceiverAddress().getCity())
                                .state(delivery.getReceiverAddress().getState())
                                .zipCode(delivery.getReceiverAddress().getZipCode())
                                .country(delivery.getReceiverAddress().getCountry())
                                .build())
                .packageDetails(delivery.getParcelPackage() == null ? null
                        : PackageDTO.builder()
                                .weight(delivery.getParcelPackage().getWeight())
                                .length(delivery.getParcelPackage().getLength())
                                .width(delivery.getParcelPackage().getWidth())
                                .height(delivery.getParcelPackage().getHeight())
                                .description(delivery.getParcelPackage().getDescription())
                                .serviceType(delivery.getParcelPackage().getServiceType().name())
                                .declaredValue(delivery.getParcelPackage().getDeclaredValue())
                                .fragile(delivery.getParcelPackage().getFragile())
                                .build())
                .status(delivery.getStatus())
                .charge(delivery.getCharge())
                .specialInstructions(delivery.getSpecialInstructions())
                .paid(delivery.isPaid())
                .scheduledPickup(delivery.getScheduledPickup())
                .createdAt(delivery.getCreatedAt())
                .build();
    }



    private boolean canTransition(String role, DeliveryStatus from, DeliveryStatus to, Delivery delivery,
            String username) {
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            // Customers can only map Draft -> Booked for their own deliveries
            if (!delivery.getUsername().equals(username)) {
                throw new UnauthorizedAccessException("Access denied: You do not own this delivery.");
            }
            return from == DeliveryStatus.DRAFT && to == DeliveryStatus.BOOKED;
        }

        if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role)) {
            return ALLOWED_TRANSITIONS.getOrDefault(from, List.of()).contains(to);
        }

        return false;
    }

    private Double calculateCharge(ParcelPackage parcel) {
        double base;
        switch (parcel.getServiceType()) {
            case EXPRESS:
                base = 14.99;
                break;
            case INTERNATIONAL:
                base = 29.99;
                break;
            default:
                base = 5.99;
                break;
        }
        double weightCharge = parcel.getWeight() * 0.5;
        double fragileCharge = Boolean.TRUE.equals(parcel.getFragile()) ? 3.0 : 0;
        return Math.round((base + weightCharge + fragileCharge) * 100.0) / 100.0;
    }

    private String generateTrackingNumber() {
        return "SC" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

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
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "delivery.status", event);
        } catch (Exception e) {
            log.warn("RabbitMQ failed, falling back to synchronous Feign call for delivery {}", delivery.getId());
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
        trackingClient.addTrackingEvent(request);
    }

    private Address mapToAddressEntity(AddressDTO dto) {
        if (dto == null)
            return null;
        return Address.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .street(dto.getStreet())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry())
                .build();
    }

    private void updateAddressEntity(Address address, AddressDTO dto) {
        address.setFullName(dto.getFullName());
        address.setPhone(dto.getPhone());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());
    }

    private void updateParcelPackageEntity(ParcelPackage pkg, PackageDTO dto) {
        ServiceType type;
        try {
            type = ServiceType.valueOf(dto.getServiceType().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BusinessRuleViolationException("Invalid service type: " + dto.getServiceType());
        }
        pkg.setWeight(dto.getWeight() != null ? dto.getWeight() : 0.0);
        pkg.setLength(dto.getLength());
        pkg.setWidth(dto.getWidth());
        pkg.setHeight(dto.getHeight());
        pkg.setDescription(dto.getDescription());
        pkg.setServiceType(type);
        pkg.setDeclaredValue(dto.getDeclaredValue());
        pkg.setFragile(dto.getFragile());
    }

    private ParcelPackage mapToParcelPackageEntity(PackageDTO dto) {
        if (dto == null)
            return null;
        ServiceType type;
        try {
            type = ServiceType.valueOf(dto.getServiceType().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.error("Invalid service type received: {}", dto.getServiceType());
            throw new BusinessRuleViolationException("Invalid service type: " + dto.getServiceType() + ". Allowed values are DOMESTIC, EXPRESS, INTERNATIONAL.");
        }

        return ParcelPackage.builder()
                .weight(dto.getWeight() != null ? dto.getWeight() : 0.0)
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .description(dto.getDescription())
                .serviceType(type)
                .declaredValue(dto.getDeclaredValue())
                .fragile(dto.getFragile())
                .build();
    }
}
