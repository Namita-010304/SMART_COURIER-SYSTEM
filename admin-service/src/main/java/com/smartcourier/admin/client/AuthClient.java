package com.smartcourier.admin.client;

import com.smartcourier.admin.dto.AdminUserCreateRequest;
import com.smartcourier.admin.dto.AdminUserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/admin/users")
    Map<String, Object> createUser(
            @RequestBody AdminUserCreateRequest request,
            @RequestHeader("X-User-Role") String role
    );

    @GetMapping("/auth/users")
    List<Object> getAllUsers(@RequestHeader("X-User-Role") String role);

    @PutMapping("/auth/users/{id}")
    Object updateUser(@PathVariable("id") Long id, @RequestBody AdminUserUpdateRequest request,
                      @RequestHeader("X-User-Role") String role);

    @DeleteMapping("/auth/users/{id}")
    void deleteUser(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role);
}
