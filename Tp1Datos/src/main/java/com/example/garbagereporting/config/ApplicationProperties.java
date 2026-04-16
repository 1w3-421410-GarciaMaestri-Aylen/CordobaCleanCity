package com.example.garbagereporting.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    @NotNull
    private Messaging messaging = new Messaging();

    @NotNull
    private Cache cache = new Cache();

    @NotNull
    private Vision vision = new Vision();

    @NotNull
    private Storage storage = new Storage();

    @NotNull
    private Cors cors = new Cors();

    @NotNull
    private Auth auth = new Auth();

    @NotNull
    private Mail mail = new Mail();

    @NotNull
    private Routing routing = new Routing();

    @Data
    public static class Messaging {
        @NotBlank
        private String reportProcessingQueue;
        @NotBlank
        private String reportProcessingExchange;
        @NotBlank
        private String reportProcessingRoutingKey;
        @NotBlank
        private String reportProcessingDlq;
        @NotBlank
        private String reportProcessingDlx;
        @NotBlank
        private String reportProcessingDlqRoutingKey;
    }

    @Data
    public static class Cache {
        @Min(1)
        @Max(1440)
        private Integer reportsTodayTtlMinutes = 10;
        @Min(1)
        @Max(1440)
        private Integer routeOptimizationTtlMinutes = 20;
    }

    @Data
    public static class Vision {
        @NotBlank
        private String trashCatalogDir = "storage/catalog/basura";
        @NotBlank
        private String nonTrashCatalogDir = "storage/catalog/no-basura";
    }

    @Data
    public static class Storage {
        @NotBlank
        private String imageDir = "uploads";
    }

    @Data
    public static class Cors {
        @NotBlank
        private String allowedOrigins = "http://localhost:5173";
        @NotBlank
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
        @NotBlank
        private String allowedHeaders = "*";
        @NotNull
        private Boolean allowCredentials = false;
    }

    @Data
    public static class Auth {
        @NotBlank
        @Size(min = 32, message = "jwtSecret must have at least 32 characters")
        private String jwtSecret = "change-this-jwt-secret-for-demo-at-least-32-chars";
        @Min(5)
        @Max(1440)
        private Integer jwtExpirationMinutes = 120;
        @Min(1)
        @Max(168)
        private Integer verificationTokenExpirationHours = 24;
        @NotNull
        private Admin admin = new Admin();
    }

    @Data
    public static class Admin {
        @NotBlank
        private String firstName = "Admin";
        @NotBlank
        private String lastName = "Demo";
        @NotBlank
        private String email = "admin@ecoruta.local";
        @NotBlank
        @Size(min = 8, max = 120)
        private String password = "Admin12345!";
    }

    @Data
    public static class Mail {
        @NotNull
        private Boolean mockEnabled = true;
        @NotBlank
        private String verificationBaseUrl = "http://localhost:5173/verify-email/confirm";
        @NotBlank
        private String from = "no-reply@ecoruta.local";
    }

    @Data
    public static class Routing {
        @NotNull
        private Boolean enabled = true;
        @NotBlank
        private String provider = "osrm";
        @NotBlank
        private String osrmBaseUrl = "https://router.project-osrm.org";
        @NotBlank
        private String profile = "driving";
    }
}
