package com.smartcourier.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class PackageDTO {

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private Double weight;

    private Double length;
    private Double width;
    private Double height;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Service type is required")
    private String serviceType;

    private Double declaredValue;
    private Boolean fragile;
}
