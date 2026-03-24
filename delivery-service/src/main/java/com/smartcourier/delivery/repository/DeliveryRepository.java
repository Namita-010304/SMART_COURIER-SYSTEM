package com.smartcourier.delivery.repository;

import com.smartcourier.delivery.entity.Delivery;
import com.smartcourier.delivery.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByUsernameOrderByCreatedAtDesc(String username);
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
    List<Delivery> findByStatus(DeliveryStatus status);
    long countByStatus(DeliveryStatus status);
}
