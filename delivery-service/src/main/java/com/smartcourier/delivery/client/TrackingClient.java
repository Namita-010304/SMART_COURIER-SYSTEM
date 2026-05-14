package com.smartcourier.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "tracking-service")
public interface TrackingClient {

    @PostMapping("/tracking/events")
    Object addTrackingEvent(@RequestBody Map<String, Object> request);
}
