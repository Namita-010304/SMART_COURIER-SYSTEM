package com.smartcourier.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParcelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double weight;

    private Double length;
    private Double width;
    private Double height;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    private Double declaredValue;
    private Boolean fragile;
}
