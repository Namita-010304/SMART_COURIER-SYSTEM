package com.smartcourier.auth.dto;

import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String message;
}
