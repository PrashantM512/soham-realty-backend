package com.soham.realty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {
 private Long id;
 private String title;
 private BigDecimal price;
 private String description;
 private String address;
 private String city;
 private String state;
 private String zip;
 private List<String> images;
 private Integer bedrooms;
 private BigDecimal bathrooms;
 private Integer squareFootage;
 private String videoLink;
 private String propertyType;
 private Boolean featured;
 private String status;
 private LocalDateTime createdAt;
 private LocalDateTime updatedAt;
}
