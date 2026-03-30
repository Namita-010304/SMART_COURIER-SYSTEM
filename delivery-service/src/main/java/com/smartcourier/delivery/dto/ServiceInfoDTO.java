package com.smartcourier.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfoDTO {
    private List<ServiceItemDTO> services;
    private String company;
    private String tagline;
}
