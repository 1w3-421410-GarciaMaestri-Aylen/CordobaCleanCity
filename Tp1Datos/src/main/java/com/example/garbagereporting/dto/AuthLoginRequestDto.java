package com.example.garbagereporting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthLoginRequestDto {

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 120, message = "email must have at most 120 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(max = 120, message = "password must have at most 120 characters")
    private String password;
}
