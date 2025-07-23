package com.soham.realty.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.soham.realty.entity.PropertyImage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    @Modifying
    @Query("DELETE FROM PropertyImage pi WHERE pi.property.id = :propertyId")
    void deleteByPropertyId(@Param("propertyId") Long propertyId);
}
