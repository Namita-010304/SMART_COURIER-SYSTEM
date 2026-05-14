package com.smartcourier.admin.dto;

public record AdminUserCreateRequest(
        String username,
        String email,
        String password,
        String fullName,
        String phone,
        String role
) {}
