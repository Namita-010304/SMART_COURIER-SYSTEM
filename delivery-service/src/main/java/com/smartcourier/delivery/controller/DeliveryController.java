package com.smartcourier.delivery.controller;

import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    public DeliveryController(DeliveryService deliveryService) {

        this.deliveryService = deliveryService;
    }
    @PostMapping
    public ResponseEntity<DeliveryResponseDTO> createDelivery(@Valid @RequestBody DeliveryRequest request,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.createDelivery(request, username, role));
    }
    @GetMapping("/my")
    public ResponseEntity<List<DeliveryResponseDTO>> getMyDeliveries(
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(id)); 
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(deliveryService.getDeliveryByTrackingNumber(trackingNumber));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DeliveryResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Username", defaultValue = "system") String username,
            @RequestHeader(value = "X-User-Role", defaultValue = "SYSTEM") String role,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, status, role, username, reason));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<DeliveryResponseDTO> bookDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "BOOKED", role, username, "Customer confirmed booking"));
    }

    @PostMapping("/{id}/pickup")
    public ResponseEntity<DeliveryResponseDTO> pickupDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "PICKED_UP", role, username, "Agent picked up package"));
    }

    @PostMapping("/{id}/transit")
    public ResponseEntity<DeliveryResponseDTO> transitDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "IN_TRANSIT", role, username, "Package in transit"));
    }

    @PostMapping("/{id}/out-for-delivery")
    public ResponseEntity<DeliveryResponseDTO> outForDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "OUT_FOR_DELIVERY", role, username, "Package out for delivery"));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<DeliveryResponseDTO> deliverDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "DELIVERED", role, username, "Package delivered successfully"));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<DeliveryResponseDTO> failDelivery(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestHeader("X-User-Username") String username,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, "FAILED", role, username, reason));
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponseDTO>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    // ---  Wizard Steps ---
    @PostMapping("/draft")
    public ResponseEntity<DeliveryResponseDTO> initDraft(@RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.initDraft(username));
    }

    @PutMapping("/{id}/sender")
    public ResponseEntity<DeliveryResponseDTO> updateSender(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO address,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.updateSender(id, address, username));
    }

    @PutMapping("/{id}/receiver")
    public ResponseEntity<DeliveryResponseDTO> updateReceiver(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO address,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.updateReceiver(id, address, username));
    }

    @PutMapping("/{id}/package")
    public ResponseEntity<DeliveryResponseDTO> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageDTO packageDetails,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.updatePackage(id, packageDetails, username));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<DeliveryResponseDTO> finalizeDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String username) {
        return ResponseEntity.ok(deliveryService.finalizeDelivery(id, username));
    }

    @GetMapping("/stats/distribution")
    public ResponseEntity<java.util.Map<String, Long>> getStatusDistribution() {
        return ResponseEntity.ok(deliveryService.getStatusDistribution());
    }

    // --- (ADMIN only) ---
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/test/create-samples")
    public ResponseEntity<String> createSampleDeliveries(
            @RequestHeader("X-User-Username") String username) {
        deliveryService.createSampleDeliveries(username);
        return ResponseEntity.ok("Sample deliveries created for user: " + username);
    }
}
