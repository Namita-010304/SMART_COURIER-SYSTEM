package com.smartcourier.delivery.dto;

import com.smartcourier.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDTO {
    private Long id;
    private String trackingNumber;
    private String username;
    private AddressDTO senderAddress;
    private AddressDTO receiverAddress;
    private PackageDTO packageDetails;
    private DeliveryStatus status;
    private Double charge;
    private String specialInstructions;
    private LocalDateTime scheduledPickup;
    private LocalDateTime createdAt;
}
