package com.soham.realty.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.soham.realty.exception.BadRequestException;
import com.soham.realty.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@Profile("prod")
public class CloudinaryFileStorageServiceImpl implements FileStorageService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret
        ));
        log.info("Cloudinary initialized successfully");
    }

    @Override
    public String storeFile(MultipartFile file) {
        try {
            File uploadedFile = convertMultiPartToFile(file);
            Map uploadResult = cloudinary.uploader().upload(uploadedFile, ObjectUtils.emptyMap());
            String fileUrl = uploadResult.get("url").toString();
            log.debug("File uploaded to Cloudinary: {}", fileUrl);
            return fileUrl; // Return the URL to store in your database
        } catch (Exception e) {
            log.error("Error uploading file to Cloudinary", e);
            throw new BadRequestException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        throw new UnsupportedOperationException("Use Cloudinary URL directly in production");
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            cloudinary.uploader().destroy(fileName, ObjectUtils.emptyMap());
            log.debug("File deleted from Cloudinary: {}", fileName);
        } catch (Exception e) {
            log.error("Error deleting file from Cloudinary", e);
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes()); // ✅ Fixed this line
        fos.close();
        return convFile;
    }

}