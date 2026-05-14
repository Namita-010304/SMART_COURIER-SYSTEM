package com.smartcourier.tracking.listener;

import com.smartcourier.tracking.config.RabbitMQConfig;
import com.smartcourier.tracking.dto.DeliveryEvent;
import com.smartcourier.tracking.service.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrackingMessageListener {

    private final TrackingService trackingService;

    public TrackingMessageListener(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleDeliveryStatusUpdate(DeliveryEvent event) {
        log.info("Received async status update for delivery {}: {}", event.getDeliveryId(), event.getStatus());
        try {
            trackingService.addTrackingEvent(
                    event.getDeliveryId(),
                    event.getTrackingNumber(),
                    event.getStatus(),
                    event.getLocation(),
                    event.getMessage()
            );
        } catch (Exception e) {
            log.error("Failed to process tracking event for delivery {}: {}", event.getDeliveryId(), e.getMessage());
        }
    }
}
