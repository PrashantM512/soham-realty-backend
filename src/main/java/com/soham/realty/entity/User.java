package com.soham.realty.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;
 
 @Column(nullable = false, length = 100)
 private String name;
 
 @Column(nullable = false, unique = true, length = 50)
 private String username;
 
 @Column(nullable = false, unique = true, length = 100)
 private String email;
 
 @Column(nullable = false)
 private String password;
 
 @Column(length = 20)
 private String role = "ADMIN";
 
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