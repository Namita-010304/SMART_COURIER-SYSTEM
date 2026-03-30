package com.smartcourier.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDTO {
    private String type;
    private String name;
    private String description;
    private String estimatedDays;
    private Double basePrice;
}
