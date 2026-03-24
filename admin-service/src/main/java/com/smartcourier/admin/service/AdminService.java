package com.smartcourier.admin.service;

import com.smartcourier.admin.client.DeliveryClient;
import com.smartcourier.admin.client.TrackingClient;
import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.repository.HubRepository;
import com.smartcourier.admin.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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

        try {
            List<Object> deliveries = deliveryClient.getAllDeliveries();
            int totalDeliveries = deliveries != null ? deliveries.size() : 0;
            dashboard.put("totalDeliveries", totalDeliveries);
        } catch (Exception e) {
            dashboard.put("totalDeliveries", 0);
        }

        dashboard.put("totalHubs", hubRepository.count());
        dashboard.put("activeHubs", hubRepository.findByActive(true).size());
        dashboard.put("totalReports", reportRepository.count());
        dashboard.put("timestamp", LocalDateTime.now());

        // Mock status distribution
        Map<String, Integer> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("BOOKED", 12);
        statusDistribution.put("PICKED_UP", 8);
        statusDistribution.put("IN_TRANSIT", 15);
        statusDistribution.put("OUT_FOR_DELIVERY", 5);
        statusDistribution.put("DELIVERED", 45);
        statusDistribution.put("DELAYED", 3);
        statusDistribution.put("FAILED", 1);
        dashboard.put("statusDistribution", statusDistribution);

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
        try {
            return deliveryClient.getAllDeliveries();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Object resolveDeliveryException(Long deliveryId, String resolution) {
        try {
            // Update delivery status
            deliveryClient.updateStatus(deliveryId, resolution);
            
            // Record tracking event
            try {
                // Get tracking number first
                Map<String, Object> delivery = deliveryClient.getDeliveryById(deliveryId);
                if (delivery != null) {
                    String trackingNumber = (String) delivery.get("trackingNumber");
                    trackingClient.addTrackingEvent(deliveryId, trackingNumber, resolution, 
                        "Admin Hub", "Exception resolved by administrator");
                }
            } catch (Exception te) {
                // Log and continue if tracking fails
                System.err.println("Failed to add tracking event: " + te.getMessage());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("deliveryId", deliveryId);
            result.put("resolution", resolution);
            result.put("resolvedAt", LocalDateTime.now());
            result.put("message", "Delivery exception resolved successfully");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve delivery exception: " + e.getMessage());
        }
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
    @SuppressWarnings("unchecked")
    public List<Object> getAllUsers() {
        try {
            return authClient.getAllUsers();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // ========== Reports ==========
    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Report generateReport(String type, String title, String generatedBy) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("type", type);
        reportData.put("generatedAt", LocalDateTime.now());

        switch (type.toUpperCase()) {
            case "DELIVERY_SUMMARY":
                reportData.put("totalDeliveries", 89);
                reportData.put("completed", 45);
                reportData.put("inTransit", 15);
                reportData.put("pending", 20);
                reportData.put("failed", 1);
                reportData.put("delayed", 3);
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
                reportData.put("avgPackagesPerHub", 125);
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
