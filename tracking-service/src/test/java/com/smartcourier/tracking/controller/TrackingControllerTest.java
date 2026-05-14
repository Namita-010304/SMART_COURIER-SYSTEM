package com.smartcourier.tracking.controller;

import com.smartcourier.tracking.dto.TrackingResponseDTO;
import com.smartcourier.tracking.entity.*;
import com.smartcourier.tracking.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrackingController.class)
public class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    @Test
    void getTracking_Success() throws Exception {
        TrackingResponseDTO response = TrackingResponseDTO.builder().trackingNumber("SC123").currentStatus("IN_TRANSIT").build();
        when(trackingService.getTrackingInfo("SC123")).thenReturn(response);

        mockMvc.perform(get("/tracking/SC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("SC123"));
    }

    @Test
    void addTrackingEvent_Success() throws Exception {
        TrackingEvent event = TrackingEvent.builder().id(1L).status("DELIVERED").build();
        when(trackingService.addTrackingEvent(anyLong(), anyString(), anyString(), any(), any())).thenReturn(event);

        mockMvc.perform(post("/tracking/events")
                        .param("deliveryId", "1")
                        .param("trackingNumber", "SC123")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void uploadDocument_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test content".getBytes());
        Document doc = Document.builder().id(1L).fileName("test.pdf").build();
        
        when(trackingService.uploadDocument(anyLong(), any())).thenReturn(doc);

        mockMvc.perform(multipart("/tracking/documents/upload")
                        .file(file)
                        .param("deliveryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.pdf"));
    }

    @Test
    void getDocuments_Success() throws Exception {
        when(trackingService.getDocuments(1L)).thenReturn(Arrays.asList(new Document()));

        mockMvc.perform(get("/tracking/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getDeliveryProof_Success() throws Exception {
        DeliveryProof proof = DeliveryProof.builder().id(1L).recipientName("John Doe").build();
        when(trackingService.getDeliveryProof(1L)).thenReturn(proof);

        mockMvc.perform(get("/tracking/1/proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("John Doe"));
    }
}
