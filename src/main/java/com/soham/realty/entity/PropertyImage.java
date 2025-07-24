package com.soham.realty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_images", 
    indexes = {
        @Index(name = "idx_property_images_property_id", columnList = "property_id"),
        @Index(name = "idx_property_images_order", columnList = "property_id, image_order")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "property") // Prevent circular reference
@EqualsAndHashCode(exclude = "property") // Prevent circular reference
public class PropertyImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnore
    private Property property;
    
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    
    @Column(name = "image_order")
    private Integer imageOrder = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Version
    @Column(name = "version")
    private Long version = 0L;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}