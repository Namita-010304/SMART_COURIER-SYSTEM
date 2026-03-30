package com.smartcourier.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String username;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "sender_address_id", nullable = false)
    private Address senderAddress;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "receiver_address_id", nullable = false)
    private Address receiverAddress;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "package_id", nullable = false)
    private ParcelPackage parcelPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    private Double charge;

    private LocalDateTime scheduledPickup;

    private String specialInstructions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = DeliveryStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
