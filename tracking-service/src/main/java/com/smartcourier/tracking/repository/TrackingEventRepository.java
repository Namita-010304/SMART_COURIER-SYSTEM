package com.smartcourier.tracking.repository;

import com.smartcourier.tracking.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByTrackingNumberOrderByTimestampDesc(String trackingNumber);
    List<TrackingEvent> findByDeliveryIdOrderByTimestampDesc(Long deliveryId);
}
