package com.soham.realty.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.soham.realty.exception.BadRequestException;
import com.soham.realty.exception.ResourceNotFoundException;
import com.soham.realty.service.FileStorageService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;

@Service
@Slf4j
@Profile("prod")
public class CloudinaryFileStorageServiceImpl implements FileStorageService {

    @Autowired
    private Cloudinary cloudinary;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Override
    public String storeFile(MultipartFile file) {
        try {
            String publicId = UUID.randomUUID().toString();
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("public_id", publicId));
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload file: " + file.getOriginalFilename());
        }
    }

    @Override
    public Resource loadFileAsResource(String fileUrl) {
        try {
            Resource resource = new UrlResource(fileUrl);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileUrl);
            }
        } catch (MalformedURLException ex) {
            log.error("Malformed URL for file: {}", fileUrl, ex);
            throw new ResourceNotFoundException("File not found: " + fileUrl);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            cloudinary.uploader().destroy(fileName, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete file: " + fileName);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new BadRequestException("File name is required");
        }
        String fileExtension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new BadRequestException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only image files are allowed.");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}

