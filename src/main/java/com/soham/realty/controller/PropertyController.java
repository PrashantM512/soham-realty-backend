package com.soham.realty.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.dto.request.PropertyRequest;
import com.soham.realty.dto.request.SearchRequest;
import com.soham.realty.dto.response.ApiResponse;
import com.soham.realty.dto.response.PaginatedResponse;
import com.soham.realty.dto.response.PropertyResponse;
import com.soham.realty.service.PropertyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<PropertyResponse>> getAllProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) String bedrooms,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "9") Integer limit
    ) {
        log.debug("Received request - search: {}, location: {}, priceRange: {}, propertyType: {}, bedrooms: {}, sortBy: {}, page: {}, limit: {}", 
                  search, location, priceRange, propertyType, bedrooms, sortBy, page, limit);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSearch(search);
        searchRequest.setLocation(location);
        searchRequest.setPriceRange(priceRange);
        searchRequest.setPropertyType(propertyType);
        searchRequest.setBedrooms(bedrooms);
        searchRequest.setSortBy(sortBy);
        searchRequest.setPage(page);
        searchRequest.setLimit(limit);

        PaginatedResponse<PropertyResponse> response = propertyService.getAllProperties(searchRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getFeaturedProperties() {
        List<PropertyResponse> properties = propertyService.getFeaturedProperties();
        return ResponseEntity.ok(ApiResponse.success(properties, "Featured properties retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(@PathVariable Long id) {
        PropertyResponse property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(ApiResponse.success(property, "Property retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(@Valid @RequestBody PropertyRequest request) {
        PropertyResponse property = propertyService.createProperty(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(property, "Property created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request) {
        PropertyResponse property = propertyService.updateProperty(id, request);
        return ResponseEntity.ok(ApiResponse.success(property, "Property updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Property deleted successfully"));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadPropertyImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] files) {
        
        if (files.length == 0) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No files provided"));
        }
        
        if (files.length > 5) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Maximum 5 images allowed"));
        }
        
        List<String> uploadedUrls = propertyService.uploadPropertyImages(id, files);
        return ResponseEntity.ok(ApiResponse.success(uploadedUrls, "Images uploaded successfully"));
    }
}