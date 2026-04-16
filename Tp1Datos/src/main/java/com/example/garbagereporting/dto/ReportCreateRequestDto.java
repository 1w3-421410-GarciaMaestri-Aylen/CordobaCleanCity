package com.example.garbagereporting.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReportCreateRequestDto {

    @NotNull(message = "image is required")
    private MultipartFile image;

    @NotNull(message = "lat is required")
    @DecimalMin(value = "-90.0", message = "lat must be >= -90")
    @DecimalMax(value = "90.0", message = "lat must be <= 90")
    private Double lat;

    @NotNull(message = "lng is required")
    @DecimalMin(value = "-180.0", message = "lng must be >= -180")
    @DecimalMax(value = "180.0", message = "lng must be <= 180")
    private Double lng;
}
