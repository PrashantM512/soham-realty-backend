package com.soham.realty.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.soham.realty.exception.BadRequestException;
import com.soham.realty.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Profile("prod")
@Slf4j
@RequiredArgsConstructor
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;
    
    @Value("${file.max-size:10485760}")
    private long maxFileSize;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Override
    public String storeFile(MultipartFile file) {
        validateFile(file);
        
        try {
            // Upload to Cloudinary
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", "soham-realty/properties",
                "resource_type", "image",
                "allowed_formats", String.join(",", ALLOWED_EXTENSIONS),
                "transformation", ObjectUtils.asMap(
                    "quality", "auto:good",
                    "fetch_format", "auto"
                )
            );
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            
            // Return the full Cloudinary URL instead of just the public_id
            String url = (String) uploadResult.get("secure_url");
            log.info("File uploaded to Cloudinary: {}", url);
            
            return url;
            
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BadRequestException("Failed to upload file. Please try again!");
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            // If it's already a full Cloudinary URL, return it as is
            if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
                return new UrlResource(fileName);
            }
            
            // Otherwise, construct the Cloudinary URL
            String url = cloudinary.url().secure(true).generate(fileName);
            return new UrlResource(url);
            
        } catch (MalformedURLException e) {
            log.error("Failed to load file from Cloudinary: {}", fileName, e);
            throw new BadRequestException("File not found: " + fileName);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        
        try {
            String publicId = extractPublicId(fileName);
            if (publicId != null) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("File deleted from Cloudinary: {}, result: {}", publicId, result);
            }
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", fileName, e);
            // Don't throw exception for delete failures
        }
    }
    
    private String extractPublicId(String url) {
        // Extract public_id from Cloudinary URL
        // Example: https://res.cloudinary.com/daz7kufro/image/upload/v1234567890/soham-realty/properties/image.jpg
        if (url.contains("cloudinary.com")) {
            String[] parts = url.split("/upload/v\\d+/");
            if (parts.length > 1) {
                String publicIdWithExt = parts[1];
                // Remove file extension
                int lastDotIndex = publicIdWithExt.lastIndexOf('.');
                return lastDotIndex > 0 ? publicIdWithExt.substring(0, lastDotIndex) : publicIdWithExt;
            }
        }
        return null;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(
                String.format("File size exceeds maximum allowed size of %d MB", maxFileSize / 1024 / 1024)
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only image files are allowed.");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null) {
            String extension = getFileExtension(originalFileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
            }
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}