package com.soham.realty.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

public class FileUploadUtil {
    
    public static boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        return Arrays.asList(Constants.ALLOWED_IMAGE_EXTENSIONS).contains(extension);
    }
    
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    public static boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= Constants.MAX_IMAGE_SIZE;
    }
}

