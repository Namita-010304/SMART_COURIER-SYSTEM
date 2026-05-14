package com.smartcourier.delivery.event;

import com.smartcourier.delivery.entity.DeliveryStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryStatusChangedEvent extends ApplicationEvent {
    private final Long deliveryId;
    private final String trackingNumber;
    private final DeliveryStatus oldStatus;
    private final DeliveryStatus newStatus;
    private final String changedBy;

    public DeliveryStatusChangedEvent(Object source, Long deliveryId, String trackingNumber, 
                                     DeliveryStatus oldStatus, DeliveryStatus newStatus, String changedBy) {
        super(source);
        this.deliveryId = deliveryId;
        this.trackingNumber = trackingNumber;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }
}
