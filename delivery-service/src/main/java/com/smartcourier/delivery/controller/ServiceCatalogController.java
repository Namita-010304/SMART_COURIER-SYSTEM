package com.smartcourier.delivery.controller;

import com.smartcourier.delivery.dto.ServiceInfoDTO;
import com.smartcourier.delivery.service.DeliveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/services")
public class ServiceCatalogController {

    private final DeliveryService deliveryService;

    public ServiceCatalogController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<Map<String, Object>> getServices() {
        return List.of(
            Map.of("name", "Domestic",
                    "description","Same-day and next-day delivery across 500+ cities with real-time route optimization.",
                    "icon", "bi-geo-alt", "features", List.of("24h Express Local", "Door-to-door service"),
                "featured", false),
            Map.of("name", "Express", "description",
                "High-priority shipping for time-sensitive documents and high-value fragile assets.",
                "icon", "bi-lightning-charge", "features", List.of("4-Hour Urban Delivery", "Priority Air Cargo"),
                "featured", true),
            Map.of("name", "International", "description",
                "Seamless border crossing with managed customs and global tracking visibility.",
                "icon", "bi-globe-americas", "features", List.of("220+ Countries", "Customs Assistance"),
                "featured", false)
        );
    }
}
