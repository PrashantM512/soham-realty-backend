package com.soham.realty.service;

import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.dto.request.PropertyRequest;
import com.soham.realty.dto.request.SearchRequest;
import com.soham.realty.dto.response.PaginatedResponse;
import com.soham.realty.dto.response.PropertyResponse;

import java.util.List;

public interface PropertyService {
 PaginatedResponse<PropertyResponse> getAllProperties(SearchRequest searchRequest);
 List<PropertyResponse> getFeaturedProperties();
 PropertyResponse getPropertyById(Long id);
 PropertyResponse createProperty(PropertyRequest request);
 PropertyResponse updateProperty(Long id, PropertyRequest request);
 void deleteProperty(Long id);
 List<String> uploadPropertyImages(Long propertyId, MultipartFile[] files);
}
