package com.smartcourier.tracking.controller;

import com.smartcourier.tracking.dto.AddTrackingEventRequest;
import com.smartcourier.tracking.dto.TrackingResponseDTO;
import com.smartcourier.tracking.entity.*;
import com.smartcourier.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<TrackingResponseDTO> getTracking(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(trackingService.getTrackingInfo(trackingNumber));
    }

    @PostMapping("/events")
    public ResponseEntity<TrackingEvent> addTrackingEvent(@Valid @RequestBody AddTrackingEventRequest request) {
        return ResponseEntity.ok(trackingService.addTrackingEvent(
                request.getDeliveryId(),
                request.getTrackingNumber(),
                request.getStatus(),
                request.getLocation(),
                request.getDescription()));
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("deliveryId") Long deliveryId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(trackingService.uploadDocument(deliveryId, file));
    }

    @GetMapping("/documents/{deliveryId}")
    public ResponseEntity<List<Document>> getDocuments(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(trackingService.getDocuments(deliveryId));
    }

    @GetMapping("/documents/{id}/view")
    public ResponseEntity<byte[]> viewDocument(@PathVariable Long id) throws IOException {
        Document doc = trackingService.getDocumentById(id);
        byte[] fileBytes = trackingService.getDocumentBytes(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, doc.getFileType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                .body(fileBytes);
    }

    @PostMapping("/{deliveryId}/proof")
    public ResponseEntity<DeliveryProof> addDeliveryProof(
            @PathVariable Long deliveryId,
            @RequestParam String recipientName,
            @RequestParam(required = false) String signatureUrl,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(trackingService.addDeliveryProof(
                deliveryId, recipientName, signatureUrl, photoUrl, notes));
    }

    @GetMapping("/{deliveryId}/proof")
    public ResponseEntity<DeliveryProof> getDeliveryProof(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(trackingService.getDeliveryProof(deliveryId));
    }
}
