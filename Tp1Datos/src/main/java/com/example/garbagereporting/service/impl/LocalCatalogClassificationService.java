package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.dto.VisionClassificationResultDto;
import com.example.garbagereporting.service.VisionClassificationService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCatalogClassificationService implements VisionClassificationService {

    private static final String TRASH_MATCH_MESSAGE =
            "Coincidencia exacta con catalogo local de basura. El reporte fue clasificado como basura.";
    private static final String NON_TRASH_MATCH_MESSAGE =
            "Coincidencia exacta con catalogo local de no basura. El reporte fue clasificado como no basura.";
    private static final String NO_MATCH_MESSAGE =
            "La imagen no coincide con ningun archivo del catalogo local. Se clasifica como no basura por defecto.";

    private final ApplicationProperties properties;

    @PostConstruct
    void initializeCatalogDirectories() {
        createCatalogDirectoriesIfMissing();
    }

    @Override
    public VisionClassificationResultDto classify(String imagePath, String originalFilename) {
        try {
            createCatalogDirectoriesIfMissing();
        } catch (RuntimeException ex) {
            log.warn("Unable to initialize local catalog directories: {}", ex.getMessage());
            return buildNoMatchResult();
        }

        if (!StringUtils.hasText(imagePath)) {
            return buildNoMatchResult();
        }

        Path submittedImagePath = Paths.get(imagePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(submittedImagePath)) {
            log.warn("Image path does not exist or is not a file: {}", submittedImagePath);
            return buildNoMatchResult();
        }

        try {
            String submittedHash = calculateSha256(submittedImagePath);

            if (containsHash(resolveTrashCatalogPath(), submittedHash)) {
                return buildResult(true, TRASH_MATCH_MESSAGE);
            }
            if (containsHash(resolveNonTrashCatalogPath(), submittedHash)) {
                return buildResult(false, NON_TRASH_MATCH_MESSAGE);
            }
        } catch (IOException ex) {
            log.warn("Catalog hash classification failed for {}: {}", submittedImagePath, ex.getMessage());
        }

        return buildNoMatchResult();
    }

    private VisionClassificationResultDto buildResult(boolean isTrash, String message) {
        return VisionClassificationResultDto.builder()
                .isTrash(isTrash)
                .classificationResult(message)
                .confidence(1.0)
                .build();
    }

    private VisionClassificationResultDto buildNoMatchResult() {
        return VisionClassificationResultDto.builder()
                .isTrash(false)
                .classificationResult(NO_MATCH_MESSAGE)
                .confidence(null)
                .build();
    }

    private boolean containsHash(Path catalogPath, String expectedHash) throws IOException {
        try (Stream<Path> files = Files.walk(catalogPath)) {
            return files
                    .filter(Files::isRegularFile)
                    .anyMatch(filePath -> hashMatches(filePath, expectedHash));
        }
    }

    private boolean hashMatches(Path filePath, String expectedHash) {
        try {
            return expectedHash.equals(calculateSha256(filePath));
        } catch (IOException ex) {
            log.warn("Skipping unreadable catalog file {}: {}", filePath, ex.getMessage());
            return false;
        }
    }

    private String calculateSha256(Path filePath) throws IOException {
        MessageDigest digest = newSha256Digest();
        try (InputStream fileStream = Files.newInputStream(filePath);
             DigestInputStream digestStream = new DigestInputStream(fileStream, digest)) {
            byte[] buffer = new byte[8192];
            while (digestStream.read(buffer) != -1) {
                // read stream to update digest
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private void createCatalogDirectoriesIfMissing() {
        createDirectory(resolveTrashCatalogPath());
        createDirectory(resolveNonTrashCatalogPath());
    }

    private void createDirectory(Path directoryPath) {
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create catalog directory: " + directoryPath, ex);
        }
    }

    private Path resolveTrashCatalogPath() {
        return Paths.get(properties.getVision().getTrashCatalogDir()).toAbsolutePath().normalize();
    }

    private Path resolveNonTrashCatalogPath() {
        return Paths.get(properties.getVision().getNonTrashCatalogDir()).toAbsolutePath().normalize();
    }
}
