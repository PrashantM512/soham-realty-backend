package com.soham.realty.repository;

import com.soham.realty.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    
    // Find all images for a property
    List<PropertyImage> findByPropertyIdOrderByImageOrderAsc(Long propertyId);
    
    // Count images for a property
    Long countByPropertyId(Long propertyId);
    
    // Note: We don't need deleteByPropertyId anymore since cascade handles it
}