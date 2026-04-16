package com.example.garbagereporting.config;

import com.example.garbagereporting.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CacheWarmupCleaner {

    private final CacheManager cacheManager;

    @Bean
    public ApplicationRunner clearApplicationCachesOnStartup() {
        return args -> {
            clear(CacheNames.REPORTS_TODAY);
            clear(CacheNames.ROUTE_OPTIMIZATION);
        };
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.info("Cache {} not configured at startup", cacheName);
            return;
        }

        cache.clear();
        log.info("Cache {} cleared at startup", cacheName);
    }
}
