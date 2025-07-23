package com.soham.realty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
 private boolean success;
 private T data;
 private String message;
 private String error;
 
 public static <T> ApiResponse<T> success(T data) {
     return new ApiResponse<>(true, data, null, null);
 }
 
 public static <T> ApiResponse<T> success(T data, String message) {
     return new ApiResponse<>(true, data, message, null);
 }
 
 public static <T> ApiResponse<T> error(String error) {
     return new ApiResponse<>(false, null, null, error);
 }
}

