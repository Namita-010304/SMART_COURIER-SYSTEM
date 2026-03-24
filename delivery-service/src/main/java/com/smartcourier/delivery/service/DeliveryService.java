package com.smartcourier.delivery.service;

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

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public Delivery createDelivery(DeliveryRequest request, String username) {
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

        return deliveryRepository.save(delivery);
    }

    public List<Delivery> getMyDeliveries(String username) {
        return deliveryRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with id: " + id));
    }

    public Delivery getDeliveryByTrackingNumber(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Delivery not found with tracking number: " + trackingNumber));
    }

    @Transactional
    public Delivery updateStatus(Long id, String status) {
        Delivery delivery = getDeliveryById(id);
        delivery.setStatus(DeliveryStatus.valueOf(status));
        return deliveryRepository.save(delivery);
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    public List<Delivery> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatus(status);
    }

    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();

        Map<String, Object> domestic = new HashMap<>();
        domestic.put("type", "DOMESTIC");
        domestic.put("name", "Domestic Courier");
        domestic.put("description", "Standard delivery within the country");
        domestic.put("estimatedDays", "3-5 business days");
        domestic.put("basePrice", 5.99);
        services.add(domestic);

        Map<String, Object> express = new HashMap<>();
        express.put("type", "EXPRESS");
        express.put("name", "Express Delivery");
        express.put("description", "Priority delivery with faster transit");
        express.put("estimatedDays", "1-2 business days");
        express.put("basePrice", 14.99);
        services.add(express);

        Map<String, Object> international = new HashMap<>();
        international.put("type", "INTERNATIONAL");
        international.put("name", "International Shipping");
        international.put("description", "Worldwide delivery with tracking");
        international.put("estimatedDays", "7-14 business days");
        international.put("basePrice", 29.99);
        services.add(international);

        info.put("services", services);
        info.put("company", "SmartCourier");
        info.put("tagline", "Fast, reliable, and smart delivery solutions");
        return info;
    }

    private Double calculateCharge(ParcelPackage parcel) {
        double base;
        switch (parcel.getServiceType()) {
            case EXPRESS: base = 14.99; break;
            case INTERNATIONAL: base = 29.99; break;
            default: base = 5.99; break;
        }
        double weightCharge = parcel.getWeight() * 0.5;
        double fragileCharge = Boolean.TRUE.equals(parcel.getFragile()) ? 3.0 : 0;
        return Math.round((base + weightCharge + fragileCharge) * 100.0) / 100.0;
    }

    private String generateTrackingNumber() {
        return "SC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
