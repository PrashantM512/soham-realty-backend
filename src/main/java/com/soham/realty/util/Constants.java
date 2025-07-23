package com.soham.realty.util;

public class Constants {
    public static final String DEFAULT_PAGE_NUMBER = "1";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    
    public static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MAX_IMAGES_PER_PROPERTY = 5;
    
    public static final String[] ALLOWED_IMAGE_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "webp"
    };
    
    public static final String[] PROPERTY_TYPES = {
        "House", "Apartment", "Condo", "Townhouse", "Villa",
        "Studio Apartment", "Penthouse", "Loft", "Row House",
        "Bungalow", "Independent House"
    };
    
    public static final String[] PROPERTY_STATUS = {
        "Available", "Sold", "Pending"
    };
    
    public static final String[] CONTACT_STATUS = {
        "New", "Contacted", "Resolved"
    };
}
