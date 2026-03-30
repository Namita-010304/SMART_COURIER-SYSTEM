package com.smartcourier.admin.service;

import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    private com.smartcourier.admin.repository.HubRepository hubRepository;
    private com.smartcourier.admin.repository.ReportRepository reportRepository;
    private com.smartcourier.admin.client.DeliveryClient deliveryClient;
    private com.smartcourier.admin.client.TrackingClient trackingClient;
    private com.smartcourier.admin.client.AuthClient authClient;

    private AdminService adminService;

    private Hub testHub;

    @BeforeEach
    void setUp() {
        hubRepository = mock(com.smartcourier.admin.repository.HubRepository.class);
        reportRepository = mock(com.smartcourier.admin.repository.ReportRepository.class);
        deliveryClient = mock(com.smartcourier.admin.client.DeliveryClient.class);
        trackingClient = mock(com.smartcourier.admin.client.TrackingClient.class);
        authClient = mock(com.smartcourier.admin.client.AuthClient.class);

        adminService = new AdminService(hubRepository, reportRepository, deliveryClient, trackingClient, authClient);
        
        testHub = Hub.builder()
                .id(1L)
                .name("NYC Central Hub")
                .code("NYC-01")
                .city("New York")
                .state("NY")
                .address("100 Main St")
                .active(true)
                .build();
    }

    @Test
    void getDashboardData_ReturnsData() {
        when(hubRepository.count()).thenReturn(5L);
        when(hubRepository.findByActive(true)).thenReturn(Arrays.asList(testHub));
        when(reportRepository.count()).thenReturn(3L);
        when(deliveryClient.getAllDeliveries()).thenReturn(Arrays.asList(new Object(), new Object()));

        var dashboard = adminService.getDashboardData();

        assertNotNull(dashboard);
        assertEquals(2, dashboard.get("totalDeliveries"));
        assertEquals(5L, dashboard.get("totalHubs"));
    }

    @Test
    void getAllHubs_ReturnsList() {
        when(hubRepository.findAll()).thenReturn(Arrays.asList(testHub));

        List<Hub> hubs = adminService.getAllHubs();

        assertEquals(1, hubs.size());
        assertEquals("NYC-01", hubs.get(0).getCode());
    }

    @Test
    void createHub_Success() {
        when(hubRepository.save(any(Hub.class))).thenReturn(testHub);

        Hub result = adminService.createHub(testHub);

        assertNotNull(result);
        assertEquals("NYC Central Hub", result.getName());
        verify(hubRepository).save(any(Hub.class));
    }

    @Test
    void getHubById_Found() {
        when(hubRepository.findById(1L)).thenReturn(Optional.of(testHub));

        Hub result = adminService.getHubById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getHubById_NotFound() {
        when(hubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adminService.getHubById(99L));
    }

    @Test
    void deleteHub_Success() {
        doNothing().when(hubRepository).deleteById(1L);

        adminService.deleteHub(1L);

        verify(hubRepository).deleteById(1L);
    }

    @Test
    void generateReport_DeliverySummary() {
        Report report = Report.builder()
                .id(1L)
                .type("DELIVERY_SUMMARY")
                .title("Test Report")
                .build();

        when(reportRepository.save(any(Report.class))).thenReturn(report);

        Report result = adminService.generateReport("DELIVERY_SUMMARY", "Test Report", "admin");

        assertNotNull(result);
        assertEquals("DELIVERY_SUMMARY", result.getType());
    }

    @Test
    void getAllReports_ReturnsSortedList() {
        when(reportRepository.findAllByOrderByGeneratedAtDesc()).thenReturn(Arrays.asList());

        List<Report> reports = adminService.getAllReports();

        assertNotNull(reports);
        verify(reportRepository).findAllByOrderByGeneratedAtDesc();
    }

    @Test
    void resolveDeliveryException_Success() {
        Map<String, Object> delivery = new HashMap<>();
        delivery.put("trackingNumber", "SC123456");

        when(deliveryClient.getDeliveryById(1L)).thenReturn(delivery);
        when(deliveryClient.updateStatus(1L, "RESOLVED")).thenReturn(delivery);

        Object result = adminService.resolveDeliveryException(1L, "RESOLVED");

        assertNotNull(result);
        verify(deliveryClient).updateStatus(1L, "RESOLVED");
        verify(trackingClient).addTrackingEvent(eq(1L), eq("SC123456"), anyString(), anyString(), anyString());
    }
}
