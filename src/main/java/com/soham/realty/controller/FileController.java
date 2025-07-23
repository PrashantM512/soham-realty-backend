package com.soham.realty.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.soham.realty.dto.response.ApiResponse;
import com.soham.realty.service.FileStorageService;

import java.io.IOException;
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
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = "/api/files/" + fileName;
        
        return ResponseEntity.ok(ApiResponse.success(fileDownloadUri, "File uploaded successfully"));
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> fileDownloadUris = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String fileName = fileStorageService.storeFile(file);
            String fileDownloadUri = "/api/files/" + fileName;
            fileDownloadUris.add(fileDownloadUri);
        }
        
        return ResponseEntity.ok(ApiResponse.success(fileDownloadUris, "Files uploaded successfully"));
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
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
}
