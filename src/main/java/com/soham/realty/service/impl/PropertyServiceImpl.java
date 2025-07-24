package com.soham.realty.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

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
        List<Property> featured = propertyRepository.findFeaturedPropertiesByStatus("Available");
        List<PropertyResponse> result = featured.stream()
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

        Property saved = propertyRepository.save(property);
        log.info("Created property with ID: {}, Featured: {}", saved.getId(), saved.getFeatured());
        return mapToPropertyResponse(saved);
    }

    @Override
    @CacheEvict(value = {"featuredProperties", "propertyDetails"}, allEntries = true)
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {
        log.info("Updating property with ID: {}, Featured: {}", id, request.getFeatured());

        Property property = propertyRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        boolean featuredChanging = !property.getFeatured().equals(request.getFeatured());
        mapRequestToProperty(request, property);
        Property updated = propertyRepository.save(property);

        if (featuredChanging) {
            log.info("Featured status changed for property {}: {} -> {}",
                id, !request.getFeatured(), request.getFeatured());
        }

        return mapToPropertyResponse(updated);
    }

    @Override
    @CacheEvict(value = {"featuredProperties", "propertyDetails"}, allEntries = true)
    public void deleteProperty(Long id) {
        log.info("Attempting to delete property with id: {}", id);

        Property property = propertyRepository.findByIdWithImages(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        List<PropertyImage> images = property.getImages();
        for (PropertyImage image : images) {
            try {
                String fileName = extractFileNameFromUrl(image.getImageUrl());
                if (fileName != null) {
                    fileStorageService.deleteFile(fileName);
                    log.debug("Deleted image file: {}", fileName);
                }
            } catch (Exception e) {
                log.error("Failed to delete image file: {}", image.getImageUrl(), e);
                // Continue even if file deletion fails
            }
        }

        try {
            propertyImageRepository.deleteByPropertyId(id);
            log.debug("Deleted PropertyImage records for property id: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete PropertyImage records for property id: {}", id, e);
            throw new RuntimeException("Failed to delete property images", e);
        }

        try {
            propertyRepository.deleteById(id);
            log.info("Successfully deleted property with id: {} and {} associated images", id, images.size());
        } catch (Exception e) {
            log.error("Failed to delete property with id: {}", id, e);
            throw new RuntimeException("Failed to delete property", e);
        }
    }

    @Override
    @CacheEvict(value = "propertyDetails", key = "#propertyId")
    public List<String> uploadPropertyImages(Long propertyId, MultipartFile[] files) {
        Property property = propertyRepository.findByIdWithImages(propertyId)
            .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        if (files.length == 0) {
            throw new BadRequestException("No files provided");
        }

        // Delete existing images
        List<PropertyImage> existing = property.getImages();
        for (PropertyImage img : existing) {
            try {
                String fileName = extractFileNameFromUrl(img.getImageUrl());
                if (fileName != null) {
                    fileStorageService.deleteFile(fileName);
                    log.debug("Deleted existing image file: {}", fileName);
                }
            } catch (Exception e) {
                log.error("Failed to delete existing image: {}", img.getImageUrl(), e);
            }
        }
        propertyImageRepository.deleteByPropertyId(propertyId);

        List<String> uploadedUrls = new ArrayList<>();
        int maxImages = Math.min(files.length, MAX_IMAGES_PER_PROPERTY);

        for (int i = 0; i < maxImages; i++) {
            try {
                String fileUrl = fileStorageService.storeFile(files[i]);
                if (!"prod".equals(activeProfile)) {
                    fileUrl = "/api/files/" + fileUrl;
                }
                uploadedUrls.add(fileUrl);

                PropertyImage image = new PropertyImage();
                image.setProperty(property);
                image.setImageUrl(fileUrl);
                image.setImageOrder(i);
                propertyImageRepository.save(image);

                if (i == 0) {
                    property.setImageUrl(fileUrl);
                }
            } catch (Exception e) {
                log.error("Failed to upload image {}: {}", i, files[i].getOriginalFilename(), e);
                throw new BadRequestException("Failed to upload image: " + files[i].getOriginalFilename());
            }
        }

        propertyRepository.save(property);
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
            String term = searchRequest.getSearch().trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("address")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("city")), "%" + term + "%"),
                    cb.like(root.get("zip"), "%" + term + "%")
                )
            );
        }

        if (searchRequest.getLocation() != null && !searchRequest.getLocation().trim().isEmpty()) {
            String loc = searchRequest.getLocation().trim().toLowerCase();
            spec = spec.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("city")), "%" + loc + "%"),
                    cb.like(cb.lower(root.get("address")), "%" + loc + "%"),
                    cb.like(root.get("zip"), "%" + loc + "%")
                )
            );
        }

        if (searchRequest.getPriceRange() != null && !searchRequest.getPriceRange().isEmpty()) {
            String[] range = searchRequest.getPriceRange().split("-");
            BigDecimal min = new BigDecimal(range[0]);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min));
            if (range.length > 1) {
                BigDecimal max = new BigDecimal(range[1]);
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max));
            }
        }

        if (searchRequest.getPropertyType() != null && !searchRequest.getPropertyType().isEmpty() && !"All Types".equals(searchRequest.getPropertyType())) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("propertyType"), searchRequest.getPropertyType()));
        }

        if (searchRequest.getBedrooms() != null && !searchRequest.getBedrooms().isEmpty() && !"Any".equals(searchRequest.getBedrooms())) {
            int beds = Integer.parseInt(searchRequest.getBedrooms().replace("+", ""));
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("bedrooms"), beds));
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
        if (url == null) {
            return null;
        }
        if ("prod".equals(activeProfile) && url.startsWith("http")) {
            int slash = url.lastIndexOf("/");
            int dot = url.lastIndexOf(".");
            if (slash != -1 && dot > slash) {
                return url.substring(slash + 1, dot);
            }
            return url;
        }
        if (url.contains("/api/files/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return url;
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
        PropertyResponse resp = new PropertyResponse();
        resp.setId(property.getId());
        resp.setTitle(property.getTitle());
        resp.setPrice(property.getPrice());
        resp.setDescription(property.getDescription());
        resp.setAddress(property.getAddress());
        resp.setCity(property.getCity());
        resp.setState(property.getState());
        resp.setZip(property.getZip());
        resp.setBedrooms(property.getBedrooms());
        resp.setBathrooms(property.getBathrooms());
        resp.setSquareFootage(property.getSquareFootage());
        resp.setPropertyType(property.getPropertyType());
        resp.setStatus(property.getStatus());
        resp.setVideoLink(property.getVideoLink());
        resp.setFeatured(property.getFeatured());
        resp.setCreatedAt(property.getCreatedAt());
        resp.setUpdatedAt(property.getUpdatedAt());

        List<String> urls = property.getImages().stream()
            .map(PropertyImage::getImageUrl)
            .collect(Collectors.toList());
        resp.setImages(urls);

        return resp;
    }

    private PropertyResponse mapToPropertyResponse(Property property) {
        return mapToPropertyResponseLight(property);
    }
}
