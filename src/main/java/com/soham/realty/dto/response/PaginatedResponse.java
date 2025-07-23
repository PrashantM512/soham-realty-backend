package com.soham.realty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
 private List<T> data;
 private long total;
 private int page;
 private int limit;
 private int totalPages;
 
 public static <T> PaginatedResponse<T> of(List<T> data, long total, int page, int limit) {
     int totalPages = (int) Math.ceil((double) total / limit);
     return new PaginatedResponse<>(data, total, page, limit, totalPages);
 }
}