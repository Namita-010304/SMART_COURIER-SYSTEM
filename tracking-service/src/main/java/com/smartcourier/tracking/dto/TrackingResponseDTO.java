package com.smartcourier.tracking.dto;

import com.smartcourier.tracking.entity.TrackingEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponseDTO {
    private String trackingNumber;
    private String currentStatus;
    private LocalDateTime lastUpdated;
    private List<TrackingEvent> events;
}
