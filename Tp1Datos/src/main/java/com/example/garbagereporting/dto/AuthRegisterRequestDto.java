package com.example.garbagereporting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequestDto {

    @NotBlank(message = "firstName is required")
    @Size(max = 60, message = "firstName must have at most 60 characters")
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 60, message = "lastName must have at most 60 characters")
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    @Size(max = 120, message = "email must have at most 120 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 120, message = "password must be between 8 and 120 characters")
    private String password;
}
