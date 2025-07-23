// CacheConfig.java - UPDATED WITH SHORTER CACHE DURATION
package com.soham.realty.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableAsync
public class CacheConfig {

//    @Bean
//    public CacheManager cacheManager() {
//        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
//        
//        // FIXED: Shorter cache duration for featured properties to ensure they update quickly
//        cacheManager.setCaffeine(Caffeine.newBuilder()
//            .maximumSize(1000)
//            .expireAfterWrite(2, TimeUnit.MINUTES) // Reduced from 5 to 2 minutes
//            .recordStats()
//            .evictionListener((key, value, cause) -> {
//                System.out.println("Cache evicted - Key: " + key + ", Cause: " + cause);
//            }));
//        
//        cacheManager.setCacheNames(Arrays.asList("featuredProperties", "propertyDetails", "userDetails"));
//        cacheManager.setAllowNullValues(false);
//        
//        return cacheManager;
//    }
//}

// Alternative: Simple ConcurrentMapCacheManager (if you prefer not to add Caffeine dependency)
@Bean
public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    cacheManager.setCacheNames(Arrays.asList("featuredProperties", "propertyDetails", "userDetails"));
    cacheManager.setAllowNullValues(false);
    return cacheManager;
  }
}