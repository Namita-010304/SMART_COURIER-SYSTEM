package com.smartcourier.delivery.service;

import com.smartcourier.delivery.client.TrackingClient;
import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.entity.*;
import com.smartcourier.delivery.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final TrackingClient trackingClient;

    public DeliveryService(DeliveryRepository deliveryRepository, TrackingClient trackingClient) {
        this.deliveryRepository = deliveryRepository;
        this.trackingClient = trackingClient;
    }

    @Transactional
    public DeliveryResponseDTO createDelivery(DeliveryRequest request, String username) {
        Address sender = Address.builder()
                .fullName(request.getSenderAddress().getFullName())
                .phone(request.getSenderAddress().getPhone())
                .street(request.getSenderAddress().getStreet())
                .city(request.getSenderAddress().getCity())
                .state(request.getSenderAddress().getState())
                .zipCode(request.getSenderAddress().getZipCode())
                .country(request.getSenderAddress().getCountry())
                .build();

        Address receiver = Address.builder()
                .fullName(request.getReceiverAddress().getFullName())
                .phone(request.getReceiverAddress().getPhone())
                .street(request.getReceiverAddress().getStreet())
                .city(request.getReceiverAddress().getCity())
                .state(request.getReceiverAddress().getState())
                .zipCode(request.getReceiverAddress().getZipCode())
                .country(request.getReceiverAddress().getCountry())
                .build();

        ParcelPackage parcel = ParcelPackage.builder()
                .weight(request.getPackageDetails().getWeight())
                .length(request.getPackageDetails().getLength())
                .width(request.getPackageDetails().getWidth())
                .height(request.getPackageDetails().getHeight())
                .description(request.getPackageDetails().getDescription())
                .serviceType(ServiceType.valueOf(request.getPackageDetails().getServiceType()))
                .declaredValue(request.getPackageDetails().getDeclaredValue())
                .fragile(request.getPackageDetails().getFragile())
                .build();

        Double charge = calculateCharge(parcel);

        Delivery delivery = Delivery.builder()
                .trackingNumber(generateTrackingNumber())
                .username(username)
                .senderAddress(sender)
                .receiverAddress(receiver)
                .parcelPackage(parcel)
                .status(DeliveryStatus.BOOKED)
                .charge(charge)
                .specialInstructions(request.getSpecialInstructions())
                .build();

        if (request.getScheduledPickup() != null && !request.getScheduledPickup().isEmpty()) {
            delivery.setScheduledPickup(LocalDateTime.parse(request.getScheduledPickup()));
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);
        
        try {
            trackingClient.addTrackingEvent(
                savedDelivery.getId(), 
                savedDelivery.getTrackingNumber(), 
                "BOOKED", 
                "Origin Terminal", 
                "Delivery order has been successfully booked."
            );
        } catch (Exception e) {
            System.err.println("Non-blocking error: Failed to initialize tracking: " + e.getMessage());
        }

        return mapToResponseDTO(savedDelivery);
    }

    public List<DeliveryResponseDTO> getMyDeliveries(String username) {
        return deliveryRepository.findByUsernameOrderByCreatedAtDesc(username)
                .stream().map(this::mapToResponseDTO).toList();
    }

    public DeliveryResponseDTO getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
    }

    public DeliveryResponseDTO getDeliveryByTrackingNumber(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new RuntimeException("Delivery not found with tracking number: " + trackingNumber));
    }

    @Transactional
    public DeliveryResponseDTO updateStatus(Long id, String status) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
        delivery.setStatus(DeliveryStatus.valueOf(status));
        Delivery savedDelivery = deliveryRepository.save(delivery);

        // --- 🚀 NEW: AUTO-LOG STATUS CHANGE ---
        try {
            trackingClient.addTrackingEvent(
                savedDelivery.getId(), 
                savedDelivery.getTrackingNumber(), 
                status, 
                "Hub", 
                "Status updated to: " + status
            );
        } catch (Exception e) {
            System.err.println("Non-blocking error: Failed to log tracking event: " + e.getMessage());
        }

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
            ServiceItemDTO.builder().type("DOMESTIC").name("Domestic Courier").description("Standard delivery within the country").estimatedDays("3-5 business days").basePrice(5.99).build(),
            ServiceItemDTO.builder().type("EXPRESS").name("Express Delivery").description("Priority delivery with faster transit").estimatedDays("1-2 business days").basePrice(14.99).build(),
            ServiceItemDTO.builder().type("INTERNATIONAL").name("International Shipping").description("Worldwide delivery with tracking").estimatedDays("7-14 business days").basePrice(29.99).build()
        );

        return ServiceInfoDTO.builder()
                .services(services)
                .company("SmartCourier")
                .tagline("Fast, reliable, and smart delivery solutions")
                .build();
    }

    private DeliveryResponseDTO mapToResponseDTO(Delivery delivery) {
        return DeliveryResponseDTO.builder()
                .id(delivery.getId())
                .trackingNumber(delivery.getTrackingNumber())
                .username(delivery.getUsername())
                .senderAddress(AddressDTO.builder()
                        .fullName(delivery.getSenderAddress().getFullName())
                        .phone(delivery.getSenderAddress().getPhone())
                        .street(delivery.getSenderAddress().getStreet())
                        .city(delivery.getSenderAddress().getCity())
                        .state(delivery.getSenderAddress().getState())
                        .zipCode(delivery.getSenderAddress().getZipCode())
                        .country(delivery.getSenderAddress().getCountry())
                        .build())
                .receiverAddress(AddressDTO.builder()
                        .fullName(delivery.getReceiverAddress().getFullName())
                        .phone(delivery.getReceiverAddress().getPhone())
                        .street(delivery.getReceiverAddress().getStreet())
                        .city(delivery.getReceiverAddress().getCity())
                        .state(delivery.getReceiverAddress().getState())
                        .zipCode(delivery.getReceiverAddress().getZipCode())
                        .country(delivery.getReceiverAddress().getCountry())
                        .build())
                .packageDetails(PackageDTO.builder()
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
                .scheduledPickup(delivery.getScheduledPickup())
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    private Double calculateCharge(ParcelPackage parcel) {
        double base;
        switch (parcel.getServiceType()) {
            case EXPRESS: base = 14.99; 
                break;
            case INTERNATIONAL: base = 29.99; 
                break;
            default: base = 5.99; 
                break;
        }
        double weightCharge = parcel.getWeight() * 0.5;
        double fragileCharge = Boolean.TRUE.equals(parcel.getFragile()) ? 3.0 : 0;
        return Math.round((base + weightCharge + fragileCharge) * 100.0) / 100.0;
    }

    private String generateTrackingNumber() {
        return "SC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
