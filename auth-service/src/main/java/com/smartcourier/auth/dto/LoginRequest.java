package com.smartcourier.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank
    @Size(min = 6)
    @NotBlank(message = "Password is required")
    private String password;
}
