package com.smartcourier.admin.controller;

import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardData());
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<Object>> getAllDeliveries() {
        return ResponseEntity.ok(adminService.getAllDeliveries());
    }

    @PutMapping("/deliveries/{id}/resolve")
    public ResponseEntity<Object> resolveException(
            @PathVariable Long id,
            @RequestParam String resolution) {
        return ResponseEntity.ok(adminService.resolveDeliveryException(id, resolution));
    } //left 

    @GetMapping("/hubs")
    public ResponseEntity<List<Hub>> getAllHubs() {
        return ResponseEntity.ok(adminService.getAllHubs());
    }

    @GetMapping("/hubs/{id}")
    public ResponseEntity<Hub> getHubById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getHubById(id));
    }

    @PostMapping("/hubs")
    public ResponseEntity<Hub> createHub(@RequestBody Hub hub) {
        return ResponseEntity.ok(adminService.createHub(hub));
    }

    @PutMapping("/hubs/{id}")
    public ResponseEntity<Hub> updateHub(@PathVariable Long id, @RequestBody Hub hub) {
        return ResponseEntity.ok(adminService.updateHub(id, hub));
    }

    @DeleteMapping("/hubs/{id}")
    public ResponseEntity<Void> deleteHub(@PathVariable Long id) {
        adminService.deleteHub(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users") 
    public ResponseEntity<List<Object>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(adminService.getAllReports());
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> generateReport(
            @RequestParam String type,
            @RequestParam String title,
            @RequestHeader(value = "X-User-Username", defaultValue = "admin") String username)   // Use header for Auditing (I created this report).
            {
        return ResponseEntity.ok(adminService.generateReport(type, title, username));
    }
}
