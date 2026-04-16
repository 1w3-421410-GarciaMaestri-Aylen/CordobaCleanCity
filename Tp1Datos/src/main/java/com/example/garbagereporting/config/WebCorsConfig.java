package com.example.garbagereporting.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final ApplicationProperties properties;

    public WebCorsConfig(ApplicationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = splitCsv(properties.getCors().getAllowedOrigins());
        String[] methods = splitCsv(properties.getCors().getAllowedMethods());
        String[] headers = splitCsv(properties.getCors().getAllowedHeaders());

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders(headers)
                .allowCredentials(Boolean.TRUE.equals(properties.getCors().getAllowCredentials()));

        registry.addMapping("/uploads/**")
                .allowedOrigins(origins)
                .allowedMethods("GET")
                .allowedHeaders(headers)
                .allowCredentials(Boolean.TRUE.equals(properties.getCors().getAllowCredentials()));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storagePath = Paths.get(properties.getStorage().getImageDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(storagePath.toUri().toString());
    }

    private String[] splitCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            return new String[0];
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }
}
