package com.smartcourier.admin.repository;

import com.smartcourier.admin.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByTypeOrderByGeneratedAtDesc(String type);
    List<Report> findAllByOrderByGeneratedAtDesc();
}
