package com.smartcourier.admin.repository;

import com.smartcourier.admin.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HubRepository extends JpaRepository<Hub, Long> {
    Optional<Hub> findByCode(String code);
    List<Hub> findByActive(Boolean active);
    List<Hub> findByCity(String city);
}
