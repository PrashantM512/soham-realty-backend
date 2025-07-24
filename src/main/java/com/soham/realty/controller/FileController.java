package com.soham.realty.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.dto.response.ApiResponse;
import com.soham.realty.service.FileStorageService;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file);
        
        // For Cloudinary, the service returns the full URL
        // For local storage, it returns just the filename
        String responseUrl;
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            // It's a Cloudinary URL, return as is
            responseUrl = fileUrl;
        } else {
            // It's a local file, add the API prefix
            responseUrl = "/api/files/" + fileUrl;
        }
        
        return ResponseEntity.ok(ApiResponse.success(responseUrl, "File uploaded successfully"));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> fileUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String fileUrl = fileStorageService.storeFile(file);
            
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                fileUrls.add(fileUrl);
            } else {
                fileUrls.add("/api/files/" + fileUrl);
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(fileUrls, "Files uploaded successfully"));
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Check if this is a request for a Cloudinary URL that was incorrectly routed here
        if (fileName.startsWith("http") || fileName.contains("res.cloudinary.com")) {
            // Redirect to the actual Cloudinary URL
            String cloudinaryUrl = fileName;
            if (!fileName.startsWith("http")) {
                cloudinaryUrl = "https://" + fileName;
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(cloudinaryUrl))
                    .build();
        }
        
        // For local files
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @DeleteMapping("/{fileUrl}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String fileUrl) {
        fileStorageService.deleteFile(fileUrl);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
    }
}