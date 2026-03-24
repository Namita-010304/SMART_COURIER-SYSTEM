package com.smartcourier.admin.service;

import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.repository.HubRepository;
import com.smartcourier.admin.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private HubRepository hubRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminService adminService;

    private Hub testHub;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "deliveryServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(adminService, "authServiceUrl", "http://localhost:8081");

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
        when(restTemplate.getForObject(anyString(), eq(Object[].class))).thenReturn(new Object[10]);

        var dashboard = adminService.getDashboardData();

        assertNotNull(dashboard);
        assertEquals(10, dashboard.get("totalDeliveries"));
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
}
