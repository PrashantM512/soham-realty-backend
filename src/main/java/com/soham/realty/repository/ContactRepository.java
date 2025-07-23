package com.soham.realty.repository;

import com.soham.realty.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    
    // OPTIMIZED: Single query with LEFT JOIN to handle null properties
    @Query("""
        SELECT c FROM Contact c 
        LEFT JOIN FETCH c.property p 
        ORDER BY c.createdAt DESC
    """)
    Page<Contact> findAllWithPropertyDetails(Pageable pageable);
    
    // Keep existing method for compatibility
    Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // OPTIMIZED: Count contacts by property for analytics
    @Query("SELECT COUNT(c) FROM Contact c WHERE c.property.id = :propertyId")
    long countByPropertyId(@Param("propertyId") Long propertyId);
}