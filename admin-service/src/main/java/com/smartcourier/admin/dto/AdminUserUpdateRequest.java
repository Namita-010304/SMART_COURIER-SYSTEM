package com.smartcourier.admin.dto;

public record AdminUserUpdateRequest(
        String email,
        String fullName,
        String phone,
        String role
) {}
