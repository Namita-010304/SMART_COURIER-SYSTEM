package com.smartcourier.admin.service;

import com.smartcourier.admin.client.DeliveryClient;
import com.smartcourier.admin.client.TrackingClient;
import com.smartcourier.admin.dto.AdminUserCreateRequest;
import com.smartcourier.admin.dto.AdminUserUpdateRequest;
import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.repository.HubRepository;
import com.smartcourier.admin.repository.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;

@Slf4j
@Service
public class AdminService {

    private final HubRepository hubRepository;
    private final ReportRepository reportRepository;
    private final DeliveryClient deliveryClient;
    private final TrackingClient trackingClient;
    private final com.smartcourier.admin.client.AuthClient authClient;

    public AdminService(HubRepository hubRepository, ReportRepository reportRepository,
                        DeliveryClient deliveryClient, TrackingClient trackingClient,
                        com.smartcourier.admin.client.AuthClient authClient) {
        this.hubRepository = hubRepository;
        this.reportRepository = reportRepository;
        this.deliveryClient = deliveryClient;
        this.trackingClient = trackingClient;
        this.authClient = authClient;
    }

    // ========== Dashboard ==========
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        // Get status distribution first — use it for totalDeliveries too
        Map<String, Long> dist = new HashMap<>();
        try {
            dist = deliveryClient.getStatusDistribution();
        } catch (Exception e) {
            log.error("Failed to fetch status distribution: {}", e.getMessage());
            // Return partial dashboard rather than silently zeroing out counts
            dashboard.put("statusDistribution", dist);
            dashboard.put("totalDeliveries", -1L);
            dashboard.put("totalHubs", hubRepository.count());
            dashboard.put("activeHubs", hubRepository.findByActive(true).size());
            dashboard.put("totalReports", reportRepository.count());
            dashboard.put("timestamp", LocalDateTime.now());
            return dashboard;
        }
        dashboard.put("statusDistribution", dist);

        // Sum all statuses — DeliveryService guarantees every DeliveryStatus key is present
        long totalDeliveries = dist.values().stream().mapToLong(Long::longValue).sum();
        dashboard.put("totalDeliveries", totalDeliveries);

        dashboard.put("totalHubs", hubRepository.count());
        dashboard.put("activeHubs", hubRepository.findByActive(true).size());
        dashboard.put("totalReports", reportRepository.count());
        dashboard.put("timestamp", LocalDateTime.now());

        // Mock performance metrics
        Map<String, Object> performance = new HashMap<>();
        performance.put("avgDeliveryTime", "2.5 days");
        performance.put("onTimeDeliveryRate", "94.5%");
        performance.put("customerSatisfaction", "4.7/5");
        dashboard.put("performance", performance);

        return dashboard;
    }

    // ========== Deliveries (via Delivery Service) ==========
    public List<Object> getAllDeliveries() {
        return deliveryClient.getAllDeliveries();
    }

    public Object getDeliveryById(Long id) {
        return deliveryClient.getDeliveryById(id);
    }

    public Object resolveDeliveryException(Long deliveryId, String resolution, String username, String role) {
        // Update delivery status — resolution must be a valid DeliveryStatus value
        String statusValue = resolution.toUpperCase(java.util.Locale.ROOT).trim();
        deliveryClient.updateStatus(deliveryId, statusValue, username, role, "Admin resolved exception: " + statusValue);
        
        // Record tracking event
        try {
            // Get tracking number first
            Map<String, Object> delivery = deliveryClient.getDeliveryById(deliveryId);
            if (delivery != null) {
                String trackingNumber = (String) delivery.get("trackingNumber");
                if (trackingNumber != null) {
                    Map<String, Object> trackingRequest = new java.util.HashMap<>();
                    trackingRequest.put("deliveryId", deliveryId);
                    trackingRequest.put("trackingNumber", trackingNumber);
                    trackingRequest.put("status", statusValue);
                    trackingRequest.put("location", "Admin Hub");
                    trackingRequest.put("description", "Exception resolved by administrator");
                    trackingClient.addTrackingEvent(trackingRequest);
                }
            }
        } catch (Exception te) {
            log.error("Failed to add tracking event during resolution: {}", te.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deliveryId", deliveryId);
        result.put("resolution", resolution);
        result.put("resolvedAt", LocalDateTime.now());
        result.put("message", "Delivery exception resolved successfully");
        return result;
    }

    // ========== Hubs ==========
    public List<Hub> getAllHubs() {
        return hubRepository.findAll();
    }

    public Hub getHubById(Long id) {
        return hubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hub not found with id: " + id));
    }

    public Hub createHub(Hub hub) {
        return hubRepository.save(hub);
    }

    public Hub updateHub(Long id, Hub hubDetails) {
        Hub hub = getHubById(id);
        hub.setName(hubDetails.getName());
        hub.setCode(hubDetails.getCode());
        hub.setCity(hubDetails.getCity());
        hub.setState(hubDetails.getState());
        hub.setAddress(hubDetails.getAddress());
        hub.setActive(hubDetails.getActive());
        hub.setContactPhone(hubDetails.getContactPhone());
        hub.setContactEmail(hubDetails.getContactEmail());
        return hubRepository.save(hub);
    }

    public void deleteHub(Long id) {
        hubRepository.deleteById(id);
    }

    // ========== Users (via Auth Service) ==========
    public List<Object> getAllUsers() {
        return authClient.getAllUsers("ADMIN");
    }

    public Object createUser(AdminUserCreateRequest request) {
        return authClient.createUser(request, "ADMIN");
    }

    public Object updateUser(Long id, AdminUserUpdateRequest request) {
        return authClient.updateUser(id, request, "ADMIN");
    }

    public void deleteUser(Long id) {
        authClient.deleteUser(id, "ADMIN");
    }

    // ========== Reports ==========
    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Report generateReport(String type, String title, String generatedBy) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("type", type);
        reportData.put("generatedAt", LocalDateTime.now());

        switch (type.toUpperCase(Locale.ROOT)) {
            case "DELIVERY_SUMMARY":
                try {
                    Map<String, Long> stats = deliveryClient.getStatusDistribution();
                    reportData.putAll(stats);
                    reportData.put("total", stats.values().stream().mapToLong(Long::longValue).sum());
                } catch (Exception e) {
                    reportData.put("error", "Failed to fetch real delivery stats");
                }
                break;
            case "PERFORMANCE":
                reportData.put("avgDeliveryTime", "2.5 days");
                reportData.put("onTimeRate", "94.5%");
                reportData.put("customerRating", "4.7/5");
                reportData.put("returnRate", "1.2%");
                break;
            case "HUB_UTILIZATION":
                reportData.put("totalHubs", hubRepository.count());
                reportData.put("activeHubs", hubRepository.findByActive(true).size());
                break;
            default:
                reportData.put("info", "General report");
        }

        Report report = Report.builder()
                .type(type)
                .title(title)
                .data(reportData.toString())
                .generatedBy(generatedBy)
                .build();

        return reportRepository.save(report);
    }
}
