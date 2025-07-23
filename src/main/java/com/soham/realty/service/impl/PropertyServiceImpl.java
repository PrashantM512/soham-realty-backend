package com.soham.realty.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.dto.request.PropertyRequest;
import com.soham.realty.dto.request.SearchRequest;
import com.soham.realty.dto.response.PaginatedResponse;
import com.soham.realty.dto.response.PropertyResponse;
import com.soham.realty.entity.Property;
import com.soham.realty.entity.PropertyImage;
import com.soham.realty.exception.BadRequestException;
import com.soham.realty.exception.ResourceNotFoundException;
import com.soham.realty.repository.PropertyImageRepository;
import com.soham.realty.repository.PropertyRepository;
import com.soham.realty.service.FileStorageService;
import com.soham.realty.service.PropertyService;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageService fileStorageService;
    
    @Value("${app.backend.url}")
    private String backendUrl;
    
    private static final int MAX_IMAGES_PER_PROPERTY = 5;

    @Override
    public PaginatedResponse<PropertyResponse> getAllProperties(SearchRequest searchRequest) {
        log.debug("Fetching properties with filters: {}", searchRequest);
        
        Specification<Property> spec = buildSpecification(searchRequest);
        Sort sort = buildSort(searchRequest.getSortBy());
        Pageable pageable = PageRequest.of(
            searchRequest.getPage() - 1, 
            searchRequest.getLimit(), 
            sort
        );

        Page<Property> propertyPage = propertyRepository.findAll(spec, pageable);
        
        List<PropertyResponse> propertyResponses = propertyPage.getContent().stream()
            .map(this::mapToPropertyResponseLight)
            .collect(Collectors.toList());

        return PaginatedResponse.of(
            propertyResponses,
            propertyPage.getTotalElements(),
            searchRequest.getPage(),
            searchRequest.getLimit()
        );
    }

    @Override
    @Cacheable(value = "featuredProperties", unless = "#result.isEmpty()")
    public List<PropertyResponse> getFeaturedProperties() {
        log.debug("Fetching featured properties from database");
        List<Property> featuredProperties = propertyRepository.findFeaturedPropertiesByStatus("Available");
        List<PropertyResponse> result = featuredProperties.stream()
            .map(this::mapToPropertyResponse)
            .collect(Collectors.toList());
        
        log.info("Loaded {} featured properties", result.size());
        return result;
    }

    @Override
    @Cacheable(value = "propertyDetails", key = "#id")
    public PropertyResponse getPropertyById(Long id) {
        Property property = propertyRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        return mapToPropertyResponse(property);
    }

    @Override
    @CacheEvict(value = {"featuredProperties", "propertyDetails"}, allEntries = true)
    public PropertyResponse createProperty(PropertyRequest request) {
        log.info("Creating new property: {}", request.getTitle());
        
        Property property = new Property();
        mapRequestToProperty(request, property);
        
        Property savedProperty = propertyRepository.save(property);
        
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            savePropertyImages(savedProperty, request.getImages());
        }
        
        log.info("Created property with ID: {}, Featured: {}", savedProperty.getId(), savedProperty.getFeatured());
        return mapToPropertyResponse(savedProperty);
    }

    @Override
    @CacheEvict(value = {"featuredProperties", "propertyDetails"}, allEntries = true)
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {
        log.info("Updating property with ID: {}, Featured: {}", id, request.getFeatured());
        
        Property property = propertyRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        
        boolean featuredStatusChanging = !property.getFeatured().equals(request.getFeatured());
        
        mapRequestToProperty(request, property);
        
        if (request.getImages() != null) {
            updatePropertyImages(property, request.getImages());
        }
        
        Property updatedProperty = propertyRepository.save(property);
        
        if (featuredStatusChanging) {
            log.info("Featured status changed for property {}: {} -> {}", 
                id, !request.getFeatured(), request.getFeatured());
        }
        
        return mapToPropertyResponse(updatedProperty);
    }

    @Override
    @CacheEvict(value = {"featuredProperties", "propertyDetails"}, allEntries = true)
    public void deleteProperty(Long id) {
        Property property = propertyRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        
        List<PropertyImage> images = property.getImages();
        for (PropertyImage image : images) {
            try {
                String fileName = extractFileNameFromUrl(image.getImageUrl());
                fileStorageService.deleteFile(fileName);
            } catch (Exception e) {
                log.error("Failed to delete image file: {}", image.getImageUrl(), e);
            }
        }
        
        propertyRepository.deleteById(id);
        log.info("Deleted property with id: {} and {} associated images", id, images.size());
    }

    @Override
    @CacheEvict(value = "propertyDetails", key = "#propertyId")
    public List<String> uploadPropertyImages(Long propertyId, MultipartFile[] files) {
        Property property = propertyRepository.findByIdWithImages(propertyId)
            .orElseThrow(()-> new ResourceNotFoundException("Property not found with id: " + propertyId));
        
        if (files.length == 0) {
            throw new BadRequestException("No files provided");
        }
        
        List<PropertyImage> existingImages = property.getImages();
        for (PropertyImage existingImage : existingImages) {
            try {
                String fileName = extractFileNameFromUrl(existingImage.getImageUrl());
                fileStorageService.deleteFile(fileName);
                log.debug("Deleted existing image file: {}", fileName);
            } catch (Exception e) {
                log.error("Failed to delete existing image: {}", existingImage.getImageUrl(), e);
            }
        }
        
        propertyImageRepository.deleteByPropertyId(propertyId);
        
        List<String> uploadedUrls = new ArrayList<>();
        int maxImages = Math.min(files.length, MAX_IMAGES_PER_PROPERTY);
        
        for (int i = 0; i < maxImages; i++) {
            try {
                String fileName = fileStorageService.storeFile(files[i]);
                String fileUrl = "/api/files/" + fileName;
                String fullUrl = backendUrl + fileUrl;
                uploadedUrls.add(fullUrl);
                
                PropertyImage image = new PropertyImage();
                image.setProperty(property);
                image.setImageUrl(fileUrl);
                image.setImageOrder(i);
                propertyImageRepository.save(image);
                
            } catch (Exception e) {
                log.error("Failed to upload image {}: {}", i, files[i].getOriginalFilename(), e);
                throw new BadRequestException("Failed to upload image: " + files[i].getOriginalFilename());
            }
        }
        
        log.info("Uploaded {} images for property {}", uploadedUrls.size(), propertyId);
        return uploadedUrls;
    }

    @CacheEvict(value = "featuredProperties", allEntries = true)
    public void clearFeaturedPropertiesCache() {
        log.info("Manually cleared featured properties cache");
    }

    private Specification<Property> buildSpecification(SearchRequest searchRequest) {
        Specification<Property> spec = Specification.where(null);

        if (searchRequest.getSearch() != null && !searchRequest.getSearch().trim().isEmpty()) {
            String search = searchRequest.getSearch().trim().toLowerCase();
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + search + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search + "%"),
                    cb.like(cb.lower(root.get("address")), "%" + search + "%"),
                    cb.like(cb.lower(root.get("city")), "%" + search + "%"),
                    cb.like(root.get("zip"), "%" + search + "%")
                )
            );
        }

        if (searchRequest.getLocation() != null && !searchRequest.getLocation().trim().isEmpty()) {
            String location = searchRequest.getLocation().trim();
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("city")), "%" + location.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("address")), "%" + location.toLowerCase() + "%"),
                    cb.like(root.get("zip"), "%" + location + "%")
                )
            );
        }

        if (searchRequest.getPriceRange() != null && !searchRequest.getPriceRange().isEmpty()) {
            String[] range = searchRequest.getPriceRange().split("-");
            if (range.length > 0) {
                BigDecimal minPrice = new BigDecimal(range[0]);
                spec = spec.and((root, query, cb) -> 
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice)
                );
                
                if (range.length > 1) {
                    BigDecimal maxPrice = new BigDecimal(range[1]);
                    spec = spec.and((root, query, cb) -> 
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice)
                    );
                }
            }
        }

        if (searchRequest.getPropertyType() != null && !searchRequest.getPropertyType().isEmpty() 
            && !"All Types".equals(searchRequest.getPropertyType())) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("propertyType"), searchRequest.getPropertyType())
            );
        }

        if (searchRequest.getBedrooms() != null && !searchRequest.getBedrooms().isEmpty() 
            && !"Any".equals(searchRequest.getBedrooms())) {
            int minBeds = Integer.parseInt(searchRequest.getBedrooms().replace("+", ""));
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("bedrooms"), minBeds)
            );
        }

        return spec;
    }

    private Sort buildSort(String sortBy) {
        return switch (sortBy != null ? sortBy : "newest") {
            case "priceLow" -> Sort.by("price").ascending();
            case "priceHigh" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };
    }

    private String extractFileNameFromUrl(String url) {
        if (url != null && url.contains("/api/files/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
    }

    private void updatePropertyImages(Property property, List<String> newImageUrls) {
        List<PropertyImage> existingImages = property.getImages();
        for (PropertyImage image : existingImages) {
            try {
                String fileName = extractFileNameFromUrl(image.getImageUrl());
                fileStorageService.deleteFile(fileName);
            } catch (Exception e) {
                log.error("Failed to delete existing image: {}", image.getImageUrl(), e);
            }
        }
        propertyImageRepository.deleteByPropertyId(property.getId());
        
        savePropertyImages(property, newImageUrls);
    }

    private void savePropertyImages(Property property, List<String> imageUrls) {
        int maxImages = Math.min(imageUrls.size(), MAX_IMAGES_PER_PROPERTY);
        for (int i = 0; i < maxImages; i++) {
            PropertyImage image = new PropertyImage();
            image.setProperty(property);
            image.setImageUrl(imageUrls.get(i));
            image.setImageOrder(i);
            propertyImageRepository.save(image);
        }
    }

    private void mapRequestToProperty(PropertyRequest request, Property property) {
        property.setTitle(request.getTitle());
        property.setPrice(request.getPrice());
        property.setDescription(request.getDescription());
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setState(request.getState());
        property.setZip(request.getZip());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setSquareFootage(request.getSquareFootage());
        property.setPropertyType(request.getPropertyType());
        property.setStatus(request.getStatus());
        property.setVideoLink(request.getVideoLink());
        property.setFeatured(request.getFeatured());
    }

    private PropertyResponse mapToPropertyResponseLight(Property property) {
        PropertyResponse response = new PropertyResponse();
        response.setId(property.getId());
        response.setTitle(property.getTitle());
        response.setPrice(property.getPrice());
        response.setDescription(property.getDescription());
        response.setAddress(property.getAddress());
        response.setCity(property.getCity());
        response.setState(property.getState());
        response.setZip(property.getZip());
        response.setBedrooms(property.getBedrooms());
        response.setBathrooms(property.getBathrooms());
        response.setSquareFootage(property.getSquareFootage());
        response.setPropertyType(property.getPropertyType());
        response.setStatus(property.getStatus());
        response.setVideoLink(property.getVideoLink());
        response.setFeatured(property.getFeatured());
        response.setCreatedAt(property.getCreatedAt());
        response.setUpdatedAt(property.getUpdatedAt());
        
        if (!property.getImages().isEmpty()) {
            PropertyImage firstImage = property.getImages().get(0);
            String url = firstImage.getImageUrl();
            if (url != null && url.startsWith("/api/files")) {
                url = backendUrl + url;
            }
            response.setImages(List.of(url));
        } else {
            response.setImages(new ArrayList<>());
        }
        
        return response;
    }

    private PropertyResponse mapToPropertyResponse(Property property) {
        PropertyResponse response = mapToPropertyResponseLight(property);
        
        List<String> imageUrls = property.getImages().stream()
            .map(image -> {
                String url = image.getImageUrl();
                if (url != null && url.startsWith("/api/files")) {
                    return backendUrl + url;
                }
                return url;
            })
            .collect(Collectors.toList());
        response.setImages(imageUrls);
        
        return response;
    }
}