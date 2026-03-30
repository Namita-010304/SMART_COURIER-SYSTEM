package com.smartcourier.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequest {

    @NotNull(message = "Sender address is required")
    @Valid
    private AddressDTO senderAddress;

    @NotNull(message = "Receiver address is required")
    @Valid
    private AddressDTO receiverAddress;

    @NotNull(message = "Package details are required")
    @Valid
    private PackageDTO packageDetails;

    private String scheduledPickup;
    private String specialInstructions;
}
