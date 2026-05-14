package com.smartcourier.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.exception.ResourceNotFoundException;
import com.smartcourier.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
public class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDelivery_Success() throws Exception { 
        DeliveryRequest request = DeliveryRequest.builder()
                .senderAddress(AddressDTO.builder().fullName("Sender").phone("1234567890").street("Street").city("City").state("State").zipCode("12345").country("Country").build())
                .receiverAddress(AddressDTO.builder().fullName("Receiver").phone("0987654321").street("Street").city("City").state("State").zipCode("54321").country("Country").build())
                .packageDetails(PackageDTO.builder().weight(1.0).description("Test Package").serviceType("DOMESTIC").build())
                .build();

        DeliveryResponseDTO response = DeliveryResponseDTO.builder()
                .id(1L)
                .trackingNumber("SC123")
                .status(com.smartcourier.delivery.entity.DeliveryStatus.BOOKED)
                .build();

        when(deliveryService.createDelivery(any(DeliveryRequest.class), anyString(), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/deliveries")
                        .header("X-User-Username", "testuser")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("SC123"));
    }

    @Test
    void getDeliveryById_Found() throws Exception {
        DeliveryResponseDTO response = DeliveryResponseDTO.builder().id(1L).trackingNumber("SC123").build();
        when(deliveryService.getDeliveryById(1L)).thenReturn(response);

        mockMvc.perform(get("/deliveries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getDeliveryById_NotFound() throws Exception {
        when(deliveryService.getDeliveryById(99L)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/deliveries/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyDeliveries_Success() throws Exception {
        when(deliveryService.getMyDeliveries("testuser")).thenReturn(Arrays.asList(new DeliveryResponseDTO()));

        mockMvc.perform(get("/deliveries/my")
                        .header("X-User-Username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateStatus_Success() throws Exception {
        DeliveryResponseDTO response = DeliveryResponseDTO.builder().id(1L).status(com.smartcourier.delivery.entity.DeliveryStatus.PICKED_UP).build();
        when(deliveryService.updateStatus(anyLong(), anyString(), anyString(), anyString(), any())).thenReturn(response);

        mockMvc.perform(put("/deliveries/1/status")
                        .param("status", "PICKED_UP")
                        .header("X-User-Username", "admin")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
    }

    @Test
    void initDraft_Success() throws Exception {
        when(deliveryService.initDraft("testuser")).thenReturn(new DeliveryResponseDTO());

        mockMvc.perform(post("/deliveries/draft")
                        .header("X-User-Username", "testuser"))
                .andExpect(status().isOk());
    }
}
