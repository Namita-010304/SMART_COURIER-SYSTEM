package com.smartcourier.delivery.controller;

import com.smartcourier.delivery.dto.DeliveryRequest;
import com.smartcourier.delivery.dto.DeliveryResponseDTO;
import com.smartcourier.delivery.dto.ServiceInfoDTO;
import com.smartcourier.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200") 
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {

        this.deliveryService = deliveryService;
    }
    @GetMapping("/services")
    public ResponseEntity<ServiceInfoDTO> getServices() {

        return ResponseEntity.ok(deliveryService.getServiceInfo());
    }

    @PostMapping("/deliveries")
    public ResponseEntity<DeliveryResponseDTO> createDelivery(@Valid @RequestBody DeliveryRequest request,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.createDelivery(request, username));
    }

    @GetMapping("/deliveries/my")
    public ResponseEntity<List<DeliveryResponseDTO>> getMyDeliveries(
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(username));
    }

    @GetMapping("/deliveries/{id}")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id)); 
    }

    @GetMapping("/deliveries/track/{trackingNumber}")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(deliveryService.getDeliveryByTrackingNumber(trackingNumber));
    }

    @PutMapping("/deliveries/{id}/status")
    public ResponseEntity<DeliveryResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, status));
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryResponseDTO>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }
}
