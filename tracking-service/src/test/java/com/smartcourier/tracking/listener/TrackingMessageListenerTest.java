package com.smartcourier.tracking.listener;

import com.smartcourier.tracking.dto.DeliveryEvent;
import com.smartcourier.tracking.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackingMessageListenerTest {

    @Mock
    private TrackingService trackingService;

    @InjectMocks
    private TrackingMessageListener trackingMessageListener;

    @Test
    void handleDeliveryStatusUpdate_Success() {
        DeliveryEvent event = DeliveryEvent.builder()
                .deliveryId(1L)
                .trackingNumber("SC123")
                .status("IN_TRANSIT")
                .message("Left hub")
                .build();

        trackingMessageListener.handleDeliveryStatusUpdate(event);

        verify(trackingService).addTrackingEvent(eq(1L), eq("SC123"), eq("IN_TRANSIT"), eq("Hub"), eq("Left hub"));
    }

    @Test
    void handleDeliveryStatusUpdate_Error_LogsError() {
        DeliveryEvent event = DeliveryEvent.builder().deliveryId(1L).build();
        doThrow(new RuntimeException("DB error")).when(trackingService).addTrackingEvent(any(), any(), any(), any(), any());

        // Should not throw exception as it's caught and logged
        trackingMessageListener.handleDeliveryStatusUpdate(event);

        verify(trackingService).addTrackingEvent(any(), any(), any(), any(), any());
    }
}
