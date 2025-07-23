package com.soham.realty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contact_created_at", columnList = "created_at"),
    @Index(name = "idx_contact_status", columnList = "status"),
    @Index(name = "idx_contact_property_id", columnList = "property_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    // OPTIMIZED: Use FetchType.LAZY for better performance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;
    
    @Column(length = 20)
    private String status = "New";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}