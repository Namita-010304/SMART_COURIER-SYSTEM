package com.smartcourier.tracking.repository;

import com.smartcourier.tracking.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByDeliveryId(Long deliveryId);
}
