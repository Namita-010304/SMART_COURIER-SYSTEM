package com.smartcourier.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "tracking-service")
public interface TrackingClient {

    @PostMapping("/tracking/events")
    Object addTrackingEvent(
            @RequestParam("deliveryId") Long deliveryId,
            @RequestParam("trackingNumber") String trackingNumber,
            @RequestParam("status") String status,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "description", required = false) String description);
}
