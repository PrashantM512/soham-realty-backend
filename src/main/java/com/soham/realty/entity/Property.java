package com.soham.realty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties", indexes = {
    @Index(name = "idx_property_city", columnList = "city"),
    @Index(name = "idx_property_price", columnList = "price"),
    @Index(name = "idx_property_type", columnList = "property_type"),
    @Index(name = "idx_property_status", columnList = "status"),
    @Index(name = "idx_property_featured", columnList = "featured"),
    @Index(name = "idx_property_created_at", columnList = "created_at"),
    @Index(name = "idx_property_search", columnList = "city, property_type, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false, length = 100)
    private String city;
    
    @Column(nullable = false, length = 100)
    private String state;
    
    @Column(nullable = false, length = 10)
    private String zip;
    
    private Integer bedrooms = 0;
    
    @Column(precision = 3, scale = 1)
    private BigDecimal bathrooms = BigDecimal.ZERO;
    
    @Column(name = "square_footage")
    private Integer squareFootage = 0;
    
    @Column(name = "property_type", nullable = false, length = 50)
    private String propertyType;
    
    @Column(length = 20)
    private String status = "Available";
    
    @Column(name = "video_link", length = 500)
    private String videoLink;
    
    private Boolean featured = false;
    
    // OPTIMIZED: Use FetchType.LAZY and proper cascade
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("imageOrder ASC")
    private List<PropertyImage> images = new ArrayList<>();
    
    @Column(name = "image_url")
    private String imageUrl;

    // Other fields, getters, setters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}