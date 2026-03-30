package com.smartcourier.tracking.service;

import com.smartcourier.tracking.entity.*;
import com.smartcourier.tracking.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DeliveryProofRepository deliveryProofRepository;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    void addTrackingEvent_Success() {
        TrackingEvent event = TrackingEvent.builder()
                .id(1L)
                .deliveryId(1L)
                .trackingNumber("SC123")
                .status("IN_TRANSIT")
                .location("New York Hub")
                .description("Package arrived at hub")
                .timestamp(LocalDateTime.now())
                .build();

        when(trackingEventRepository.save(any(TrackingEvent.class))).thenReturn(event);

        TrackingEvent result = trackingService.addTrackingEvent(1L, "SC123", "IN_TRANSIT", "New York Hub",
                "Package arrived at hub");

        assertNotNull(result);
        assertEquals("IN_TRANSIT", result.getStatus());
        verify(trackingEventRepository).save(any(TrackingEvent.class));
    }

    @Test
    void getTrackingEvents_ReturnsSortedEvents() {
        List<TrackingEvent> events = Arrays.asList(
                TrackingEvent.builder().id(1L).trackingNumber("SC123").status("BOOKED").build(),
                TrackingEvent.builder().id(2L).trackingNumber("SC123").status("IN_TRANSIT").build());

        when(trackingEventRepository.findByTrackingNumberOrderByTimestampDesc("SC123")).thenReturn(events);

        List<TrackingEvent> result = trackingService.getTrackingEvents("SC123");

        assertEquals(2, result.size());
    }

    @Test
    void getTrackingInfo_ReturnsTracking() {
        List<TrackingEvent> events = Arrays.asList(
                TrackingEvent.builder().id(1L).trackingNumber("SC123").status("IN_TRANSIT")
                        .timestamp(LocalDateTime.now()).build());

        when(trackingEventRepository.findByTrackingNumberOrderByTimestampDesc("SC123")).thenReturn(events);

        var info = trackingService.getTrackingInfo("SC123");

        assertNotNull(info);
        assertEquals("SC123", info.getTrackingNumber());
        assertEquals("IN_TRANSIT", info.getCurrentStatus());
    }

    @Test
    void getDocuments_ReturnsDocumentList() {
        List<Document> docs = Arrays.asList(
                Document.builder().id(1L).deliveryId(1L).fileName("invoice.pdf").build());

        when(documentRepository.findByDeliveryId(1L)).thenReturn(docs);

        List<Document> result = trackingService.getDocuments(1L);

        assertEquals(1, result.size());
        assertEquals("invoice.pdf", result.get(0).getFileName());
    }

    @Test
    void addDeliveryProof_Success() {
        DeliveryProof proof = DeliveryProof.builder()
                .id(1L)
                .deliveryId(1L)
                .recipientName("Jane Doe")
                .deliveredAt(LocalDateTime.now())
                .build();

        when(deliveryProofRepository.save(any(DeliveryProof.class))).thenReturn(proof);

        DeliveryProof result = trackingService.addDeliveryProof(1L, "Jane Doe", null, null, "Left at door");

        assertNotNull(result);
        assertEquals("Jane Doe", result.getRecipientName());
    }

    @Test
    void getDeliveryProof_NotFound_ThrowsException() {
        when(deliveryProofRepository.findByDeliveryId(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> trackingService.getDeliveryProof(99L));
    }
}
