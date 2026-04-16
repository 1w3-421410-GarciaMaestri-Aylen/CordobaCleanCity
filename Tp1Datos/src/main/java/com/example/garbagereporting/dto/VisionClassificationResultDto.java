package com.example.garbagereporting.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VisionClassificationResultDto {
    boolean isTrash;
    String classificationResult;
    Double confidence;
}
