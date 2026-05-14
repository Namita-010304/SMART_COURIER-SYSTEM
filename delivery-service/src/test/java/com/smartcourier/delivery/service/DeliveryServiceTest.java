package com.smartcourier.delivery.service;

import com.smartcourier.delivery.client.TrackingClient;
import com.smartcourier.delivery.dto.*;
import com.smartcourier.delivery.entity.*;
import com.smartcourier.delivery.exception.*;
import com.smartcourier.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*; 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) 
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private TrackingClient trackingClient;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

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
                .paid(true)
                .assignedAgentId("AGENT001")
                .build();
    }

    @Test
    void createDelivery_Success() {
        // Create an ArgumentCaptor to "catch" the object that goes into the repository
        org.mockito.ArgumentCaptor<Delivery> deliveryCaptor = org.mockito.ArgumentCaptor.forClass(Delivery.class);
        
        // We tell Mockito: When save is called, return whatever was passed in (so we can see the changes)
        when(deliveryRepository.save(deliveryCaptor.capture())).thenReturn(testDelivery);

        deliveryService.createDelivery(deliveryRequest, "testuser", "CUSTOMER");

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

        assertThrows(ResourceNotFoundException.class, () -> deliveryService.getDeliveryById(99L));
    }

    @Test
    void updateStatus_Success() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);

        DeliveryResponseDTO result = deliveryService.updateStatus(1L, "PICKED_UP", "ADMIN", "system", "Test Pickup");

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

    @Test
    void getStatusDistribution_Success() {
        when(deliveryRepository.countByStatus(any(DeliveryStatus.class))).thenReturn(5L);

        Map<String, Long> stats = deliveryService.getStatusDistribution();

        assertNotNull(stats);
        assertTrue(stats.containsKey("BOOKED"));
        assertEquals(5L, stats.get("BOOKED"));
    }

    @Test
    void initDraft_Success() {
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArguments()[0]);

        DeliveryResponseDTO result = deliveryService.initDraft("testuser");

        assertNotNull(result);
        assertEquals(DeliveryStatus.DRAFT, result.getStatus());
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void updateSender_Success() {
        Delivery draft = Delivery.builder().id(1L).username("testuser").status(DeliveryStatus.DRAFT).build();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArguments()[0]);

        AddressDTO sender = AddressDTO.builder().fullName("Sender Name").build();
        DeliveryResponseDTO result = deliveryService.updateSender(1L, sender, "testuser");

        assertNotNull(result);
        assertEquals("Sender Name", result.getSenderAddress().getFullName());
    }

    @Test
    void updateReceiver_Success() {
        Delivery draft = Delivery.builder().id(1L).username("testuser").status(DeliveryStatus.DRAFT).build();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArguments()[0]);

        AddressDTO receiver = AddressDTO.builder().fullName("Receiver Name").build();
        DeliveryResponseDTO result = deliveryService.updateReceiver(1L, receiver, "testuser");

        assertNotNull(result);
        assertEquals("Receiver Name", result.getReceiverAddress().getFullName());
    }

    @Test
    void updatePackage_Success() {
        Delivery draft = Delivery.builder().id(1L).username("testuser").status(DeliveryStatus.DRAFT).build();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArguments()[0]);

        PackageDTO pkg = PackageDTO.builder().weight(2.0).serviceType("DOMESTIC").build();
        DeliveryResponseDTO result = deliveryService.updatePackage(1L, pkg, "testuser");

        assertNotNull(result);
        assertNotNull(result.getCharge());
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void finalizeDelivery_Success() {
        Delivery draft = Delivery.builder()
                .id(1L)
                .username("testuser")
                .status(DeliveryStatus.DRAFT)
                .senderAddress(Address.builder().fullName("S").build())
                .receiverAddress(Address.builder().fullName("R").build())
                .parcelPackage(ParcelPackage.builder().weight(1.0).serviceType(ServiceType.DOMESTIC).build())
                .build();
        
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArguments()[0]);

        DeliveryResponseDTO result = deliveryService.finalizeDelivery(1L, "testuser");

        assertEquals(DeliveryStatus.BOOKED, result.getStatus());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void updatePackage_InvalidServiceType_ThrowsException() {
        Delivery draft = Delivery.builder().id(1L).username("testuser").status(DeliveryStatus.DRAFT).build();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(draft));

        PackageDTO pkg = PackageDTO.builder().weight(2.0).serviceType("INVALID_TYPE").build();

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, 
                () -> deliveryService.updatePackage(1L, pkg, "testuser"));

        assertTrue(exception.getMessage().contains("Invalid service type"));
    }
}
