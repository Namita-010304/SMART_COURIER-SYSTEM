package com.smartcourier.admin.controller;

import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    void getDashboard_Success() throws Exception {
        when(adminService.getDashboardData()).thenReturn(new HashMap<>());
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllHubs_Success() throws Exception {
        when(adminService.getAllHubs()).thenReturn(Arrays.asList(new Hub()));
        mockMvc.perform(get("/admin/hubs"))
                .andExpect(status().isOk());
    }

    @Test
    void createHub_Success() throws Exception {
        Hub hub = Hub.builder().name("New Hub").build();
        when(adminService.createHub(any())).thenReturn(hub);

        mockMvc.perform(post("/admin/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Hub\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Hub"));
    }

    @Test
    void resolveException_Success() throws Exception {
        when(adminService.resolveDeliveryException(anyLong(), anyString(), anyString(), anyString())).thenReturn(new HashMap<>());

        mockMvc.perform(put("/admin/deliveries/1/resolve")
                        .param("resolution", "RESOLVED"))
                .andExpect(status().isOk());
    }

    @Test
    void generateReport_Success() throws Exception {
        Report report = Report.builder().type("SUMMARY").build();
        when(adminService.generateReport(anyString(), anyString(), anyString())).thenReturn(report);

        mockMvc.perform(post("/admin/reports")
                        .param("type", "SUMMARY")
                        .param("title", "Daily Summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SUMMARY"));
    }
}
