package com.example.garbagereporting.service;

import com.example.garbagereporting.dto.VisionClassificationResultDto;

public interface VisionClassificationService {

    VisionClassificationResultDto classify(String imagePath, String originalFilename);
}
