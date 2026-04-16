package com.example.garbagereporting.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String store(MultipartFile file);

    void deleteIfExists(String imagePath);
}
