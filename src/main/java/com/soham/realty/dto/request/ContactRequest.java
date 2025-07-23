package com.soham.realty.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ContactRequest {
 @NotBlank(message = "Name is required")
 @Size(min = 2, max = 100)
 private String name;
 
 @NotBlank(message = "Email is required")
 @Email(message = "Email should be valid")
 private String email;
 
 @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Phone number should be valid")
 private String phone;
 
 @NotBlank(message = "Message is required")
 @Size(min = 10, message = "Message must be at least 10 characters")
 private String message;
 
 private Long propertyId;
}

