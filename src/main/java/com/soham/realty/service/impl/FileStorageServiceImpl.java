package com.soham.realty.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.exception.BadRequestException;
import com.soham.realty.exception.ResourceNotFoundException;
import com.soham.realty.service.FileStorageService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Profile("dev")
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private Path fileStorageLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory initialized: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create file storage directory: {}", this.fileStorageLocation, ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // ENHANCED: Comprehensive file validation
        validateFile(file);
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new BadRequestException("Filename contains invalid path sequence " + originalFileName);
            }

            // ENHANCED: Validate file extension
            String fileExtension = getFileExtension(originalFileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new BadRequestException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
            }

            // ENHANCED: Validate MIME type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new BadRequestException("Invalid file type. Only image files are allowed.");
            }

            // Generate unique filename with timestamp for better uniqueness
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "." + fileExtension;

            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.debug("File stored successfully: {}", fileName);
            return fileName;
            
        } catch (IOException ex) {
            log.error("Could not store file {}: {}", originalFileName, ex.getMessage());
            throw new BadRequestException("Could not store file " + originalFileName + ". Please try again!");
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            // ENHANCED: Sanitize filename
            String sanitizedFileName = StringUtils.cleanPath(fileName);
            if (sanitizedFileName.contains("..")) {
                throw new BadRequestException("Invalid file path: " + fileName);
            }
            
            Path filePath = this.fileStorageLocation.resolve(sanitizedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            log.error("Malformed URL for file: {}", fileName, ex);
            throw new ResourceNotFoundException("File not found: " + fileName);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("Attempted to delete file with null or empty filename");
            return;
        }
        
        try {
            // ENHANCED: Sanitize filename
            String sanitizedFileName = StringUtils.cleanPath(fileName);
            if (sanitizedFileName.contains("..")) {
                log.error("Invalid file path for deletion: {}", fileName);
                return;
            }
            
            Path filePath = this.fileStorageLocation.resolve(sanitizedFileName).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.debug("File deleted successfully: {}", fileName);
            } else {
                log.warn("File not found for deletion: {}", fileName);
            }
        } catch (IOException ex) {
            log.error("Could not delete file {}: {}", fileName, ex.getMessage());
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }

    // ENHANCED: Comprehensive file validation
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(
                String.format("File size exceeds maximum allowed size of %d MB", maxFileSize / 1024 / 1024)
            );
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new BadRequestException("File name is required");
        }

        // Check for potentially malicious file names
        if (originalFileName.matches(".*[<>:\"/\\\\|?*].*")) {
            throw new BadRequestException("File name contains invalid characters");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    // ENHANCED: Utility method to get storage statistics
    public StorageStats getStorageStats() {
        try {
            Path storage = this.fileStorageLocation;
            long totalSpace = Files.getFileStore(storage).getTotalSpace();
            long usableSpace = Files.getFileStore(storage).getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            long fileCount = Files.list(storage).count();
            
            return new StorageStats(totalSpace, usedSpace, usableSpace, fileCount);
        } catch (IOException e) {
            log.error("Error getting storage stats", e);
            return new StorageStats(0, 0, 0, 0);
        }
    }

    public static class StorageStats {
        public final long totalSpace;
        public final long usedSpace;
        public final long usableSpace;
        public final long fileCount;

        public StorageStats(long totalSpace, long usedSpace, long usableSpace, long fileCount) {
            this.totalSpace = totalSpace;
            this.usedSpace = usedSpace;
            this.usableSpace = usableSpace;
            this.fileCount = fileCount;
        }
    }
}