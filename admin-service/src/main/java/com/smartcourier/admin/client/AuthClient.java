package com.smartcourier.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestBody Object loginRequest);

    @PostMapping("/auth/signup")
    Map<String, Object> signup(@RequestBody Object signupRequest);

    @org.springframework.web.bind.annotation.GetMapping("/auth/users")
    java.util.List<Object> getAllUsers();
}
