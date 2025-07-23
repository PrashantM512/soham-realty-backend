package com.soham.realty.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
 @NotBlank(message = "Name is required")
 @Size(min = 2, max = 100)
 private String name;
 
 @NotBlank(message = "Username is required")
 @Size(min = 3, max = 50)
 private String username;
 
 @NotBlank(message = "Email is required")
 @Email(message = "Email should be valid")
 private String email;
 
 @NotBlank(message = "Password is required")
 @Size(min = 6, message = "Password must be at least 6 characters")
 private String password;
}