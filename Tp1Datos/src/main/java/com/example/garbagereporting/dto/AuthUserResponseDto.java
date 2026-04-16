package com.example.garbagereporting.dto;

import com.example.garbagereporting.model.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthUserResponseDto {
    String id;
    String firstName;
    String lastName;
    String email;
    UserRole role;
    boolean emailVerified;
}
