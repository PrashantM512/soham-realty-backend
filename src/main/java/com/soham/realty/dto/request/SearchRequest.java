package com.soham.realty.dto.request;

import lombok.Data;

@Data
public class SearchRequest {
    private String search;
    private String title;
    private String location;
    private String priceRange;
    private String propertyType;
    private String bedrooms;
    private String sortBy;
    private Integer page;
    private Integer limit;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}