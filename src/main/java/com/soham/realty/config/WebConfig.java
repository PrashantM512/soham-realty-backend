package com.soham.realty.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./Uploads}")
    private String uploadDir;
    
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only configure for dev profile when uploadDir is present
        if (!uploadDir.isEmpty()) {
            String uploadPath = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadPath)
                    .setCachePeriod(3600)
                    .resourceChain(true);
        }
    }
}