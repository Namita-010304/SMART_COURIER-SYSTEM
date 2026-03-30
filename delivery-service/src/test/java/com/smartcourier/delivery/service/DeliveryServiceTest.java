package com.smartcourier.delivery.service;

import com.smartcourier.delivery.client.TrackingClient;
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

    @Mock
    private TrackingClient trackingClient;

    @InjectMocks
    private DeliveryService deliveryService;

    private DeliveryRequest deliveryRequest;
    private Delivery testDelivery;

    @BeforeEach
    void setUp() {
        AddressDTO sender = AddressDTO.builder()
                .fullName("John Sender")
                .phone("1111111111")
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .build();

        AddressDTO receiver = AddressDTO.builder()
                .fullName("Jane Receiver")
                .phone("2222222222")
                .street("456 Oak Ave")
                .city("Los Angeles")
                .state("CA")
                .zipCode("90001")
                .country("USA")
                .build();

        PackageDTO pkg = PackageDTO.builder()
                .weight(2.5)
                .description("Books")
                .serviceType("DOMESTIC")
                .fragile(false)
                .build();

        deliveryRequest = DeliveryRequest.builder()
                .senderAddress(sender)
                .receiverAddress(receiver)
                .packageDetails(pkg)
                .build();

        Address senderEntity = Address.builder()
                .fullName("John Sender").phone("1111111111").street("123 Main St").city("New York").state("NY").zipCode("10001").country("USA")
                .build();
        Address receiverEntity = Address.builder()
                .fullName("Jane Receiver").phone("2222222222").street("456 Oak Ave").city("Los Angeles").state("CA").zipCode("90001").country("USA")
                .build();
        ParcelPackage packageEntity = ParcelPackage.builder()
                .weight(2.5).description("Books").serviceType(ServiceType.DOMESTIC).fragile(false)
                .build();

        testDelivery = Delivery.builder()
                .id(1L)
                .trackingNumber("SC123456789")
                .username("testuser")
                .senderAddress(senderEntity)
                .receiverAddress(receiverEntity)
                .parcelPackage(packageEntity)
                .status(DeliveryStatus.BOOKED)
                .charge(7.24)
                .build();
    }

    @Test
    void createDelivery_Success() {
        // Create an ArgumentCaptor to "catch" the object that goes into the repository
        org.mockito.ArgumentCaptor<Delivery> deliveryCaptor = org.mockito.ArgumentCaptor.forClass(Delivery.class);
        
        // We tell Mockito: When save is called, return whatever was passed in (so we can see the changes)
        when(deliveryRepository.save(deliveryCaptor.capture())).thenReturn(testDelivery);

        deliveryService.createDelivery(deliveryRequest, "testuser");

        // Now we inspect the object that the service tried to save
        Delivery savedDelivery = deliveryCaptor.getValue();
        
        assertNotNull(savedDelivery);
        assertEquals("testuser", savedDelivery.getUsername());
        assertEquals(DeliveryStatus.BOOKED, savedDelivery.getStatus());
        
        // Verify the Business Logic (calculateCharge)
        // Base for DOMESTIC is 5.99, weight is 2.5 * 0.5 = 1.25. Total = 7.24
        assertEquals(7.24, savedDelivery.getCharge());
        
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void getMyDeliveries_ReturnsUserDeliveries() {
        when(deliveryRepository.findByUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(Arrays.asList(testDelivery));

        List<DeliveryResponseDTO> results = deliveryService.getMyDeliveries("testuser");

        assertEquals(1, results.size());
        assertEquals("SC123456789", results.get(0).getTrackingNumber());
    }

    @Test
    void getDeliveryById_Found() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));

        DeliveryResponseDTO result = deliveryService.getDeliveryById(1L);

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

        DeliveryResponseDTO result = deliveryService.updateStatus(1L, "IN_TRANSIT");

        assertNotNull(result);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void getServiceInfo_ReturnsValidData() {
        ServiceInfoDTO info = deliveryService.getServiceInfo();

        assertNotNull(info);
        assertEquals("SmartCourier", info.getCompany());
        assertNotNull(info.getServices());
    }
}
