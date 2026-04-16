package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.exception.BusinessValidationException;
import com.example.garbagereporting.exception.FileStorageException;
import com.example.garbagereporting.service.ImageStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    private final ApplicationProperties properties;

    @Override
    public String store(MultipartFile file) {
        try {
            Path storagePath = Paths.get(properties.getStorage().getImageDir()).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);

            byte[] fileBytes = file.getBytes();
            validateImage(file, fileBytes);

            String extension = extractExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = storagePath.resolve(fileName).normalize();
            if (targetPath.getParent() == null || !targetPath.getParent().equals(storagePath)) {
                throw new BusinessValidationException("invalid storage path");
            }
            Files.write(targetPath, fileBytes);

            return targetPath.toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store image locally", ex);
        }
    }

    @Override
    public void deleteIfExists(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(imagePath));
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete temporary image", ex);
        }
    }

    private String extractExtension(String originalName) {
        if (originalName == null) {
            return ".bin";
        }
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return ".bin";
        }
        return originalName.substring(dotIndex);
    }

    private void validateImage(MultipartFile file, byte[] fileBytes) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessValidationException("uploaded file must be an image");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            if (ImageIO.read(inputStream) == null) {
                throw new BusinessValidationException("uploaded file is not a valid image");
            }
        } catch (IOException ex) {
            throw new BusinessValidationException("uploaded file is not a valid image");
        }
    }
}
