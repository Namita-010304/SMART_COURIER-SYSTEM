package com.smartcourier.tracking.controller;

import com.smartcourier.tracking.entity.*;
import com.smartcourier.tracking.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<Map<String, Object>> getTracking(@PathVariable("trackingNumber") String trackingNumber) {
        return ResponseEntity.ok(trackingService.getTrackingInfo(trackingNumber));
    }

    @PostMapping("/tracking/events")
    public ResponseEntity<TrackingEvent> addTrackingEvent(
            @RequestParam Long deliveryId,
            @RequestParam String trackingNumber,
            @RequestParam String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(trackingService.addTrackingEvent(
                deliveryId, trackingNumber, status, location, description));
    }

    @PostMapping("/tracking/documents/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("deliveryId") Long deliveryId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(trackingService.uploadDocument(deliveryId, file));
    }

    @GetMapping("/tracking/documents/{deliveryId}")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable("deliveryId") Long deliveryId) {
        return ResponseEntity.ok(trackingService.getDocuments(deliveryId));
    }

    @PostMapping("/tracking/{deliveryId}/proof")
    public ResponseEntity<DeliveryProof> addDeliveryProof(
            @PathVariable Long deliveryId,
            @RequestParam String recipientName,
            @RequestParam(required = false) String signatureUrl,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(trackingService.addDeliveryProof(
                deliveryId, recipientName, signatureUrl, photoUrl, notes));
    }

    @GetMapping("/tracking/{deliveryId}/proof")
    public ResponseEntity<DeliveryProof> getDeliveryProof(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(trackingService.getDeliveryProof(deliveryId));
    }
}
