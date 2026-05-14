package com.smartcourier.auth.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    private String fullName;
    private String phone;
    private String role;
    private String email;
    private String password;
}
