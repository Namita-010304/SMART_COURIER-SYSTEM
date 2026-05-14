package com.smartcourier.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {

    @GetMapping("/deliveries")
    List<Object> getAllDeliveries();

    @GetMapping("/deliveries/{id}")
    Map<String, Object> getDeliveryById(@PathVariable("id") Long id);

    @PutMapping("/deliveries/{id}/status") 
    Object updateStatus(@PathVariable("id") Long id, 
                        @RequestParam("status") String status,
                        @RequestHeader("X-User-Username") String username,
                        @RequestHeader("X-User-Role") String role,
                        @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/deliveries/stats/distribution")
    Map<String, Long> getStatusDistribution();
}
