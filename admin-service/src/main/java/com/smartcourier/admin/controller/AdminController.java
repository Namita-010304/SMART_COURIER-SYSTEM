package com.smartcourier.admin.controller;

import com.smartcourier.admin.dto.AdminUserCreateRequest;
import com.smartcourier.admin.dto.AdminUserUpdateRequest;
import com.smartcourier.admin.entity.Hub;
import com.smartcourier.admin.entity.Report;
import com.smartcourier.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/deliveries/{id}")
    public ResponseEntity<Object> getDeliveryById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getDeliveryById(id));
    }

    @PutMapping("/deliveries/{id}/resolve")
    public ResponseEntity<Object> resolveException(
            @PathVariable Long id,
            @RequestParam String resolution,
            @RequestHeader(value = "X-User-Username", defaultValue = "admin") String username,
            @RequestHeader(value = "X-User-Role", defaultValue = "ADMIN") String role) {
        return ResponseEntity.ok(adminService.resolveDeliveryException(id, resolution, username, role));
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

    @PostMapping("/users")
    public ResponseEntity<Object> createUser(@RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
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
