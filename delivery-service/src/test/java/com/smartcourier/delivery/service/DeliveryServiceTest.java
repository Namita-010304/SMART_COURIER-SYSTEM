package com.smartcourier.delivery.service;

import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.entity.*;
import com.smartcourier.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryService deliveryService;

    private DeliveryRequest deliveryRequest;
    private Delivery testDelivery;

    @BeforeEach
    void setUp() {
        AddressDTO sender = new AddressDTO();
        sender.setFullName("John Sender");
        sender.setPhone("1111111111");
        sender.setStreet("123 Main St");
        sender.setCity("New York");
        sender.setState("NY");
        sender.setZipCode("10001");
        sender.setCountry("USA");

        AddressDTO receiver = new AddressDTO();
        receiver.setFullName("Jane Receiver");
        receiver.setPhone("2222222222");
        receiver.setStreet("456 Oak Ave");
        receiver.setCity("Los Angeles");
        receiver.setState("CA");
        receiver.setZipCode("90001");
        receiver.setCountry("USA");

        PackageDTO pkg = new PackageDTO();
        pkg.setWeight(2.5);
        pkg.setDescription("Books");
        pkg.setServiceType("DOMESTIC");
        pkg.setFragile(false);

        deliveryRequest = new DeliveryRequest();
        deliveryRequest.setSenderAddress(sender);
        deliveryRequest.setReceiverAddress(receiver);
        deliveryRequest.setPackageDetails(pkg);

        testDelivery = Delivery.builder()
                .id(1L)
                .trackingNumber("SC123456789")
                .username("testuser")
                .status(DeliveryStatus.BOOKED)
                .charge(7.24)
                .build();
    }

    @Test
    void createDelivery_Success() {
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        Delivery result = deliveryService.createDelivery(deliveryRequest, "testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals(DeliveryStatus.BOOKED, result.getStatus());
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void getMyDeliveries_ReturnsUserDeliveries() {
        when(deliveryRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(Arrays.asList(testDelivery));

        List<Delivery> results = deliveryService.getMyDeliveries("testuser");

        assertEquals(1, results.size());
        assertEquals("SC123456789", results.get(0).getTrackingNumber());
    }

    @Test
    void getDeliveryById_Found() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));

        Delivery result = deliveryService.getDeliveryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getDeliveryById_NotFound_ThrowsException() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> deliveryService.getDeliveryById(99L));
    }

    @Test
    void updateStatus_Success() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        Delivery result = deliveryService.updateStatus(1L, "IN_TRANSIT");

        assertNotNull(result);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void getServiceInfo_ReturnsValidData() {
        var info = deliveryService.getServiceInfo();

        assertNotNull(info);
        assertEquals("SmartCourier", info.get("company"));
        assertNotNull(info.get("services"));
    }
}
