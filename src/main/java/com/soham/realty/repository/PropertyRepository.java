package com.soham.realty.repository;

import com.soham.realty.entity.Property;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    
    // OPTIMIZED: Cache featured properties query
    @Query("SELECT p FROM Property p WHERE p.featured = true AND p.status = :status ORDER BY p.createdAt DESC")
    List<Property> findFeaturedPropertiesByStatus(@Param("status") String status);
    
    // For reading with images (no lock needed for read operations)
    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Property> findByIdWithImages(@Param("id") Long id);
    
    // For update operations that need locking
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Property> findByIdWithLock(@Param("id") Long id);
    
    // OPTIMIZED: Efficient search queries
    @Query("""
    		SELECT p FROM Property p
    		WHERE (:title IS NULL OR 
    		       LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%')) OR
    		       LOWER(p.description) LIKE LOWER(CONCAT('%', :title, '%')) OR
    		       LOWER(p.address) LIKE LOWER(CONCAT('%', :title, '%')))
    		AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
    		AND (:propertyType IS NULL OR p.propertyType = :propertyType)
    		AND (:status IS NULL OR p.status = :status)
    		AND (:minPrice IS NULL OR p.price >= :minPrice)
    		AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    		ORDER BY p.createdAt DESC
    		""")
    		Page<Property> findPropertiesWithFilters(
    		    @Param("title") String title,
    		    @Param("city") String city,
    		    @Param("propertyType") String propertyType,
    		    @Param("status") String status,
    		    @Param("minPrice") java.math.BigDecimal minPrice,
    		    @Param("maxPrice") java.math.BigDecimal maxPrice,
    		    Pageable pageable
    		);
    
    // Keep existing methods for compatibility
    List<Property> findByFeaturedTrueAndStatus(String status);
}