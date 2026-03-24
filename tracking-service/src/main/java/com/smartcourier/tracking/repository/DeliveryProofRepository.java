package com.smartcourier.tracking.repository;

import com.smartcourier.tracking.entity.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, Long> {
    Optional<DeliveryProof> findByDeliveryId(Long deliveryId);
}
