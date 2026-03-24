package com.smartcourier.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_proofs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long deliveryId;

    @Column(nullable = false)
    private String recipientName;

    private String signatureUrl;

    private String photoUrl;

    private String notes;

    @Column(nullable = false)
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        if (deliveredAt == null) {
            deliveredAt = LocalDateTime.now();
        }
    }
}
