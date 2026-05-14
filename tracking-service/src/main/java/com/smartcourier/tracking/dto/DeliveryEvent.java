package com.smartcourier.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEvent {
    private Long deliveryId;
    private String trackingNumber;
    private String status;
    private String location;
    private String message;
    private LocalDateTime timestamp;
}
