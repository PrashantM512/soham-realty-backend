package com.soham.realty.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1000.0", message = "Price must be at least ₹1,000")
    @DecimalMax(value = "999999999.0", message = "Price cannot exceed ₹999,999,999")
    private BigDecimal price;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 200, message = "Address must be between 5 and 200 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "State must contain only letters and spaces")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "\\d{6}", message = "ZIP code must be exactly 6 digits")
    private String zip;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    @Max(value = 20, message = "Bedrooms cannot exceed 20")
    private Integer bedrooms = 0;

    @DecimalMin(value = "0.0", message = "Bathrooms cannot be negative")
    @DecimalMax(value = "20.0", message = "Bathrooms cannot exceed 20")
    private BigDecimal bathrooms = BigDecimal.ZERO;

    @Min(value = 0, message = "Square footage cannot be negative")
    @Max(value = 50000, message = "Square footage cannot exceed 50,000")
    private Integer squareFootage = 0;

    @NotBlank(message = "Property type is required")
    @Pattern(regexp = "House|Farm house|Flat|Apartment|Condo|Townhouse|Villa|Studio Apartment|Penthouse|Loft|Row House|Bungalow|Independent House",
             message = "Invalid property type")
    private String propertyType;

    @Pattern(regexp = "Available|Sold", message = "Status must be either 'Available' or 'Sold'")
    private String status = "Available";

    @Pattern(
        regexp = "^$|^(https?://)?(www\\.)?(instagram\\.com|instagr\\.am)/(p|reel|tv)/[A-Za-z0-9_-]+/?.*$",
        message = "Video link must be a valid Instagram URL (posts, reels, or IGTV)"
    )
    private String videoLink;

    private Boolean featured = false;
}