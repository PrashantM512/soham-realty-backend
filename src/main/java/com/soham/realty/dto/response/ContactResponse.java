package com.soham.realty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String message;
    private Long propertyId;
    private String propertyTitle;
    private String status;
    private LocalDateTime createdAt;
}