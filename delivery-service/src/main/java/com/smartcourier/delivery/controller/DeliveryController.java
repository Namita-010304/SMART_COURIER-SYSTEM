package com.smartcourier.delivery.controller;

import com.smartcourier.delivery.dto.DeliveryRequest;
import com.smartcourier.delivery.entity.Delivery;
import com.smartcourier.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200") 
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getServices() {
        return ResponseEntity.ok(deliveryService.getServiceInfo());
    }

    @PostMapping("/deliveries")
    public ResponseEntity<Delivery> createDelivery(
            @Valid @RequestBody DeliveryRequest request,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.createDelivery(request, username));
    }

    @GetMapping("/deliveries/my")
    public ResponseEntity<List<Delivery>> getMyDeliveries(
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(username));
    }

    @GetMapping("/deliveries/{id}")
    public ResponseEntity<Delivery> getDeliveryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id)); 
    }

    @GetMapping("/deliveries/track/{trackingNumber}")
    public ResponseEntity<Delivery> getDeliveryByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(deliveryService.getDeliveryByTrackingNumber(trackingNumber));
    }

    @PutMapping("/deliveries/{id}/status")
    public ResponseEntity<Delivery> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, status));
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }
}
