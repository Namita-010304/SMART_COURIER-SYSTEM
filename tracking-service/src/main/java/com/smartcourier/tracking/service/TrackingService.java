package com.smartcourier.tracking.service;

import com.smartcourier.tracking.dto.TrackingResponseDTO;
import com.smartcourier.tracking.entity.*;
import com.smartcourier.tracking.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TrackingService {

    private final TrackingEventRepository trackingEventRepository;
    private final DocumentRepository documentRepository;
    private final DeliveryProofRepository deliveryProofRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public TrackingService(TrackingEventRepository trackingEventRepository,DocumentRepository documentRepository,DeliveryProofRepository deliveryProofRepository) {
        this.trackingEventRepository = trackingEventRepository;
        this.documentRepository = documentRepository;
        this.deliveryProofRepository = deliveryProofRepository;
    }

    public TrackingEvent addTrackingEvent(Long deliveryId, String trackingNumber, String status, String location, String description) {
        TrackingEvent event = TrackingEvent.builder()
                .deliveryId(deliveryId)
                .trackingNumber(trackingNumber)
                .status(status)
                .location(location)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();
        return trackingEventRepository.save(event);
    }

    public List<TrackingEvent> getTrackingEvents(String trackingNumber) {
        return trackingEventRepository.findByTrackingNumberOrderByTimestampDesc(trackingNumber);
    }

    public TrackingResponseDTO getTrackingInfo(String trackingNumber) {
        List<TrackingEvent> events = getTrackingEvents(trackingNumber);
        
        TrackingResponseDTO.TrackingResponseDTOBuilder builder = TrackingResponseDTO.builder()
                .trackingNumber(trackingNumber)
                .events(events);
                
        if (!events.isEmpty()) {
            builder.currentStatus(events.get(0).getStatus())
                   .lastUpdated(events.get(0).getTimestamp());
        }
        
        return builder.build();
    }

    public Document uploadDocument(Long deliveryId, MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document document = Document.builder()
                .deliveryId(deliveryId)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .build();

        return documentRepository.save(document);
    }

    public List<Document> getDocuments(Long deliveryId) {
        return documentRepository.findByDeliveryId(deliveryId);
    }

    public DeliveryProof addDeliveryProof(Long deliveryId, String recipientName,
                                          String signatureUrl, String photoUrl, String notes) {
        DeliveryProof proof = DeliveryProof.builder()
                .deliveryId(deliveryId)
                .recipientName(recipientName)
                .signatureUrl(signatureUrl)
                .photoUrl(photoUrl)
                .notes(notes)
                .deliveredAt(LocalDateTime.now())
                .build();
        return deliveryProofRepository.save(proof);
    }

    public DeliveryProof getDeliveryProof(Long deliveryId) {
        return deliveryProofRepository.findByDeliveryId(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery proof not found for delivery: " + deliveryId));
    }
}
