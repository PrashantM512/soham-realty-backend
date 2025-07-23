package com.soham.realty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImage {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;
 
 @ManyToOne
 @JoinColumn(name = "property_id", nullable = false)
 @JsonIgnore
 private Property property;
 
 @Column(name = "image_url", nullable = false, length = 500)
 private String imageUrl;
 
 @Column(name = "image_order")
 private Integer imageOrder = 0;
 
 @Column(name = "created_at")
 private LocalDateTime createdAt;
 
 @PrePersist
 protected void onCreate() {
     createdAt = LocalDateTime.now();
 }
}